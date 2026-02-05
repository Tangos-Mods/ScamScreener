package eu.tango.scamscreener.detection;

import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.Messages;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ChatBehaviorDetector {
	private static final long TREND_WINDOW_MILLIS = 45_000L;
	private static final int TREND_MIN_MESSAGES = 3;
	private static final int TREND_MIN_TRIGGERED_MESSAGES = 2;
	private static final int TREND_MIN_TOTAL_SCORE = 35;
	private static final int TREND_SCORE_BONUS = 20;

	private final Map<String, Integer> repeatedContactByPlayer = new HashMap<>();
	private final Map<String, Deque<RecentAssessment>> recentAssessmentsByPlayer = new HashMap<>();
	private final Set<String> warnedContexts = new HashSet<>();

	public void reset() {
		repeatedContactByPlayer.clear();
		recentAssessmentsByPlayer.clear();
		warnedContexts.clear();
	}

	public DetectionResult handleMessage(String message, Consumer<Component> reply) {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine(message);
		if (parsed == null) {
			return null;
		}

		String normalizedMessage = parsed.message().toLowerCase(Locale.ROOT);
		int repeated = repeatedContactByPlayer.merge(parsed.playerName().toLowerCase(Locale.ROOT), 1, Integer::sum);
		ScamRules.BehaviorContext context = new ScamRules.BehaviorContext(
			parsed.message(),
			containsAny(normalizedMessage, "discord", "telegram", "t.me", "dm me", "add me"),
			containsAny(normalizedMessage, "pay first", "send first", "vorkasse", "first payment"),
			containsAny(normalizedMessage, "password", "passwort", "2fa", "auth code", "email login"),
			containsAny(normalizedMessage, "trusted middleman", "legit middleman", "middleman"),
			repeated
		);
		ScamRules.ScamAssessment assessment = ScamRules.assess(context);
		assessment = applyConversationTrend(parsed.playerName(), assessment);
		if (!ScamRules.shouldWarn(assessment)) {
			return null;
		}

		String dedupeKey = "behavior-risk:" + parsed.playerName().toLowerCase(Locale.ROOT) + ":" + assessment.riskLevel().name();
		if (!warnedContexts.add(dedupeKey)) {
			return null;
		}

		reply.accept(Messages.behaviorRiskWarning(parsed.playerName(), assessment));
		return new DetectionResult(parsed, assessment);
	}

	private ScamRules.ScamAssessment applyConversationTrend(String playerName, ScamRules.ScamAssessment assessment) {
		if (assessment == null || playerName == null || playerName.isBlank()) {
			return assessment;
		}

		String key = playerName.toLowerCase(Locale.ROOT);
		long now = System.currentTimeMillis();
		Deque<RecentAssessment> history = recentAssessmentsByPlayer.computeIfAbsent(key, ignored -> new ArrayDeque<>());

		while (!history.isEmpty() && now - history.peekFirst().timestampMillis() > TREND_WINDOW_MILLIS) {
			history.removeFirst();
		}
		history.addLast(new RecentAssessment(now, assessment.riskScore(), !assessment.triggeredRules().isEmpty(), assessment.evaluatedMessage()));
		while (history.size() > 8) {
			history.removeFirst();
		}

		int totalScore = 0;
		int triggeredMessages = 0;
		List<String> evaluatedMessages = new java.util.ArrayList<>();
		for (RecentAssessment item : history) {
			totalScore += Math.max(0, item.riskScore());
			if (item.hadRules()) {
				triggeredMessages++;
			}
			if (item.message() != null && !item.message().isBlank()) {
				evaluatedMessages.add(item.message());
			}
		}

		boolean trendTriggered = history.size() >= TREND_MIN_MESSAGES
			&& triggeredMessages >= TREND_MIN_TRIGGERED_MESSAGES
			&& totalScore >= TREND_MIN_TOTAL_SCORE;
		if (!trendTriggered || !ScamRules.isRuleEnabled(ScamRules.ScamRule.MULTI_MESSAGE_PATTERN)) {
			return assessment;
		}
		if (assessment.triggeredRules().contains(ScamRules.ScamRule.MULTI_MESSAGE_PATTERN)) {
			return assessment;
		}

		Set<ScamRules.ScamRule> updatedRules = new HashSet<>(assessment.triggeredRules());
		updatedRules.add(ScamRules.ScamRule.MULTI_MESSAGE_PATTERN);
		Map<ScamRules.ScamRule, String> details = new LinkedHashMap<>();
		if (assessment.ruleDetails() != null) {
			details.putAll(assessment.ruleDetails());
		}
		details.put(
			ScamRules.ScamRule.MULTI_MESSAGE_PATTERN,
			"Conversation trend: " + history.size() + " messages in " + (TREND_WINDOW_MILLIS / 1000)
				+ "s, triggered messages=" + triggeredMessages + ", cumulative score=" + totalScore + " (+" + TREND_SCORE_BONUS + ")"
		);

		int boostedScore = Math.min(100, assessment.riskScore() + TREND_SCORE_BONUS);
		return new ScamRules.ScamAssessment(
			boostedScore,
			mapLevel(boostedScore),
			updatedRules,
			details,
			assessment.evaluatedMessage(),
			evaluatedMessages.isEmpty() ? assessment.allEvaluatedMessages() : evaluatedMessages
		);
	}

	private static ScamRules.ScamRiskLevel mapLevel(int score) {
		if (score >= 70) {
			return ScamRules.ScamRiskLevel.CRITICAL;
		}
		if (score >= 40) {
			return ScamRules.ScamRiskLevel.HIGH;
		}
		if (score >= 20) {
			return ScamRules.ScamRiskLevel.MEDIUM;
		}
		return ScamRules.ScamRiskLevel.LOW;
	}

	private static boolean containsAny(String text, String... tokens) {
		for (String token : tokens) {
			if (text.contains(token)) {
				return true;
			}
		}
		return false;
	}

	public record DetectionResult(ChatLineParser.ParsedPlayerLine parsedLine, ScamRules.ScamAssessment assessment) {
	}

	private record RecentAssessment(long timestampMillis, int riskScore, boolean hadRules, String message) {
	}
}
