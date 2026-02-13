package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import eu.tango.scamscreener.pipeline.core.DetectionScoring;
import eu.tango.scamscreener.pipeline.model.DetectionLevel;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.Signal;

public final class ScoringStage {
	private final DoubleFunction<DetectionLevel> levelMapper;
	private final Supplier<String> autoCaptureSettingSupplier;
	private final Function<DetectionLevel, ScamRules.ScamRiskLevel> riskLevelMapper;

	public ScoringStage() {
		this(DetectionScoring::mapLevel, ScamRules::autoCaptureAlertLevelSetting, DetectionScoring::toScamRiskLevel);
	}

	ScoringStage(
		DoubleFunction<DetectionLevel> levelMapper,
		Supplier<String> autoCaptureSettingSupplier,
		Function<DetectionLevel, ScamRules.ScamRiskLevel> riskLevelMapper
	) {
		this.levelMapper = levelMapper;
		this.autoCaptureSettingSupplier = autoCaptureSettingSupplier;
		this.riskLevelMapper = riskLevelMapper;
	}

	/**
	 * Sums signal weights, maps the total to a {@link DetectionLevel},
	 * and builds a {@link DetectionResult} with rule details.
	 */
	public DetectionResult score(MessageEvent event, List<Signal> signals) {
		List<Signal> safeSignals = signals == null ? List.of() : List.copyOf(signals);
		double total = safeSignals.stream().mapToDouble(Signal::weight).sum();
		boolean hasConversationBonus = safeSignals.stream().anyMatch(signal ->
			signal.ruleId() == ScamRules.ScamRule.MULTI_MESSAGE_PATTERN
				|| signal.ruleId() == ScamRules.ScamRule.FUNNEL_SEQUENCE_PATTERN
				|| signal.ruleId() == ScamRules.ScamRule.LOCAL_AI_FUNNEL_SIGNAL
		);
		if (hasConversationBonus) {
			total = Math.min(100, total);
		}

		DetectionLevel level = levelMapper.apply(total);
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

	/**
	 * Converts the detection level into the configured auto-capture threshold.
	 */
	private boolean shouldAutoCapture(DetectionLevel level, double totalScore, Set<ScamRules.ScamRule> rules) {
		if (level == null || totalScore <= 0 || rules == null || rules.isEmpty()) {
			return false;
		}
		String setting = autoCaptureSettingSupplier.get();
		if (setting == null || setting.isBlank() || setting.equalsIgnoreCase("OFF")) {
			return false;
		}
		// Funnel-based positives are especially useful for model training, even when
		// the overall score stays below the normal alert threshold.
		if (rules.contains(ScamRules.ScamRule.FUNNEL_SEQUENCE_PATTERN)
			|| rules.contains(ScamRules.ScamRule.LOCAL_AI_FUNNEL_SIGNAL)) {
			return true;
		}
		try {
			ScamRules.ScamRiskLevel minimum = ScamRules.ScamRiskLevel.valueOf(setting.trim().toUpperCase(java.util.Locale.ROOT));
			ScamRules.ScamRiskLevel actual = riskLevelMapper.apply(level);
			if (actual == null) {
				return false;
			}
			return actual.ordinal() >= minimum.ordinal();
		} catch (IllegalArgumentException ignored) {
			return false;
		}
	}
}
