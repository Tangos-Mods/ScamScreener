package eu.tango.scamscreener.ui;

import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.rules.ScamRules;

import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Messages {
	private static final String PREFIX = "[ScamScreener] ";
	private static final int PREFIX_LIGHT_RED = 0xFF5555;
	private static final String WARNING_BORDER = "====================================";
	private static final int WARNING_WIDTH = WARNING_BORDER.length();
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

	private Messages() {
	}

	public static MutableComponent addedToBlacklist(String name, UUID uuid) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was added to the blacklist."));
	}

	public static MutableComponent addedToBlacklistWithScore(String name, UUID uuid, int score) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was added with score "))
			.append(Component.literal(String.valueOf(score)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal("."));
	}

	public static MutableComponent addedToBlacklistWithMetadata(String name, UUID uuid) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was added with metadata."));
	}

	public static MutableComponent alreadyBlacklisted(String name, UUID uuid) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" is already blacklisted."));
	}

	public static MutableComponent removedFromBlacklist(String name, UUID uuid) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was removed from the blacklist."));
	}

	public static MutableComponent notOnBlacklist(String name, UUID uuid) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" is not on the blacklist."));
	}

	public static MutableComponent blacklistEmpty() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("The blacklist is empty."));
	}

	public static MutableComponent blacklistHeader() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Blacklist entries:"));
	}

	public static Component blacklistEntry(BlacklistManager.ScamEntry entry) {
		return Component.literal(entry.name()).withStyle(ChatFormatting.AQUA)
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(String.valueOf(entry.score())).withStyle(ChatFormatting.DARK_RED))
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(entry.reason()).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(formatTimestamp(entry.addedAt())).withStyle(ChatFormatting.GREEN));
	}

	public static MutableComponent unresolvedTarget(String input) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Could not resolve '"))
			.append(Component.literal(input).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("'. Tried UUID, online player list, local blacklist, and Mojang lookup."));
	}

	public static MutableComponent noChatToCapture() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("No chat line captured yet. Wait for a message, then run the capture command."));
	}

	public static MutableComponent trainingSampleSaved(String path, int label) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Saved training sample with label "))
			.append(Component.literal(String.valueOf(label)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal(" to "))
			.append(clickablePath(path))
			.append(Component.literal("."));
	}

	public static MutableComponent trainingSamplesSaved(String path, int label, int count) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Saved "))
			.append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(" training samples with label "))
			.append(Component.literal(String.valueOf(label)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal(" to "))
			.append(clickablePath(path))
			.append(Component.literal("."));
	}

	private static MutableComponent clickablePath(String path) {
		String safePath = path == null ? "" : path;
		return Component.literal(safePath).setStyle(
			Style.EMPTY
				.withColor(ChatFormatting.YELLOW)
				.withUnderlined(true)
				.withClickEvent(new ClickEvent.OpenFile(safePath))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to open this file in Explorer")))
		);
	}

	public static MutableComponent trainingSaveFailed(String errorMessage) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Failed to save training sample: "))
			.append(Component.literal(errorMessage == null ? "unknown error" : errorMessage).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent trainingSamplesSaveFailed(String errorMessage) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Failed to save training samples: "))
			.append(Component.literal(errorMessage == null ? "unknown error" : errorMessage).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent trainingCompleted(int sampleCount, int positiveCount, String archivedFilename) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Trained local AI model with "))
			.append(Component.literal(String.valueOf(sampleCount)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(" samples ("))
			.append(Component.literal(String.valueOf(positiveCount)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal(" scam). Archived data to "))
			.append(Component.literal(archivedFilename == null ? "unknown" : archivedFilename).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("."));
	}

	public static MutableComponent trainingFailed(String errorMessage) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Training failed: "))
			.append(Component.literal(errorMessage == null ? "unknown error" : errorMessage).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent mojangLookupStarted(String input) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Resolving '"))
			.append(Component.literal(input == null ? "" : input).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("' via Mojang API... try again in a moment."));
	}

	public static MutableComponent mojangLookupCompleted(String input, String resolvedName) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Mojang lookup for '"))
			.append(Component.literal(input == null ? "" : input).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("' completed. Run command again to use "))
			.append(Component.literal(resolvedName == null ? "unknown" : resolvedName).withStyle(ChatFormatting.AQUA))
			.append(Component.literal("."));
	}

	public static MutableComponent commandHelp() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Commands:"))
			.append(Component.literal("\n- /scamscreener add <player> [score] [reason]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener remove <player>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener list").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai capture <player> <scam|legit> [count]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai train").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai reset").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai autocapture [off|low|medium|high|critical]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener rules <list|disable|enable> [rule]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener alertlevel [low|medium|high|critical]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener preview").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent aiCommandHelp() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("AI Commands:"))
			.append(Component.literal("\n- /scamscreener ai capture <player> <scam|legit> [count]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai train").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai reset").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai autocapture [off|low|medium|high|critical]").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent addCommandHelp() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Usage: "))
			.append(Component.literal("/scamscreener add <player> [score] [reason]").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent removeCommandHelp() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Usage: "))
			.append(Component.literal("/scamscreener remove <player>").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent aiCaptureCommandHelp() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Usage: "))
			.append(Component.literal("/scamscreener ai capture <player> <scam|legit> [count]").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent ruleCommandHelp() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Rule command usage:"))
			.append(Component.literal("\n- /scamscreener rules list").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener rules disable <rule>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener rules enable <rule>").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent disabledRulesList(Set<ScamRules.ScamRule> disabled) {
		String list = (disabled == null || disabled.isEmpty())
			? "none"
			: disabled.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Disabled rules: "))
			.append(Component.literal(list).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent invalidRuleName() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Invalid rule name. Use autocomplete or /scamscreener rules list.").withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent ruleDisabled(ScamRules.ScamRule rule) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Rule disabled: "))
			.append(Component.literal(rule.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent ruleEnabled(ScamRules.ScamRule rule) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Rule enabled: "))
			.append(Component.literal(rule.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent ruleAlreadyDisabled(ScamRules.ScamRule rule) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Rule is already disabled: "))
			.append(Component.literal(rule.name()).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent ruleNotDisabled(ScamRules.ScamRule rule) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Rule is not disabled: "))
			.append(Component.literal(rule.name()).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent localAiModelReset() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Local AI model was reset to default weights.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent currentAutoCaptureAlertLevel(String level) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Auto-capture on alerts: "))
			.append(Component.literal(level == null ? "HIGH" : level).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent updatedAutoCaptureAlertLevel(String level) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Auto-capture level updated to "))
			.append(Component.literal(level == null ? "HIGH" : level).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal("."));
	}

	public static MutableComponent invalidAutoCaptureAlertLevel() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Invalid auto-capture level. Use: off, low, medium, high, critical.").withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent currentAlertRiskLevel(ScamRules.ScamRiskLevel level) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Current alert threshold: "))
			.append(Component.literal(level == null ? "HIGH" : level.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent updatedAlertRiskLevel(ScamRules.ScamRiskLevel level) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Alert threshold updated to "))
			.append(Component.literal(level == null ? "HIGH" : level.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal("."));
	}

	public static MutableComponent behaviorRiskWarning(String playerName, ScamRules.ScamAssessment assessment) {
		String levelText = assessment.riskLevel().name();
		String playerText = playerName == null ? "unknown" : playerName;
		String scoreText = String.valueOf(assessment.riskScore());
		String playerScoreLine = playerText + " | " + scoreText;
		int scoreColor = scoreGradientColor(assessment.riskScore());
		MutableComponent message = Component.empty()
			.append(Component.literal(WARNING_BORDER).withStyle(ChatFormatting.DARK_RED))
			.append(Component.literal("\n" + centered("RISKY MESSAGE")).withStyle(style -> style.withColor(levelColor(assessment.riskLevel())).withBold(true)))
			.append(Component.literal("\n" + centered(levelText)).withStyle(style -> style.withColor(levelColor(assessment.riskLevel())).withBold(true)))
			.append(Component.literal("\n" + leftCenterPadding(playerScoreLine)))
			.append(Component.literal(playerText).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(scoreText).withStyle(style -> style.withColor(scoreColor)))
			.append(Component.literal("\n"));

		if (assessment.triggeredRules().isEmpty()) {
			message.append(Component.literal(centered("none")).withStyle(ChatFormatting.GRAY));
			return message.append(Component.literal("\n" + WARNING_BORDER).withStyle(ChatFormatting.DARK_RED));
		}

		boolean first = true;
		for (ScamRules.ScamRule rule : assessment.triggeredRules().stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList())) {
			if (!first) {
				message.append(Component.literal(", ").withStyle(ChatFormatting.DARK_GRAY));
			}
			first = false;
			message.append(readableRule(rule, assessment.detailFor(rule), assessment.evaluatedMessage()));
		}

		return message.append(Component.literal("\n" + WARNING_BORDER).withStyle(ChatFormatting.DARK_RED));
	}

	private static String centered(String text) {
		if (text == null) {
			return "";
		}
		String padding = leadingPadding(text);
		if (padding.isEmpty()) {
			return text;
		}
		return padding + text;
	}

	private static String leftCenterPadding(String text) {
		return leadingPadding(text);
	}

	private static String leadingPadding(String text) {
		if (text == null || text.isEmpty()) {
			return "";
		}

		Minecraft client = Minecraft.getInstance();
		if (client != null && client.font != null) {
			int targetWidth = client.font.width(WARNING_BORDER);
			int textWidth = client.font.width(text);
			if (textWidth >= targetWidth) {
				return "";
			}
			int spaceWidth = Math.max(1, client.font.width(" "));
			int leftPixels = (targetWidth - textWidth) / 2;
			return " ".repeat(Math.max(0, leftPixels / spaceWidth));
		}

		if (text.length() >= WARNING_WIDTH) {
			return "";
		}
		int leftPadding = (WARNING_WIDTH - text.length()) / 2;
		return " ".repeat(Math.max(0, leftPadding));
	}

	private static int scoreGradientColor(int score) {
		int clamped = Math.max(0, Math.min(100, score));
		int fade = 255 - (int) Math.round((clamped / 100.0) * 255.0);
		return (255 << 16) | (fade << 8) | fade;
	}

	private static int levelColor(ScamRules.ScamRiskLevel level) {
		return switch (level) {
			case LOW -> 0xFFB3B3;
			case MEDIUM -> 0xFF8080;
			case HIGH -> 0xFF4D4D;
			case CRITICAL -> 0xB30000;
		};
	}

	private static MutableComponent readableRule(ScamRules.ScamRule rule, String exactDetail, String evaluatedMessage) {
		String name = switch (rule) {
			case SUSPICIOUS_LINK -> "Suspicious Link";
			case PRESSURE_AND_URGENCY -> "Pressure/Urgency";
			case UPFRONT_PAYMENT -> "Upfront Payment";
			case ACCOUNT_DATA_REQUEST -> "Account Data Request";
			case EXTERNAL_PLATFORM_PUSH -> "External Platform Push";
			case FAKE_MIDDLEMAN_CLAIM -> "Fake Middleman Claim";
			case TOO_GOOD_TO_BE_TRUE -> "Too Good To Be True";
			case TRUST_MANIPULATION -> "Trust Manipulation";
			case SPAMMY_CONTACT_PATTERN -> "Spammy Contact Pattern";
			case MULTI_MESSAGE_PATTERN -> "Multi-Message Pattern";
			case LOCAL_AI_RISK_SIGNAL -> "Local AI Risk Signal";
		};

		String detail = exactDetail == null || exactDetail.isBlank()
			? "No detailed trigger context available."
			: exactDetail;

		MutableComponent hover = Component.literal("ScamScreener Rule").withStyle(style -> style.withColor(ChatFormatting.DARK_RED).withBold(true))
			.append(Component.literal("\nRule: ").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)))
			.append(Component.literal(name).withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(false)))
			.append(Component.literal("\nWhy triggered:").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)))
			.append(Component.literal("\n" + detail).withStyle(style -> style.withColor(ChatFormatting.YELLOW).withBold(false)))
			.append(Component.literal("\nEvaluated message:").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)))
			.append(Component.literal("\n" + compactHoverMessage(evaluatedMessage)).withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(false)));

		return Component.literal(name).setStyle(
			Style.EMPTY
				.withColor(ChatFormatting.YELLOW)
				.withHoverEvent(new HoverEvent.ShowText(hover))
		);
	}

	public static MutableComponent blacklistWarning(String playerName, String triggerReason, BlacklistManager.ScamEntry entry) {
		String score = entry == null ? "n/a" : String.valueOf(entry.score());
		String reason = entry == null ? "n/a" : entry.reason();
		String addedAt = entry == null ? "n/a" : formatTimestamp(entry.addedAt());
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("SCAM WARNING").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal("\n- Player: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(playerName == null ? "n/a" : playerName).withStyle(ChatFormatting.AQUA))
			.append(Component.literal("\n- Score: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(score).withStyle(ChatFormatting.DARK_RED))
			.append(Component.literal("\n- Reason: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(reason).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("\n- Added At: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(addedAt).withStyle(ChatFormatting.GREEN))
			.append(Component.literal("\n- Trigger: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(triggerReason == null ? "n/a" : triggerReason).withStyle(ChatFormatting.GOLD));
	}

	private static String formatTimestamp(String input) {
		if (input == null || input.isBlank()) {
			return "n/a";
		}
		try {
			return TIMESTAMP_FORMATTER.format(Instant.parse(input));
		} catch (Exception ignored) {
			return input;
		}
	}

	private static String compactHoverMessage(String input) {
		if (input == null || input.isBlank()) {
			return "n/a";
		}
		String singleLine = input.replace('\n', ' ').replace('\r', ' ').trim();
		if (singleLine.length() <= 180) {
			return singleLine;
		}
		return singleLine.substring(0, 177) + "...";
	}
}
