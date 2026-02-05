package eu.tango.scamscreener.ui;

import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.pipeline.core.DetectionScoring;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import eu.tango.scamscreener.ui.MessageFlagging;

public final class Messages {
	private static final String PREFIX = "[ScamScreener] ";
	private static final int PREFIX_LIGHT_RED = 0xFF5555;
	private static final String WARNING_BORDER = "====================================";
	private static final int WARNING_WIDTH = WARNING_BORDER.length();
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
	private static final int MAX_HOVER_MESSAGES = 3;

	private Messages() {
	}

	public static MutableComponent addedToBlacklist(String name, UUID uuid) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was added to the blacklist.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent addedToBlacklistWithScore(String name, UUID uuid, int score) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was added with score ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(score)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent addedToBlacklistWithMetadata(String name, UUID uuid) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was added with metadata.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent alreadyBlacklisted(String name, UUID uuid) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" is already blacklisted.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent removedFromBlacklist(String name, UUID uuid) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was removed from the blacklist.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent notOnBlacklist(String name, UUID uuid) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" is not on the blacklist.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent blacklistEmpty() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("The blacklist is empty.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent blacklistHeader() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Blacklist entries:").withStyle(ChatFormatting.GRAY));
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
			.append(Component.literal("Could not resolve '").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(input).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("'. Tried UUID, online player list, local blacklist, and Mojang lookup.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent noChatToCapture() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("No chat line captured yet. Wait for a message, then run the capture command.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingSampleSaved(String path, int label) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Saved training sample with label ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(label)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal(" to ").withStyle(ChatFormatting.GRAY))
			.append(clickablePath(path))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingSampleFlagged(String labelText) {
		String safeLabel = labelText == null ? "unknown" : labelText;
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Saved as ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(safeLabel).withStyle(ChatFormatting.GOLD))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingSamplesSaved(String path, int label, int count) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Saved ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(" training samples with label ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(label)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal(" to ").withStyle(ChatFormatting.GRAY))
			.append(clickablePath(path))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
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
			.append(Component.literal("Failed to save training sample: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(errorMessage == null ? "unknown error" : errorMessage).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent trainingSamplesSaveFailed(String errorMessage) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Failed to save training samples: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(errorMessage == null ? "unknown error" : errorMessage).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent trainingDataMigrated(int updatedRows) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Training data migrated. Updated ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(Math.max(0, updatedRows))).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(" rows.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingDataUpToDate() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Training data already up to date.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent modelUpdateAvailable(MutableComponent link) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(link == null ? Component.literal("Model update available.").withStyle(ChatFormatting.GRAY) : link);
	}

	public static MutableComponent modelUpdateDownloadLink(String command, String localVersion, String remoteVersion) {
		String localText = localVersion == null || localVersion.isBlank() ? "unknown" : localVersion.trim();
		String remoteText = remoteVersion == null || remoteVersion.isBlank() ? "unknown" : remoteVersion.trim();
		Style style = Style.EMPTY
			.withColor(ChatFormatting.YELLOW)
			.withClickEvent(new ClickEvent.RunCommand(command == null ? "" : command))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Download model update")));
		return Component.literal("A new AI Model is available. Click to update your local model. (" + localText + " -> " + remoteText + ")")
			.setStyle(style);
	}

	public static MutableComponent modelUpdateReady(MutableComponent actions) {
		MutableComponent line = Component.literal(PREFIX).withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Model update downloaded. ").withStyle(ChatFormatting.GRAY));
		if (actions != null) {
			line.append(actions);
		}
		return line;
	}

	public static MutableComponent modelUpdateApplied(String action) {
		String safe = action == null ? "applied" : action;
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Model update ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(safe).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent modelUpdateFailed(String message) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Model update failed: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(message == null ? "unknown error" : message).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent modelUpdateNotReady() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Model update not downloaded yet.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent modelUpdateNotFound() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Model update not found.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent modelUpdateIgnored() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Model update ignored.").withStyle(ChatFormatting.GRAY));
	}


	public static MutableComponent trainingCompleted(int sampleCount, int positiveCount, String archivedFilename) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Trained local AI model with ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(sampleCount)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(" samples (").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(positiveCount)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal(" scam). Archived data to ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(archivedFilename == null ? "unknown" : archivedFilename).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingFailed(String errorMessage) {
		String safe = errorMessage == null ? "unknown error" : errorMessage;
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Training failed. ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("[details]").withStyle(style -> style
				.withColor(ChatFormatting.YELLOW)
				.withHoverEvent(new HoverEvent.ShowText(Component.literal(safe).withStyle(ChatFormatting.GRAY)))));
	}

	public static MutableComponent mojangLookupStarted(String input) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Resolving '").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(input == null ? "" : input).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("' via Mojang API... try again in a moment.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent mojangLookupCompleted(String input, String resolvedName) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Mojang lookup for '").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(input == null ? "" : input).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("' completed. Run command again to use ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(resolvedName == null ? "unknown" : resolvedName).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent commandHelp() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Commands:"))
			.append(Component.literal("\n- /scamscreener add <player> [score] [reason]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener remove <player>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener list").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener mute [pattern]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener unmute <pattern>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai capture <player> <scam|legit> [count]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai train").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai reset").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai autocapture [off|low|medium|high|critical]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener rules <list|disable|enable> [rule]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener alertlevel [low|medium|high|critical]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener version").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener preview").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent mutePatternList(List<String> patterns) {
		String joined = (patterns == null || patterns.isEmpty())
			? "none"
			: String.join(", ", patterns);
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Muted patterns: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(joined).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent mutePatternAdded(String pattern) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Muted pattern added: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(pattern == null ? "" : pattern).withStyle(ChatFormatting.GOLD));
	}

	public static MutableComponent mutePatternAlreadyExists(String pattern) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Pattern already muted: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(pattern == null ? "" : pattern).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent mutePatternInvalid(String pattern) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Invalid regex pattern: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(pattern == null ? "" : pattern).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent mutePatternRemoved(String pattern) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Unmuted pattern: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(pattern == null ? "" : pattern).withStyle(ChatFormatting.GOLD));
	}

	public static MutableComponent mutePatternNotFound(String pattern) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Pattern not found: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(pattern == null ? "" : pattern).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent blockedMessagesSummary(int blockedCount, int intervalSeconds) {
		int safeInterval = Math.max(1, intervalSeconds);
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Muted ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(Math.max(0, blockedCount))).withStyle(ChatFormatting.GRAY))
			.append(Component.literal(" messages in the last ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(safeInterval)).withStyle(ChatFormatting.GRAY))
			.append(Component.literal("s.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent versionInfo(String modVersion, int aiVersion) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Mod Version: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(modVersion == null ? "unknown" : modVersion).withStyle(ChatFormatting.GOLD))
			.append(Component.literal(" | AI Model Version: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(aiVersion)).withStyle(ChatFormatting.AQUA));
	}

	public static MutableComponent aiCommandHelp() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("AI Commands:").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai capture <player> <scam|legit> [count]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai capturebulk <count>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai migrate").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai model <download|accept|merge|ignore> <id>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai train").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai reset").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai autocapture [off|low|medium|high|critical]").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent addCommandHelp() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Usage: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("/scamscreener add <player> [score] [reason]").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent removeCommandHelp() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Usage: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("/scamscreener remove <player>").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent aiCaptureCommandHelp() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Usage: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("/scamscreener ai capture <player> <scam|legit> [count]").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent ruleCommandHelp() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Rule command usage:").withStyle(ChatFormatting.GRAY))
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
			.append(Component.literal("Disabled rules: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(list).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent invalidRuleName() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Invalid rule name. Use autocomplete or /scamscreener rules list.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent ruleDisabled(ScamRules.ScamRule rule) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Rule disabled: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(rule.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent ruleEnabled(ScamRules.ScamRule rule) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Rule enabled: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(rule.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent ruleAlreadyDisabled(ScamRules.ScamRule rule) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Rule is already disabled: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(rule.name()).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent ruleNotDisabled(ScamRules.ScamRule rule) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Rule is not disabled: ").withStyle(ChatFormatting.GRAY))
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
			.append(Component.literal("Auto-capture on alerts: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(level == null ? "HIGH" : level).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent updatedAutoCaptureAlertLevel(String level) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Auto-capture level updated to ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(level == null ? "HIGH" : level).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent invalidAutoCaptureAlertLevel() {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Invalid auto-capture level. Use: off, low, medium, high, critical.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent currentAlertRiskLevel(ScamRules.ScamRiskLevel level) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Current alert threshold: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(level == null ? "HIGH" : level.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent updatedAlertRiskLevel(ScamRules.ScamRiskLevel level) {
		return Component.literal(PREFIX)
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.append(Component.literal("Alert threshold updated to ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(level == null ? "HIGH" : level.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent behaviorRiskWarning(String playerName, DetectionResult result) {
		if (result == null) {
			return behaviorRiskWarning(playerName, new ScamRules.ScamAssessment(0, ScamRules.ScamRiskLevel.LOW, Set.of(), Map.of(), null, List.of()));
		}
		int score = (int) Math.round(result.totalScore());
		ScamRules.ScamRiskLevel level = DetectionScoring.toScamRiskLevel(result.level());
		Set<ScamRules.ScamRule> rules = result.triggeredRules().isEmpty() ? Set.of() : EnumSet.copyOf(result.triggeredRules().keySet());
		Map<ScamRules.ScamRule, String> details = result.triggeredRules().isEmpty() ? Map.of() : new LinkedHashMap<>(result.triggeredRules());
		List<String> evaluatedMessages = result.evaluatedMessages();
		ScamRules.ScamAssessment assessment = new ScamRules.ScamAssessment(score, level, rules, details, evaluatedMessages.isEmpty() ? null : evaluatedMessages.get(0), evaluatedMessages);
		return behaviorRiskWarning(playerName, assessment);
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
			message.append(readableRule(rule, assessment.detailFor(rule), assessment.allEvaluatedMessages()));
		}

		String actionMessageId = registerActionMessageId(assessment.allEvaluatedMessages());
		message.append(Component.literal("\n")).append(actionLine(playerText, actionMessageId));

		List<String> evaluatedMessages = assessment.allEvaluatedMessages();
		List<String> highlightTerms = extractQuotedMatches(assessment);
		if (!evaluatedMessages.isEmpty()) {
			int limit = Math.min(MAX_HOVER_MESSAGES, evaluatedMessages.size());
			for (int i = 0; i < limit; i++) {
				String raw = evaluatedMessages.get(i);
				if (raw == null || raw.isBlank()) {
					continue;
				}
			}
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

	private static MutableComponent readableRule(ScamRules.ScamRule rule, String exactDetail, List<String> evaluatedMessages) {
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
			.append(highlightQuoted(detail))
			.append(Component.literal("\nEvaluated message(s):").withStyle(style -> style.withColor(ChatFormatting.GRAY).withBold(false)))
			.append(Component.literal("\n" + compactHoverMessages(evaluatedMessages)).withStyle(style -> style.withColor(ChatFormatting.AQUA).withBold(false)));

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
			return Component.literal("\nNo detailed trigger context available.").withStyle(style -> style.withColor(ChatFormatting.YELLOW).withBold(false));
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

	private static MutableComponent hoverableMessage(String raw, List<String> highlightTerms) {
		String normalized = raw.replace('\n', ' ').replace('\r', ' ').trim();
		String display = normalized.length() > 140 ? normalized.substring(0, 137) + "..." : normalized;
		String id = MessageFlagging.registerMessage(normalized);
		MutableComponent hover = Component.literal("CTRL+Y = legit\nCTRL+N = scam").withStyle(ChatFormatting.YELLOW);

		MutableComponent content = highlightText(display, highlightTerms);
		return content.setStyle(
			Style.EMPTY
				.withColor(ChatFormatting.AQUA)
				.withUnderlined(true)
				.withHoverEvent(new HoverEvent.ShowText(hover))
				.withClickEvent(new ClickEvent.CopyToClipboard(MessageFlagging.clickValue(id)))
		);
	}

	private static MutableComponent highlightText(String text, List<String> terms) {
		if (terms == null || terms.isEmpty() || text == null || text.isBlank()) {
			return Component.literal(text == null ? "" : text);
		}

		String lower = text.toLowerCase(Locale.ROOT);
		int index = 0;
		MutableComponent out = Component.empty();

		while (index < text.length()) {
			Match next = findNextMatch(lower, terms, index);
			if (next == null) {
				out.append(Component.literal(text.substring(index)));
				break;
			}
			if (next.start() > index) {
				out.append(Component.literal(text.substring(index, next.start())));
			}
			out.append(Component.literal(text.substring(next.start(), next.end()))
				.withStyle(style -> style.withColor(ChatFormatting.GOLD).withBold(true)));
			index = next.end();
		}

		return out;
	}

	private static Match findNextMatch(String lowerText, List<String> terms, int fromIndex) {
		int bestStart = -1;
		int bestEnd = -1;
		for (String term : terms) {
			if (term == null || term.isBlank()) {
				continue;
			}
			String lowerTerm = term.toLowerCase(Locale.ROOT);
			int found = lowerText.indexOf(lowerTerm, fromIndex);
			if (found < 0) {
				continue;
			}
			if (bestStart == -1 || found < bestStart) {
				bestStart = found;
				bestEnd = found + lowerTerm.length();
			}
		}
		if (bestStart < 0) {
			return null;
		}
		return new Match(bestStart, bestEnd);
	}

	private static List<String> extractQuotedMatches(ScamRules.ScamAssessment assessment) {
		if (assessment == null || assessment.ruleDetails() == null || assessment.ruleDetails().isEmpty()) {
			return List.of();
		}
		List<String> matches = new ArrayList<>();
		for (String detail : assessment.ruleDetails().values()) {
			if (detail == null || detail.isBlank()) {
				continue;
			}
			String[] parts = detail.split("\"", -1);
			for (int i = 1; i < parts.length; i += 2) {
				String match = parts[i].trim();
				if (!match.isBlank()) {
					matches.add(match);
				}
			}
		}
		return matches;
	}

	private record Match(int start, int end) {
	}

	private static MutableComponent actionLine(String playerName, String messageId) {
		MutableComponent line = Component.literal("Actions: ").withStyle(ChatFormatting.GRAY);
		String target = playerName == null ? "" : playerName.trim();
		boolean canAct = !target.isBlank() && !"unknown".equalsIgnoreCase(target);
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
			"add player to blacklist",
			canAct ? "/scamscreener add " + target : null
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
}
