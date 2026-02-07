package eu.tango.scamscreener.ui.messages;

import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.pipeline.model.DetectionLevel;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.MessageFlagging;
import eu.tango.scamscreener.util.TimestampFormatUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RiskMessages extends StyledMessages {
	private static final String WARNING_BORDER = "====================================";
	private static final int WARNING_WIDTH = WARNING_BORDER.length();

	private RiskMessages() {
	}

	public static MutableComponent behaviorRiskWarning(String playerName, DetectionResult result) {
		RiskMessageData data = RiskMessageData.from(playerName, result);
		String levelText = data.level().name();
		String playerText = data.playerName();
		String scoreText = String.valueOf(data.score());
		String playerScoreLine = playerText + " | " + scoreText;
		int scoreColor = scoreGradientColor(data.score());

		MutableComponent message = Component.empty()
			.append(Component.literal(WARNING_BORDER).withStyle(ChatFormatting.DARK_RED))
			.append(Component.literal("\n" + centered("RISKY MESSAGE", WARNING_BORDER, WARNING_WIDTH)).withStyle(style -> style.withColor(levelColor(data.level())).withBold(true)))
			.append(Component.literal("\n" + centered(levelText, WARNING_BORDER, WARNING_WIDTH)).withStyle(style -> style.withColor(levelColor(data.level())).withBold(true)))
			.append(Component.literal("\n" + leftCenterPadding(playerScoreLine, WARNING_BORDER, WARNING_WIDTH)))
			.append(Component.literal(playerText).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(scoreText).withStyle(style -> style.withColor(scoreColor)))
			.append(Component.literal("\n"));

		if (!data.hasRules()) {
			message.append(Component.literal(centered("none", WARNING_BORDER, WARNING_WIDTH)).withStyle(ChatFormatting.GRAY));
			return message.append(Component.literal("\n" + WARNING_BORDER).withStyle(ChatFormatting.DARK_RED));
		}

		boolean first = true;
		for (ScamRules.ScamRule rule : data.orderedRules()) {
			if (!first) {
				message.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
			}
			first = false;
			message.append(formatRuleTag(rule, data.detailFor(rule), data.evaluatedMessages()));
		}

		message.append(Component.literal("\n")).append(actionLine(data));
		return message.append(Component.literal("\n" + WARNING_BORDER).withStyle(ChatFormatting.DARK_RED));
	}

	public static MutableComponent blacklistWarning(String playerName, String triggerReason, BlacklistManager.ScamEntry entry) {
		String safePlayer = playerName == null ? "n/a" : playerName;
		String scoreText = entry == null ? "n/a" : String.valueOf(entry.score());
		String reason = entry == null ? "n/a" : entry.reason();
		String addedAt = entry == null ? "n/a" : TimestampFormatUtil.formatIsoOrRaw(entry.addedAt());
		int scoreValue = entry == null ? 0 : entry.score();
		int scoreColor = scoreGradientColor(scoreValue);
		String playerScoreLine = safePlayer + " | " + scoreText;

		MutableComponent message = Component.empty()
			.append(Component.literal(WARNING_BORDER).withStyle(ChatFormatting.DARK_RED))
			.append(Component.literal("\n" + centered("SCAM WARNING", WARNING_BORDER, WARNING_WIDTH)).withStyle(style -> style.withColor(ChatFormatting.DARK_RED).withBold(true)))
			.append(Component.literal("\n" + leftCenterPadding(playerScoreLine, WARNING_BORDER, WARNING_WIDTH)))
			.append(Component.literal(safePlayer).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(scoreText).withStyle(style -> style.withColor(scoreColor)))
			.append(Component.literal("\n" + centered(reason, WARNING_BORDER, WARNING_WIDTH)).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("\n" + centered(addedAt, WARNING_BORDER, WARNING_WIDTH)).withStyle(ChatFormatting.GREEN))
			.append(Component.literal("\n" + centered(triggerReason == null ? "n/a" : triggerReason, WARNING_BORDER, WARNING_WIDTH)).withStyle(ChatFormatting.GOLD));

		return message.append(Component.literal("\n" + WARNING_BORDER).withStyle(ChatFormatting.DARK_RED));
	}

	private static MutableComponent actionLine(RiskMessageData data) {
		MutableComponent line = Component.literal("Actions: ").withStyle(ChatFormatting.GRAY);
		String target = data.playerName() == null ? "" : data.playerName().trim();
		boolean canAct = !target.isBlank() && !"unknown".equalsIgnoreCase(target);
		String messageId = registerActionMessageId(data.evaluatedMessages());
		boolean canFlagMessage = messageId != null && !messageId.isBlank();

		line.append(actionTag(
			"legit",
			ChatFormatting.GREEN,
			"mark as legit message",
			canFlagMessage ? "/scamscreener ai flag " + messageId + " legit" : null
		));
		line.append(Component.literal(" "));
		line.append(actionTag(
			"scam",
			ChatFormatting.RED,
			"mark as scam message",
			canFlagMessage ? "/scamscreener ai flag " + messageId + " scam" : null
		));
		line.append(Component.literal(" "));
		line.append(actionTag(
			"blacklist",
			ChatFormatting.DARK_RED,
			blacklistHoverText(data),
			canAct ? buildBlacklistCommand(target, data) : null
		));
		return line;
	}

	private static MutableComponent actionTag(String label, ChatFormatting color, String hover, String command) {
		Style style = Style.EMPTY.withColor(color);
		if (hover != null && !hover.isBlank()) {
			style = style.withHoverEvent(new HoverEvent.ShowText(Component.literal(hover).withStyle(ChatFormatting.YELLOW)));
		}
		if (command != null && !command.isBlank()) {
			style = style.withClickEvent(new ClickEvent.RunCommand(command));
		} else {
			style = style.withStrikethrough(true);
		}
		return Component.literal("[" + label + "]").setStyle(style);
	}

	private static String buildBlacklistCommand(String target, RiskMessageData data) {
		if (target == null || target.isBlank()) {
			return null;
		}
		String reason = bestRuleCode(data);
		int clampedScore = Math.max(0, Math.min(100, data.score()));
		if (reason == null || reason.isBlank()) {
			return "/scamscreener add " + target + " " + clampedScore;
		}
		return "/scamscreener add " + target + " " + clampedScore + " \"" + escapeCommandReason(reason) + "\"";
	}

	private static String escapeCommandReason(String reason) {
		if (reason == null) {
			return "";
		}
		return reason.replace("\"", "\\\"");
	}

	private static String bestRuleCode(RiskMessageData data) {
		ScamRules.ScamRule best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		Map<ScamRules.ScamRule, Double> weights = data.ruleWeights();
		if (weights != null && !weights.isEmpty()) {
			for (Map.Entry<ScamRules.ScamRule, Double> entry : weights.entrySet()) {
				if (entry.getKey() == null || entry.getValue() == null) {
					continue;
				}
				double score = entry.getValue();
				if (best == null || score > bestScore) {
					best = entry.getKey();
					bestScore = score;
				}
			}
		}
		if (best == null && data.hasRules()) {
			best = data.orderedRules().stream().sorted(Comparator.naturalOrder()).findFirst().orElse(null);
		}
		return best == null ? null : best.name();
	}

	private static String blacklistHoverText(RiskMessageData data) {
		if (!data.hasRules()) {
			return "add player to blacklist";
		}
		StringBuilder out = new StringBuilder();
		String best = bestRuleCode(data);
		if (best != null) {
			out.append("Reason: ").append(best);
		}
		int count = 0;
		for (ScamRules.ScamRule rule : data.orderedRules()) {
			if (rule == null) {
				continue;
			}
			String detail = data.detailFor(rule);
			if (detail == null || detail.isBlank()) {
				detail = "n/a";
			}
			if (count == 0 && out.length() > 0) {
				out.append("\n");
			}
			out.append(rule.name()).append(": ").append(detail);
			count++;
		}
		return out.length() == 0 ? "add player to blacklist" : out.toString();
	}

	private static String registerActionMessageId(List<String> evaluatedMessages) {
		if (evaluatedMessages == null || evaluatedMessages.isEmpty()) {
			return null;
		}
		for (String raw : evaluatedMessages) {
			if (raw == null || raw.isBlank()) {
				continue;
			}
			String normalized = raw.replace('\n', ' ').replace('\r', ' ').trim();
			if (normalized.isBlank()) {
				continue;
			}
			return MessageFlagging.registerMessage(normalized);
		}
		return null;
	}

	private static MutableComponent formatRuleTag(ScamRules.ScamRule rule, String exactDetail, List<String> evaluatedMessages) {
		String name = readableRuleName(rule);
		String detail = exactDetail == null || exactDetail.isBlank()
			? "No detailed trigger context available."
			: exactDetail;

		MutableComponent hover = Component.literal("ScamScreener Rule").withStyle(style -> style.withColor(ChatFormatting.DARK_RED).withBold(true))
			.append(Component.literal("\nRule: ").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)))
			.append(Component.literal(name).withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(false)))
			.append(Component.literal("\nWhy triggered:").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)))
			.append(highlightQuoted(detail))
			.append(Component.literal("\nEvaluated message(s):").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)))
			.append(Component.literal("\n" + compactHoverMessages(evaluatedMessages)).withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(false)));

		return Component.literal(name).setStyle(
			Style.EMPTY
				.withColor(ChatFormatting.YELLOW)
				.withHoverEvent(new HoverEvent.ShowText(hover))
		);
	}

	private static String readableRuleName(ScamRules.ScamRule rule) {
		return switch (rule) {
			case SUSPICIOUS_LINK -> "Suspicious Link";
			case PRESSURE_AND_URGENCY -> "Pressure/Urgency";
			case UPFRONT_PAYMENT -> "Upfront Payment";
			case ACCOUNT_DATA_REQUEST -> "Account Data Request";
			case EXTERNAL_PLATFORM_PUSH -> "External Platform Push";
			case DISCORD_HANDLE -> "Discord Handle";
			case FAKE_MIDDLEMAN_CLAIM -> "Fake Middleman Claim";
			case TOO_GOOD_TO_BE_TRUE -> "Too Good To Be True";
			case TRUST_MANIPULATION -> "Trust Manipulation";
			case SPAMMY_CONTACT_PATTERN -> "Spammy Contact Pattern";
			case MULTI_MESSAGE_PATTERN -> "Multi-Message Pattern";
			case SIMILARITY_MATCH -> "Similarity Match";
			case LOCAL_AI_RISK_SIGNAL -> "Local AI Risk Signal";
		};
	}

	private static String compactHoverMessages(List<String> inputs) {
		if (inputs == null || inputs.isEmpty()) {
			return "n/a";
		}
		StringBuilder out = new StringBuilder();
		int limit = Math.min(4, inputs.size());
		for (int i = 0; i < limit; i++) {
			String input = inputs.get(i);
			if (input == null || input.isBlank()) {
				continue;
			}
			String singleLine = input.replace('\n', ' ').replace('\r', ' ').trim();
			if (singleLine.length() > 140) {
				singleLine = singleLine.substring(0, 137) + "...";
			}
			if (out.length() > 0) {
				out.append("\n");
			}
			out.append("- ").append(singleLine);
		}
		if (inputs.size() > limit) {
			out.append("\n- ... and ").append(inputs.size() - limit).append(" more");
		}
		return out.length() == 0 ? "n/a" : out.toString();
	}

	private static MutableComponent highlightQuoted(String detail) {
		if (detail == null || detail.isBlank()) {
			return Component.literal("\nNo detailed trigger context available.")
				.withStyle(style -> style.withColor(ChatFormatting.YELLOW).withBold(false));
		}
		String[] parts = detail.split("\"", -1);
		MutableComponent out = Component.literal("\n").withStyle(style -> style.withColor(ChatFormatting.YELLOW).withBold(false));
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			if (part.isEmpty()) {
				continue;
			}
			boolean quoted = (i % 2 == 1);
			out.append(Component.literal(part).withStyle(style -> style.withColor(quoted ? ChatFormatting.GOLD : ChatFormatting.YELLOW).withBold(quoted)));
		}
		return out;
	}

	private static String centered(String text, String border, int fallbackWidth) {
		if (text == null) {
			return "";
		}
		String padding = leadingPadding(text, border, fallbackWidth);
		if (padding.isEmpty()) {
			return text;
		}
		return padding + text;
	}

	private static String leftCenterPadding(String text, String border, int fallbackWidth) {
		return leadingPadding(text, border, fallbackWidth);
	}

	private static String leadingPadding(String text, String border, int fallbackWidth) {
		if (text == null || text.isEmpty()) {
			return "";
		}
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.font != null) {
			int targetWidth = client.font.width(border);
			int textWidth = client.font.width(text);
			if (textWidth >= targetWidth) {
				return "";
			}
			int spaceWidth = Math.max(1, client.font.width(" "));
			int leftPixels = (targetWidth - textWidth) / 2;
			return " ".repeat(Math.max(0, leftPixels / spaceWidth));
		}
		if (text.length() >= fallbackWidth) {
			return "";
		}
		int leftPadding = (fallbackWidth - text.length()) / 2;
		return " ".repeat(Math.max(0, leftPadding));
	}

	public static int scoreGradientColor(int score) {
		int clamped = Math.max(0, Math.min(100, score));
		int fade = 255 - (int) Math.round((clamped / 100.0) * 255.0);
		return (255 << 16) | (fade << 8) | fade;
	}

	public static int levelColor(ScamRules.ScamRiskLevel level) {
		return switch (level) {
			case LOW -> 0xFFB3B3;
			case MEDIUM -> 0xFF8080;
			case HIGH -> 0xFF4D4D;
			case CRITICAL -> 0xB30000;
		};
	}

	private record RiskMessageData(
		String playerName,
		int score,
		ScamRules.ScamRiskLevel level,
		Map<ScamRules.ScamRule, String> ruleDetails,
		List<String> evaluatedMessages,
		Map<ScamRules.ScamRule, Double> ruleWeights
	) {
		private static RiskMessageData from(String playerName, DetectionResult result) {
			if (result == null) {
				return new RiskMessageData("unknown", 0, ScamRules.ScamRiskLevel.LOW, Map.of(), List.of(), Map.of());
			}
			Map<ScamRules.ScamRule, String> details = result.triggeredRules() == null
				? Map.of()
				: new LinkedHashMap<>(result.triggeredRules());
			Map<ScamRules.ScamRule, Double> weights = new LinkedHashMap<>();
			if (result.signals() != null) {
				for (Signal signal : result.signals()) {
					if (signal == null || signal.ruleId() == null) {
						continue;
					}
					weights.merge(signal.ruleId(), signal.weight(), Math::max);
				}
			}
			return new RiskMessageData(
				playerName == null ? "unknown" : playerName,
				(int) Math.round(result.totalScore()),
				DetectionLevel.toRiskLevel(result.level()),
				details,
				result.evaluatedMessages() == null ? List.of() : result.evaluatedMessages(),
				weights
			);
		}

		private List<ScamRules.ScamRule> orderedRules() {
			return ruleDetails.keySet().stream().sorted(Comparator.naturalOrder()).toList();
		}

		private String detailFor(ScamRules.ScamRule rule) {
			return ruleDetails.get(rule);
		}

		private boolean hasRules() {
			return ruleDetails != null && !ruleDetails.isEmpty();
		}
	}
}
