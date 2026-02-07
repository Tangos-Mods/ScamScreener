package eu.tango.scamscreener.ui.messages;

import eu.tango.scamscreener.pipeline.model.DetectionLevel;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.rules.ScamRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;
import java.util.Map;

public final class ScreenMessages extends StyledMessages {
	private ScreenMessages() {
	}

	public static MutableComponent modeStatus(boolean enabled) {
		return prefixed()
			.append(gray("Screen mode "))
			.append(Component.literal(enabled ? "enabled" : "disabled").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY))
			.append(gray("."));
	}

	public static MutableComponent screeningHover(DetectionResult result, boolean warned, boolean muted) {
		MutableComponent hover = Component.literal("ScamScreener Screen")
			.withStyle(style -> style.withColor(ChatFormatting.DARK_RED).withBold(true));
		if (muted) {
			return hover.append(gray("\nMuted by pattern."));
		}
		if (result == null) {
			return hover.append(gray("\nNo screening result."));
		}

		int score = (int) Math.round(result.totalScore());
		int scoreColor = RiskMessages.scoreGradientColor(score);
		ScamRules.ScamRiskLevel level = DetectionLevel.toRiskLevel(result.level());

		hover.append(gray("\nScore: "))
			.append(Component.literal(String.valueOf(score)).withStyle(style -> style.withColor(scoreColor)))
			.append(darkGray(" | "))
			.append(Component.literal(level.name()).withStyle(ChatFormatting.GOLD))
			.append(gray("\nWarned: "))
			.append(Component.literal(warned ? "yes" : "no").withStyle(warned ? ChatFormatting.RED : ChatFormatting.DARK_GRAY))
			.append(gray("\nAuto-capture: "))
			.append(Component.literal(result.shouldCapture() ? "yes" : "no").withStyle(result.shouldCapture() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));

		appendRuleSection(hover, result);
		appendSignalSection(hover, result);
		return hover;
	}

	private static void appendRuleSection(MutableComponent hover, DetectionResult result) {
		if (result.triggeredRules() == null || result.triggeredRules().isEmpty()) {
			hover.append(gray("\nRules: none"));
			return;
		}

		hover.append(gray("\nRules:"));
		for (Map.Entry<ScamRules.ScamRule, String> entry : result.triggeredRules().entrySet()) {
			String rule = entry.getKey() == null ? "unknown" : entry.getKey().name();
			String detail = truncate(entry.getValue(), 140);
			String line = "- " + rule + (detail.isBlank() ? "" : ": " + detail);
			hover.append(Component.literal("\n" + line).withStyle(ChatFormatting.YELLOW));
		}
	}

	private static void appendSignalSection(MutableComponent hover, DetectionResult result) {
		if (result.signals() == null || result.signals().isEmpty()) {
			return;
		}

		hover.append(gray("\nSignals:"));
		int limit = Math.min(6, result.signals().size());
		for (int i = 0; i < limit; i++) {
			Signal signal = result.signals().get(i);
			if (signal == null) {
				continue;
			}
			String rule = signal.ruleId() == null ? "n/a" : signal.ruleId().name();
			String source = signal.source() == null ? "n/a" : signal.source().name();
			String evidence = truncate(signal.evidence(), 80);
			String weight = String.format(Locale.ROOT, "%.2f", signal.weight());
			String line = "- " + rule + " @" + source + " w=" + weight + (evidence.isBlank() ? "" : " | " + evidence);
			hover.append(Component.literal("\n" + line).withStyle(ChatFormatting.AQUA));
		}
		if (result.signals().size() > limit) {
			hover.append(Component.literal("\n- ... and " + (result.signals().size() - limit) + " more").withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	private static String truncate(String raw, int maxLength) {
		String value = raw == null ? "" : raw.trim();
		if (value.length() <= maxLength) {
			return value;
		}
		if (maxLength <= 3) {
			return value.substring(0, Math.max(0, maxLength));
		}
		return value.substring(0, maxLength - 3) + "...";
	}
}
