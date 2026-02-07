package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.ai.LocalAiScorer;
import eu.tango.scamscreener.chat.mute.MutePatternManager;
import eu.tango.scamscreener.ui.messages.RiskMessages;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import eu.tango.scamscreener.pipeline.model.BehaviorAnalysis;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.pipeline.model.DetectionLevel;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.pipeline.model.SignalSource;
import eu.tango.scamscreener.pipeline.model.ScreeningResult;
import eu.tango.scamscreener.pipeline.stage.RuleSignalStage;
import eu.tango.scamscreener.pipeline.stage.LevenshteinSignalStage;
import eu.tango.scamscreener.rules.ScamRules;

public final class DetectionPipeline {
	private final MutePatternManager mutePatternManager;
	private final RuleSignalStage ruleSignalStage;
	private final LevenshteinSignalStage levenshteinSignalStage;
	private final BehaviorAnalyzer behaviorAnalyzer;
	private final AiScorer aiScorer;
	private final TrendStore trendStore;
	private final Set<String> seenWarnings = new HashSet<>();

	/**
	 * Creates the full detection pipeline with all stages wired up.
	 * The pipeline is executed from {@link #processWithResult(MessageEvent, java.util.function.Consumer, Runnable)}.
	 */
	public DetectionPipeline(MutePatternManager mutePatternManager, LocalAiScorer localAiScorer) {
		this.mutePatternManager = mutePatternManager;
		this.ruleSignalStage = new RuleSignalStage();
		this.levenshteinSignalStage = new LevenshteinSignalStage();
		this.behaviorAnalyzer = new BehaviorAnalyzer();
		this.aiScorer = new AiScorer(localAiScorer);
		this.trendStore = new TrendStore();
	}

	/**
	 * Runs the pipeline and always returns the computed screening result, even if no warning is emitted.
	 * The warning output is still emitted when the decision allows it.
	 */
	public ScreeningResult processWithResult(MessageEvent event, Consumer<Component> reply, Runnable warningSound) {
		ScreeningResult screening = evaluate(event);
		if (screening == null || screening.muted()) {
			return screening;
		}
		if (screening.shouldWarn()) {
			emitWarning(screening.event(), screening.result(), reply, warningSound);
		}
		return screening;
	}

	private ScreeningResult evaluate(MessageEvent event) {
		Optional<MessageEvent> maybeEvent = filterMuted(event);
		if (maybeEvent.isEmpty()) {
			return new ScreeningResult(event, null, false, true);
		}

		MessageEvent safeEvent = maybeEvent.get();
		BehaviorAnalysis analysis = behaviorAnalyzer.analyze(safeEvent);
		List<Signal> signals = new ArrayList<>();
		signals.addAll(ruleSignalStage.collectSignals(safeEvent));
		signals.addAll(levenshteinSignalStage.collectSignals(safeEvent));
		signals.addAll(collectBehaviorSignals(analysis));
		addAiSignal(signals, analysis);
		signals.addAll(collectTrendSignals(safeEvent, signals));

		DetectionResult result = scoreDetection(safeEvent, signals);
		boolean shouldWarn = shouldWarn(safeEvent, result);
		return new ScreeningResult(safeEvent, result, shouldWarn, false);
	}

	private Optional<MessageEvent> filterMuted(MessageEvent event) {
		if (event == null) {
			return Optional.empty();
		}
		if (mutePatternManager != null && mutePatternManager.shouldBlock(event.rawMessage())) {
			return Optional.empty();
		}
		return Optional.of(event);
	}

	private void addAiSignal(List<Signal> signals, BehaviorAnalysis analysis) {
		Signal signal = aiScorer.score(analysis);
		if (signal != null) {
			signals.add(signal);
		}
	}

	private static List<Signal> collectBehaviorSignals(BehaviorAnalysis analysis) {
		if (analysis == null) {
			return List.of();
		}

		List<Signal> signals = new ArrayList<>();
		if (analysis.pushesExternalPlatform() && ScamRules.isRuleEnabled(ScamRules.ScamRule.EXTERNAL_PLATFORM_PUSH)) {
			signals.add(new Signal(
				ScamRules.ScamRule.EXTERNAL_PLATFORM_PUSH.name(),
				SignalSource.BEHAVIOR,
				15,
				"Behavior flag pushesExternalPlatform=true (+15)",
				ScamRules.ScamRule.EXTERNAL_PLATFORM_PUSH,
				List.of()
			));
		}
		if (analysis.demandsUpfrontPayment() && ScamRules.isRuleEnabled(ScamRules.ScamRule.UPFRONT_PAYMENT)) {
			signals.add(new Signal(
				ScamRules.ScamRule.UPFRONT_PAYMENT.name(),
				SignalSource.BEHAVIOR,
				25,
				"Behavior flag demandsUpfrontPayment=true (+25)",
				ScamRules.ScamRule.UPFRONT_PAYMENT,
				List.of()
			));
		}
		if (analysis.requestsSensitiveData() && ScamRules.isRuleEnabled(ScamRules.ScamRule.ACCOUNT_DATA_REQUEST)) {
			signals.add(new Signal(
				ScamRules.ScamRule.ACCOUNT_DATA_REQUEST.name(),
				SignalSource.BEHAVIOR,
				35,
				"Behavior flag requestsSensitiveData=true (+35)",
				ScamRules.ScamRule.ACCOUNT_DATA_REQUEST,
				List.of()
			));
		}
		if (analysis.claimsTrustedMiddlemanWithoutProof() && ScamRules.isRuleEnabled(ScamRules.ScamRule.FAKE_MIDDLEMAN_CLAIM)) {
			signals.add(new Signal(
				ScamRules.ScamRule.FAKE_MIDDLEMAN_CLAIM.name(),
				SignalSource.BEHAVIOR,
				20,
				"Behavior flag claimsTrustedMiddlemanWithoutProof=true (+20)",
				ScamRules.ScamRule.FAKE_MIDDLEMAN_CLAIM,
				List.of()
			));
		}
		if (analysis.repeatedContactAttempts() >= 3 && ScamRules.isRuleEnabled(ScamRules.ScamRule.SPAMMY_CONTACT_PATTERN)) {
			signals.add(new Signal(
				ScamRules.ScamRule.SPAMMY_CONTACT_PATTERN.name(),
				SignalSource.BEHAVIOR,
				10,
				"Repeated contact attempts=" + analysis.repeatedContactAttempts() + " (threshold: 3, +10)",
				ScamRules.ScamRule.SPAMMY_CONTACT_PATTERN,
				analysis.repeatedContactMessages() == null ? List.of() : analysis.repeatedContactMessages()
			));
		}
		return signals;
	}

	private List<Signal> collectTrendSignals(MessageEvent event, List<Signal> existingSignals) {
		if (!ScamRules.isRuleEnabled(ScamRules.ScamRule.MULTI_MESSAGE_PATTERN)) {
			trendStore.evaluate(event, existingSignals);
			return List.of();
		}

		TrendStore.TrendEvaluation evaluation = trendStore.evaluate(event, existingSignals);
		if (evaluation.detail() == null) {
			return List.of();
		}
		return List.of(new Signal(
			ScamRules.ScamRule.MULTI_MESSAGE_PATTERN.name(),
			SignalSource.TREND,
			evaluation.bonusScore(),
			evaluation.detail(),
			ScamRules.ScamRule.MULTI_MESSAGE_PATTERN,
			evaluation.evaluatedMessages()
		));
	}

	private static DetectionResult scoreDetection(MessageEvent event, List<Signal> signals) {
		List<Signal> safeSignals = signals == null ? List.of() : List.copyOf(signals);
		double total = safeSignals.stream().mapToDouble(Signal::weight).sum();
		boolean hasTrendBonus = safeSignals.stream().anyMatch(signal -> signal.ruleId() == ScamRules.ScamRule.MULTI_MESSAGE_PATTERN);
		if (hasTrendBonus) {
			total = Math.min(100, total);
		}

		DetectionLevel level = DetectionLevel.fromScore(total);
		Map<ScamRules.ScamRule, String> ruleDetails = new LinkedHashMap<>();
		Set<ScamRules.ScamRule> triggeredRules = new LinkedHashSet<>();
		List<String> evaluatedMessages = new ArrayList<>();

		for (Signal signal : safeSignals) {
			if (signal.ruleId() != null) {
				triggeredRules.add(signal.ruleId());
				if (signal.evidence() != null && !signal.evidence().isBlank()) {
					ruleDetails.merge(signal.ruleId(), signal.evidence(), (existing, added) -> existing + "\n" + added);
				}
			}
			if (!signal.relatedMessages().isEmpty()) {
				evaluatedMessages.addAll(signal.relatedMessages());
			}
		}

		if (evaluatedMessages.isEmpty() && event != null && event.rawMessage() != null && !event.rawMessage().isBlank()) {
			evaluatedMessages.add(event.rawMessage());
		}

		boolean shouldCapture = shouldAutoCapture(level, total, triggeredRules);
		return new DetectionResult(total, level, safeSignals, ruleDetails, shouldCapture, evaluatedMessages);
	}

	private static boolean shouldAutoCapture(DetectionLevel level, double totalScore, Set<ScamRules.ScamRule> rules) {
		if (level == null || totalScore <= 0 || rules == null || rules.isEmpty()) {
			return false;
		}
		String setting = ScamRules.autoCaptureAlertLevelSetting();
		if (setting == null || setting.isBlank() || setting.equalsIgnoreCase("OFF")) {
			return false;
		}
		try {
			ScamRules.ScamRiskLevel minimum = ScamRules.ScamRiskLevel.valueOf(setting.trim().toUpperCase(Locale.ROOT));
			return DetectionLevel.toRiskLevel(level).ordinal() >= minimum.ordinal();
		} catch (IllegalArgumentException ignored) {
			return false;
		}
	}

	private static void emitWarning(MessageEvent event, DetectionResult result, Consumer<Component> reply, Runnable warningSound) {
		if (result == null) {
			return;
		}
		if (reply != null) {
			reply.accept(RiskMessages.behaviorRiskWarning(event == null ? null : event.playerName(), result));
		}
		if (warningSound != null) {
			warningSound.run();
		}
	}

	private boolean shouldWarn(MessageEvent event, DetectionResult result) {
		if (result == null) {
			return false;
		}
		if (result.totalScore() <= 0 || result.triggeredRules().isEmpty()) {
			return false;
		}
		ScamRules.ScamRiskLevel minimum = ScamRules.minimumAlertRiskLevel();
		ScamRules.ScamRiskLevel actual = DetectionLevel.toRiskLevel(result.level());
		if (actual.ordinal() < minimum.ordinal()) {
			return false;
		}
		return markWarningSeen(event, result.level());
	}

	private boolean markWarningSeen(MessageEvent event, DetectionLevel level) {
		if (event == null || event.playerName() == null || event.playerName().isBlank() || level == null) {
			return false;
		}
		String key = "behavior-risk:" + event.playerName().toLowerCase(Locale.ROOT) + ":" + level.name();
		return seenWarnings.add(key);
	}

	/**
	 * Clears any stateful stage data (trend history, dedupe, repeated-contact counts).
	 */
	public void reset() {
		behaviorAnalyzer.reset();
		seenWarnings.clear();
		trendStore.reset();
	}
}
