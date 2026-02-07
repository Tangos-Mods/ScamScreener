package eu.tango.scamscreener.ui.messages;

import eu.tango.scamscreener.rules.ScamRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CommandMessages extends StyledMessages {
	private CommandMessages() {
	}

	public static MutableComponent commandHelp() {
		return prefixedList(
			"Commands:",
			"/scamscreener add <player> [score] [reason]",
			"/scamscreener remove <player>",
			"/scamscreener list",
			"/scamscreener mute [pattern]",
			"/scamscreener unmute <pattern>",
			"/scamscreener screen [true|false]",
			"/scamscreener ai capture <player> <scam|legit> [count]",
			"/scamscreener ai train",
			"/scamscreener ai reset",
			"/scamscreener ai autocapture [off|low|medium|high|critical]",
			"/scamscreener rules <list|disable|enable> [rule]",
			"/scamscreener alertlevel [low|medium|high|critical]",
			"/scamscreener version",
			"/scamscreener preview"
		);
	}

	public static MutableComponent mutePatternList(List<String> patterns) {
		String joined = (patterns == null || patterns.isEmpty()) ? "none" : String.join(", ", patterns);
		return prefixed()
			.append(gray("Muted patterns: "))
			.append(yellow(joined));
	}

	public static MutableComponent muteEnabled() {
		return prefixedGray("Mute filter enabled.");
	}

	public static MutableComponent muteDisabled() {
		return prefixedGray("Mute filter disabled.");
	}

	public static MutableComponent emailSafetyBlocked(String bypassId) {
		return bypassBlocked("Email address detected. This could be a scam. ", bypassId);
	}

	public static MutableComponent discordSafetyBlocked(String bypassId) {
		return bypassBlocked("Discord link detected. This could be a scam. ", bypassId);
	}

	public static MutableComponent emailBypassSent() {
		return prefixedGray("Bypass sent.");
	}

	public static MutableComponent emailBypassExpired() {
		return prefixedGray("Bypass expired. Please resend the message.");
	}

	public static MutableComponent mutePatternAdded(String pattern) {
		return prefixed()
			.append(gray("Muted pattern added: "))
			.append(gold(safe(pattern)));
	}

	public static MutableComponent mutePatternAlreadyExists(String pattern) {
		return prefixed()
			.append(gray("Pattern already muted: "))
			.append(yellow(safe(pattern)));
	}

	public static MutableComponent mutePatternInvalid(String pattern) {
		return error("Invalid regex pattern.", "MUTE-REGEX-001", pattern);
	}

	public static MutableComponent mutePatternRemoved(String pattern) {
		return prefixed()
			.append(gray("Unmuted pattern: "))
			.append(gold(safe(pattern)));
	}

	public static MutableComponent mutePatternNotFound(String pattern) {
		return error("Pattern not found.", "MUTE-LOOKUP-001", pattern);
	}

	public static MutableComponent blockedMessagesSummary(int blockedCount, int intervalSeconds) {
		int safeInterval = Math.max(1, intervalSeconds);
		return prefixed()
			.append(gray("Muted "))
			.append(gray(String.valueOf(Math.max(0, blockedCount))))
			.append(gray(" messages in the last "))
			.append(gray(String.valueOf(safeInterval)))
			.append(gray("s."));
	}

	public static MutableComponent versionInfo(String modVersion, int aiVersion) {
		return prefixed()
			.append(gray("Mod Version: "))
			.append(gold(safe(modVersion, "unknown")))
			.append(gray(" | AI Model Version: "))
			.append(aqua(String.valueOf(aiVersion)));
	}

	public static MutableComponent aiCommandHelp() {
		return prefixedList(
			"AI Commands:",
			"/scamscreener ai capture <player> <scam|legit> [count]",
			"/scamscreener ai capturebulk <count>",
			"/scamscreener ai migrate",
			"/scamscreener ai model <download|accept|merge|ignore> <id>",
			"/scamscreener ai train",
			"/scamscreener ai reset",
			"/scamscreener ai autocapture [off|low|medium|high|critical]"
		);
	}

	public static MutableComponent addCommandHelp() {
		return usage("/scamscreener add <player> [score] [reason]");
	}

	public static MutableComponent removeCommandHelp() {
		return usage("/scamscreener remove <player>");
	}

	public static MutableComponent unresolvedTarget(String input) {
		return prefixed()
			.append(gray("Could not resolve '"))
			.append(yellow(safe(input)))
			.append(gray("'. Tried UUID, online player list, local blacklist, and Mojang lookup."));
	}

	public static MutableComponent mojangLookupStarted(String input) {
		return prefixed()
			.append(gray("Resolving '"))
			.append(yellow(safe(input)))
			.append(gray("' via Mojang API... try again in a moment."));
	}

	public static MutableComponent mojangLookupCompleted(String input, String resolvedName) {
		return prefixed()
			.append(gray("Mojang lookup for '"))
			.append(yellow(safe(input)))
			.append(gray("' completed. Run command again to use "))
			.append(aqua(safe(resolvedName, "unknown")))
			.append(gray("."));
	}

	public static MutableComponent aiCaptureCommandHelp() {
		return usage("/scamscreener ai capture <player> <scam|legit> [count]");
	}

	public static MutableComponent ruleCommandHelp() {
		return prefixedList(
			"Rule command usage:",
			"/scamscreener rules list",
			"/scamscreener rules disable <rule>",
			"/scamscreener rules enable <rule>"
		);
	}

	public static MutableComponent disabledRulesList(Set<ScamRules.ScamRule> disabled) {
		String list = (disabled == null || disabled.isEmpty())
			? "none"
			: disabled.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
		return prefixed()
			.append(gray("Disabled rules: "))
			.append(yellow(list));
	}

	public static MutableComponent invalidRuleName() {
		return error("Invalid rule name.", "RULE-NAME-001", "Use autocomplete or /scamscreener rules list.");
	}

	public static MutableComponent ruleDisabled(ScamRules.ScamRule rule) {
		return prefixed()
			.append(gray("Rule disabled: "))
			.append(goldBold(rule.name()));
	}

	public static MutableComponent ruleEnabled(ScamRules.ScamRule rule) {
		return prefixed()
			.append(gray("Rule enabled: "))
			.append(goldBold(rule.name()));
	}

	public static MutableComponent ruleAlreadyDisabled(ScamRules.ScamRule rule) {
		return prefixed()
			.append(gray("Rule is already disabled: "))
			.append(yellow(rule.name()));
	}

	public static MutableComponent ruleNotDisabled(ScamRules.ScamRule rule) {
		return prefixed()
			.append(gray("Rule is not disabled: "))
			.append(yellow(rule.name()));
	}

	public static MutableComponent currentAutoCaptureAlertLevel(String level) {
		return prefixed()
			.append(gray("Auto-capture on alerts: "))
			.append(goldBold(safe(level, "HIGH")));
	}

	public static MutableComponent updatedAutoCaptureAlertLevel(String level) {
		return prefixed()
			.append(gray("Auto-capture level updated to "))
			.append(goldBold(safe(level, "HIGH")))
			.append(gray("."));
	}

	public static MutableComponent invalidAutoCaptureAlertLevel() {
		return error("Invalid auto-capture level.", "AI-CAPTURE-001", "Use: off, low, medium, high, critical.");
	}

	public static MutableComponent currentAlertRiskLevel(ScamRules.ScamRiskLevel level) {
		String levelText = level == null ? "HIGH" : level.name();
		return prefixed()
			.append(gray("Current alert threshold: "))
			.append(goldBold(levelText));
	}

	public static MutableComponent updatedAlertRiskLevel(ScamRules.ScamRiskLevel level) {
		String levelText = level == null ? "HIGH" : level.name();
		return prefixed()
			.append(gray("Alert threshold updated to "))
			.append(goldBold(levelText))
			.append(gray("."));
	}

	private static MutableComponent usage(String command) {
		return prefixed()
			.append(gray("Usage: "))
			.append(gray(safe(command)));
	}

	private static MutableComponent bypassBlocked(String message, String bypassId) {
		String id = safe(bypassId);
		MutableComponent line = warningPrefixed(message);
		Style bypassStyle = Style.EMPTY
			.withColor(ChatFormatting.DARK_RED)
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Send anyway").withStyle(ChatFormatting.YELLOW)))
			.withClickEvent(new ClickEvent.RunCommand("/scamscreener bypass " + id));
		line.append(Component.literal("[BYPASS]").setStyle(bypassStyle));
		return line;
	}
}

