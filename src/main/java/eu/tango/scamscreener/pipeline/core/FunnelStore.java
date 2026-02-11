package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.pipeline.model.IntentTag;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.util.TextUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FunnelStore {
	private static final int CLEANUP_INTERVAL = 64;

	private final RuleConfig ruleConfig;
	private final Map<String, PlayerContext> contextByPlayer = new HashMap<>();
	private int evaluationsSinceCleanup;

	/**
	 * Tracks per-player recent intent tags and evaluates funnel-like sequences.
	 */
	public FunnelStore(RuleConfig ruleConfig) {
		this.ruleConfig = ruleConfig;
	}

	public FunnelEvaluation evaluate(MessageEvent event, IntentTagger.TaggingResult taggingResult) {
		if (event == null || event.playerName() == null || event.playerName().isBlank()) {
			return FunnelEvaluation.empty();
		}

		ScamRules.FunnelConfig config = ruleConfig.funnelConfig();
		long now = event.timestampMs() > 0 ? event.timestampMs() : System.currentTimeMillis();
		runCleanupIfNeeded(now, config.contextTtlMillis());

		String playerKey = TextUtil.anonymizedSpeakerKey(event.playerName());
		PlayerContext context = contextByPlayer.computeIfAbsent(playerKey, ignored -> new PlayerContext());
		context.lastSeenMillis = now;

		Deque<FunnelRecord> history = context.history;
		pruneByWindow(history, now, config.windowMillis());
		history.addLast(new FunnelRecord(
			now,
			safeChannel(event.channel()),
			safeMessage(event.rawMessage()),
			taggingResult == null ? Set.of() : taggingResult.tags(),
			taggingResult != null && taggingResult.negativeContext()
		));
		while (history.size() > config.windowSize()) {
			history.removeFirst();
		}

		List<FunnelRecord> records = new ArrayList<>(history);
		if (records.isEmpty()) {
			return FunnelEvaluation.empty();
		}

		SequenceMatch full = findSequence(records, List.of(
			Step.offer(),
			Step.of(IntentTag.REP_REQUEST),
			Step.of(IntentTag.PLATFORM_REDIRECT),
			Step.of(IntentTag.INSTRUCTION_INJECTION)
		));

		SequenceMatch repRedirect = findSequence(records, List.of(
			Step.of(IntentTag.REP_REQUEST),
			Step.of(IntentTag.PLATFORM_REDIRECT)
		));

		SequenceMatch redirectInstruction = findSequence(records, List.of(
			Step.of(IntentTag.PLATFORM_REDIRECT),
			Step.of(IntentTag.INSTRUCTION_INJECTION)
		));

		SequenceMatch offerPayment = findSequence(records, List.of(
			Step.offer(),
			Step.of(IntentTag.PAYMENT_UPFRONT)
		));

		if (full == null && repRedirect == null && redirectInstruction == null && offerPayment == null) {
			return FunnelEvaluation.empty();
		}

		int bonus;
		List<String> steps;
		Set<Integer> contributingIndexes = new LinkedHashSet<>();
		if (full != null) {
			bonus = config.fullSequenceWeight();
			steps = full.stepNames();
			contributingIndexes.addAll(full.indexes());
		} else {
			bonus = config.partialSequenceWeight();
			if (repRedirect != null && redirectInstruction != null) {
				steps = List.of("REP", "REDIRECT", "INSTRUCTION");
				contributingIndexes.addAll(repRedirect.indexes());
				contributingIndexes.addAll(redirectInstruction.indexes());
				bonus += 6;
			} else if (repRedirect != null) {
				steps = repRedirect.stepNames();
				contributingIndexes.addAll(repRedirect.indexes());
			} else if (redirectInstruction != null) {
				steps = redirectInstruction.stepNames();
				contributingIndexes.addAll(redirectInstruction.indexes());
			} else {
				steps = offerPayment.stepNames();
				contributingIndexes.addAll(offerPayment.indexes());
			}
		}

		List<String> snippets = new ArrayList<>();
		List<String> channelTrail = new ArrayList<>();
		for (Integer index : contributingIndexes) {
			if (index == null || index < 0 || index >= records.size()) {
				continue;
			}
			FunnelRecord record = records.get(index);
			if (!record.message().isBlank()) {
				snippets.add(record.message());
			}
			if (!record.channel().isBlank()) {
				channelTrail.add(record.channel());
			}
		}
		if (snippets.size() > 4) {
			snippets = snippets.subList(0, 4);
		}

		String detail = "Funnel sequence " + String.join(" -> ", steps)
			+ " in " + Math.max(1, config.windowMillis() / 1000) + "s window"
			+ " (+" + bonus + ")"
			+ (channelTrail.isEmpty() ? "" : ", channels=" + String.join(">", channelTrail));

		return new FunnelEvaluation(bonus, detail, snippets);
	}

	public void reset() {
		contextByPlayer.clear();
		evaluationsSinceCleanup = 0;
	}

	private void runCleanupIfNeeded(long now, long ttlMillis) {
		evaluationsSinceCleanup++;
		if (evaluationsSinceCleanup < CLEANUP_INTERVAL) {
			return;
		}
		evaluationsSinceCleanup = 0;
		contextByPlayer.entrySet().removeIf(entry -> now - entry.getValue().lastSeenMillis > ttlMillis);
	}

	private static void pruneByWindow(Deque<FunnelRecord> history, long now, long windowMillis) {
		while (!history.isEmpty() && now - history.peekFirst().timestampMillis() > windowMillis) {
			history.removeFirst();
		}
	}

	private static SequenceMatch findSequence(List<FunnelRecord> records, List<Step> steps) {
		if (records == null || records.isEmpty() || steps == null || steps.isEmpty()) {
			return null;
		}

		List<Integer> indexes = new ArrayList<>(steps.size());
		List<String> stepNames = new ArrayList<>(steps.size());
		int fromIndex = 0;
		for (Step step : steps) {
			int matched = -1;
			for (int i = fromIndex; i < records.size(); i++) {
				if (step.matches(records.get(i))) {
					matched = i;
					break;
				}
			}
			if (matched < 0) {
				return null;
			}
			indexes.add(matched);
			stepNames.add(step.name());
			fromIndex = matched + 1;
		}
		return new SequenceMatch(indexes, stepNames);
	}

	private static String safeChannel(String channel) {
		if (channel == null || channel.isBlank()) {
			return "unknown";
		}
		return channel.trim().toLowerCase(java.util.Locale.ROOT);
	}

	private static String safeMessage(String message) {
		if (message == null || message.isBlank()) {
			return "";
		}
		return message.replace('\n', ' ').replace('\r', ' ').trim();
	}

	public record FunnelEvaluation(int bonusScore, String detail, List<String> relatedMessages) {
		public FunnelEvaluation {
			relatedMessages = relatedMessages == null ? List.of() : List.copyOf(relatedMessages);
		}

		public static FunnelEvaluation empty() {
			return new FunnelEvaluation(0, null, List.of());
		}
	}

	private record FunnelRecord(long timestampMillis, String channel, String message, Set<IntentTag> tags, boolean negativeContext) {
		private FunnelRecord {
			tags = tags == null ? Set.of() : Set.copyOf(tags);
		}
	}

	private record SequenceMatch(List<Integer> indexes, List<String> stepNames) {
	}

	private record Step(Set<IntentTag> accepted, String name, boolean ignoreNegativeOffer) {
		private static Step of(IntentTag tag) {
			return new Step(Set.of(tag), readableName(tag), false);
		}

		private static Step offer() {
			return new Step(Set.of(IntentTag.SERVICE_OFFER, IntentTag.FREE_OFFER), "OFFER", true);
		}

		private boolean matches(FunnelRecord record) {
			if (record == null || record.tags().isEmpty()) {
				return false;
			}
			if (ignoreNegativeOffer && record.negativeContext()) {
				return false;
			}
			for (IntentTag tag : accepted) {
				if (record.tags().contains(tag)) {
					return true;
				}
			}
			return false;
		}

		private static String readableName(IntentTag tag) {
			return switch (tag) {
				case SERVICE_OFFER, FREE_OFFER -> "OFFER";
				case REP_REQUEST -> "REP";
				case PLATFORM_REDIRECT -> "REDIRECT";
				case INSTRUCTION_INJECTION -> "INSTRUCTION";
				case PAYMENT_UPFRONT -> "PAYMENT";
				case COMMUNITY_ANCHOR -> "ANCHOR";
			};
		}
	}

	private static final class PlayerContext {
		private final Deque<FunnelRecord> history = new ArrayDeque<>();
		private long lastSeenMillis;
	}
}
