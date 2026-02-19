package eu.tango.scamscreener.ui;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import eu.tango.scamscreener.ai.FunnelMetricsService;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.pipeline.core.DetectionScoring;
import eu.tango.scamscreener.pipeline.model.DetectionResult;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.whitelist.WhitelistManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class Messages extends MessageBuilder {
	private static final String PREFIX = DEFAULT_PREFIX;
	private static final int PREFIX_LIGHT_RED = DEFAULT_PREFIX_COLOR;
	private static final String FANDOM_WEBSITE_URL_SCAM = "https://hypixel-skyblock.fandom.com/wiki/Scams";

	private Messages() {
	}

	public static MutableComponent addedToBlacklist(String name, UUID uuid) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was added to the blacklist.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent addedToBlacklistWithScore(String name, UUID uuid, int score) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was added with score ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(score)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent addedToBlacklistWithMetadata(String name, UUID uuid) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was added with metadata.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent updatedBlacklistEntry(String name, int score, String reason) {
		String safeName = name == null ? "unknown" : name;
		String safeReason = reason == null ? "n/a" : reason;
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Updated blacklist entry: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(safeName).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(String.valueOf(Math.max(0, score))).withStyle(ChatFormatting.DARK_RED))
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(safeReason).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent alreadyBlacklisted(String name, UUID uuid) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" is already blacklisted.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent removedFromBlacklist(String name, UUID uuid) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was removed from the blacklist.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent notOnBlacklist(String name, UUID uuid) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" is not on the blacklist.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent blacklistEmpty() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("The blacklist is empty.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent blacklistHeader() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
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

	public static MutableComponent addedToWhitelist(String name, UUID uuid) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was added to the whitelist.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent updatedWhitelistEntry(String name, UUID uuid) {
		String safeName = name == null || name.isBlank() ? "unknown" : name;
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Updated whitelist entry: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(safeName).withStyle(ChatFormatting.AQUA));
	}

	public static MutableComponent alreadyWhitelisted(String name, UUID uuid) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" is already whitelisted.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent removedFromWhitelist(String name, UUID uuid) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal(name).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" was removed from the whitelist.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent notOnWhitelist(String name) {
		String safeName = name == null || name.isBlank() ? "unknown" : name;
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal(safeName).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" is not on the whitelist.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent whitelistEmpty() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("The whitelist is empty.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent whitelistHeader() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Whitelist entries:").withStyle(ChatFormatting.GRAY));
	}

	public static Component whitelistEntry(WhitelistManager.WhitelistEntry entry) {
		String name = entry == null || entry.name() == null || entry.name().isBlank() ? "unknown" : entry.name();
		String timestamp = entry == null ? null : entry.addedAt();
		return Component.literal(name).withStyle(ChatFormatting.AQUA)
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(formatTimestamp(timestamp)).withStyle(ChatFormatting.GREEN));
	}

	public static MutableComponent whitelistCommandHelp() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Whitelist commands:").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener whitelist").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener whitelist add <player>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener whitelist remove <player>").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent unresolvedTarget(String input) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Could not resolve '").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(input).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("'. Tried UUID, online player list, local blacklist, and Mojang lookup.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent noChatToCapture() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("No chat line captured yet. Wait for a message, then try again.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingSampleSaved(String path, int label) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Saved training sample with label ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(label)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal(" to ").withStyle(ChatFormatting.GRAY))
			.append(clickableFilePath(path))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingSampleFlagged(String labelText) {
		String safeLabel = labelText == null ? "unknown" : labelText;
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Saved as ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(safeLabel).withStyle(ChatFormatting.GOLD))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingSamplesSaved(String path, int label, int count) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Saved ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(count)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(" training samples with label ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(label)).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal(" to ").withStyle(ChatFormatting.GRAY))
			.append(clickableFilePath(path))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingSaveFailed(String errorMessage) {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Failed to save training sample.",
			"TR-SAVE-001",
			errorMessage
		);
	}

	public static MutableComponent trainingSamplesSaveFailed(String errorMessage) {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Failed to save training samples.",
			"TR-SAVE-002",
			errorMessage
		);
	}

	public static MutableComponent trainingDataMigrated(int updatedRows) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Training data migrated. Updated ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(Math.max(0, updatedRows))).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(" rows.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingDataUpToDate() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Training data already up to date.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent modelUpdateAvailable(MutableComponent link) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(link == null ? Component.literal("Model update available.").withStyle(ChatFormatting.GRAY) : link);
	}

	public static MutableComponent modelUpdateUpToDate() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("AI model is already up to date.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent modelUpdateCheckFailed(String errorMessage) {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Model update check failed.",
			"MU-CHECK-001",
			errorMessage
		);
	}

	public static MutableComponent modelUpdateDownloadLink(String command) {
		Style style = Style.EMPTY
			.withColor(ChatFormatting.YELLOW)
			.withClickEvent(new ClickEvent.RunCommand(command == null ? "" : command))
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Download model update")));
		return Component.literal("A new AI Model is available. Click to update your local model.")
			.setStyle(style);
	}

	public static MutableComponent modelUpdateReady(MutableComponent actions) {
		MutableComponent line = prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Model update downloaded. ").withStyle(ChatFormatting.GRAY));
		if (actions != null) {
			line.append(actions);
		}
		return line;
	}

	public static MutableComponent modelUpdateApplied(String action) {
		String safe = action == null ? "applied" : action;
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Model update ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(safe).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent modelUpdateFailed(String message) {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Model update failed.",
			"MU-UPDATE-001",
			message
		);
	}

	public static MutableComponent modelUpdateNotReady() {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Model update not downloaded yet.",
			"MU-DOWNLOAD-001",
			"missing downloaded payload"
		);
	}

	public static MutableComponent modelUpdateNotFound() {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Model update not found.",
			"MU-LOOKUP-001",
			"unknown update id"
		);
	}

	public static MutableComponent modelUpdateIgnored() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Model update ignored.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent aiUpdateJoinNotifyStatus(boolean enabled) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("AI up-to-date message on server join: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(enabled ? "ON" : "OFF").withStyle(enabled ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
	}

	public static MutableComponent aiUpdateJoinNotifyEnabled() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("AI up-to-date message on server join enabled.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent aiUpdateJoinNotifyDisabled() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("AI up-to-date message on server join disabled.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingUploadWebhookStarted(String path) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Uploading...").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingUploadWebhookSucceeded(String path, String verificationDetail) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Training data uploaded sucessfully.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingUploadWebhookFailed(String errorMessage) {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Discord webhook upload failed.",
			"TR-UPLOAD-001",
			errorMessage
		);
	}

	public static MutableComponent trainingUploadUnavailable(String detail) {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Training data upload unavailable.",
			"TR-UPLOAD-002",
			detail
		);
	}

	public static MutableComponent trainingFailed(String errorMessage) {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Training failed.",
			"TR-TRAIN-001",
			errorMessage
		);
	}

	public static MutableComponent mojangLookupStarted(String input) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Resolving '").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(input == null ? "" : input).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("' via Mojang API... try again in a moment.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent mojangLookupCompleted(String input, String resolvedName) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Mojang lookup for '").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(input == null ? "" : input).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("' completed. Run command again to use ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(resolvedName == null ? "unknown" : resolvedName).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent commandHelp() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Commands:"))
			.append(Component.literal("\n- /scamscreener add <player> [score] [reason]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener remove <player>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener list").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener whitelist").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener whitelist add <player>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener whitelist remove <player>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener mute [pattern]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener unmute <pattern>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener autoleave [on|off]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener upload").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai reset").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai metrics [reset]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai autocapture [off|low|medium|high|critical]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener rules <list|disable|enable> [rule]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener alertlevel [low|medium|high|critical]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener review <manage|info> <alertId>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener review player <playerName>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener edu disable <messageId>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener settings").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener version").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener preview").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent mutePatternList(List<String> patterns) {
		String joined = (patterns == null || patterns.isEmpty())
			? "none"
			: String.join(", ", patterns);
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Muted patterns: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(joined).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent muteEnabled() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Mute filter enabled.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent muteDisabled() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Mute filter disabled.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent autoLeaveStatus(boolean enabled) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Auto party leave on blacklist: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(enabled ? "ON" : "OFF").withStyle(enabled ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
	}

	public static MutableComponent autoLeaveEnabled() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Auto party leave enabled.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent autoLeaveDisabled() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Auto party leave disabled.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent autoLeaveExecuted(String playerName) {
		String safeName = playerName == null || playerName.isBlank() ? "unknown" : playerName;
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Blacklisted player detected (").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(safeName).withStyle(ChatFormatting.AQUA))
			.append(Component.literal("). Executed /p leave.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent emailSafetyBlocked(String bypassId) {
		String id = bypassId == null ? "" : bypassId;
		MutableComponent line = Component.literal(PREFIX + "Email address detected. This could be a scam. ")
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.withStyle(ChatFormatting.GRAY);
		Style bypassStyle = Style.EMPTY
			.withColor(ChatFormatting.DARK_RED)
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Send anyway").withStyle(ChatFormatting.YELLOW)))
			.withClickEvent(new ClickEvent.RunCommand("/scamscreener bypass " + id));
		line.append(Component.literal("[BYPASS]").setStyle(bypassStyle));
		return line;
	}

	public static MutableComponent discordSafetyBlocked(String bypassId) {
		String id = bypassId == null ? "" : bypassId;
		MutableComponent line = Component.literal(PREFIX + "Discord link detected. This could be a scam. ")
			.withStyle(style -> style.withColor(PREFIX_LIGHT_RED))
			.withStyle(ChatFormatting.GRAY);
		Style bypassStyle = Style.EMPTY
			.withColor(ChatFormatting.DARK_RED)
			.withHoverEvent(new HoverEvent.ShowText(Component.literal("Send anyway").withStyle(ChatFormatting.YELLOW)))
			.withClickEvent(new ClickEvent.RunCommand("/scamscreener bypass " + id));
		line.append(Component.literal("[BYPASS]").setStyle(bypassStyle));
		return line;
	}

	public static MutableComponent coopAddSafetyBlocked(String playerName, String bypassId, boolean blacklisted) {
		String id = bypassId == null ? "" : bypassId;
		String safePlayer = playerName == null || playerName.isBlank() ? "unknown" : playerName;
		MutableComponent line = prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Player ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(safePlayer).withStyle(ChatFormatting.AQUA));

		if (blacklisted) {
			line.append(Component.literal(" is ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("blacklisted").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
				.append(Component.literal(". /coopadd blocked. Double-check. ").withStyle(ChatFormatting.GRAY));
		} else {
			line.append(Component.literal(" was detected in /coopadd. Confirm before sending. ").withStyle(ChatFormatting.GRAY));
		}

		line.append(actionTag("BYPASS", ChatFormatting.DARK_RED, "Invite anyway", "/scamscreener bypass " + id));
		return line;
	}

	public static MutableComponent emailBypassSent() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Bypass sent.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent emailBypassExpired() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Bypass expired. Please resend the message.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent mutePatternAdded(String pattern) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Muted pattern added: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(pattern == null ? "" : pattern).withStyle(ChatFormatting.GOLD));
	}

	public static MutableComponent mutePatternAlreadyExists(String pattern) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Pattern already muted: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(pattern == null ? "" : pattern).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent mutePatternInvalid(String pattern) {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Invalid regex pattern.",
			"MUTE-REGEX-001",
			pattern
		);
	}

	public static MutableComponent mutePatternRemoved(String pattern) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Unmuted pattern: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(pattern == null ? "" : pattern).withStyle(ChatFormatting.GOLD));
	}

	public static MutableComponent mutePatternNotFound(String pattern) {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Pattern not found.",
			"MUTE-LOOKUP-001",
			pattern
		);
	}

	public static MutableComponent blockedMessagesSummary(int blockedCount, int intervalSeconds) {
		int safeInterval = Math.max(1, intervalSeconds);
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Muted ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(Math.max(0, blockedCount))).withStyle(ChatFormatting.GRAY))
			.append(Component.literal(" messages in the last ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(safeInterval)).withStyle(ChatFormatting.GRAY))
			.append(Component.literal("s.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingDataLargeUploadReminder(int entryCount) {
		MutableComponent line = prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Training data has ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(Math.max(0, entryCount))).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(" entries. Please upload it to keep the local dataset small. ").withStyle(ChatFormatting.GRAY));
		line.append(actionTag("UPLOAD NOW", ChatFormatting.GOLD, "Run /scamscreener upload", "/scamscreener upload"));
		return line;
	}

	public static MutableComponent versionInfo(String modVersion, int aiVersion) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Mod Version: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(modVersion == null ? "unknown" : modVersion).withStyle(ChatFormatting.GOLD))
			.append(Component.literal(" | AI Model Version: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(aiVersion)).withStyle(ChatFormatting.AQUA));
	}

	public static MutableComponent aiCommandHelp() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("AI Commands:").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai flag <messageId> <legit|scam>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai migrate").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai update notify [on|off]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai model <download|accept|merge|ignore> <id>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener upload").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai reset").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai metrics [reset]").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener ai autocapture [off|low|medium|high|critical]").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent reviewCommandHelp() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Review command usage:").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener review manage <alertId>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener review info <alertId>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener review player <playerName>").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent educationCommandHelp() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Education command usage:").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener edu disable <messageId>").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent alertReviewContextMissing() {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Alert context was not found.",
			"ALERT-LOOKUP-001",
			"The alert may be expired. Wait for a new warning and click manage/info again."
		);
	}

	public static MutableComponent alertReviewOpenFailed() {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Failed to open alert review screen.",
			"ALERT-SCREEN-001",
			"Minecraft client screen context is unavailable."
		);
	}

	public static MutableComponent reviewSelectionRequired() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("No reviewed messages selected. Mark at least one line as scam or legit.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent reviewMessagesSaved(int scamCount, int legitCount) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Saved reviewed messages. scam=").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(Math.max(0, scamCount))).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
			.append(Component.literal(", legit=").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(Math.max(0, legitCount))).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingCsvReviewNoData(String path) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("No training csv rows available for review at ").withStyle(ChatFormatting.GRAY))
			.append(clickableContainingFolderPath(path == null ? "" : path))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingCsvReviewUpdated(int changedRows) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Applied ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(Math.max(0, changedRows))).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(" training csv changes.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent trainingCsvReviewFailed(String detail) {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Training csv review failed.",
			"TR-CSV-001",
			detail
		);
	}

	private static MutableComponent educationWarning(String disableCommand, String... guidanceParts) {
		MutableComponent line = prefixedMessage(PREFIX, PREFIX_LIGHT_RED);
		if (guidanceParts != null) {
			for (String part : guidanceParts) {
				if (part == null || part.isBlank()) {
					continue;
				}
				line.append(Component.literal(part).withStyle(ChatFormatting.GRAY));
			}
		}
		line.append(Component.literal("More info and help can be found ").withStyle(ChatFormatting.GRAY))
			.append(clickableUrl("here", FANDOM_WEBSITE_URL_SCAM, "Open Fandom Website to learn more about Scams"))
			.append(Component.literal(". ").withStyle(ChatFormatting.GRAY));
		line.append(actionTag("disable info message", ChatFormatting.DARK_GRAY, "Disable this message.", disableCommand));
		return line;
	}

	public static MutableComponent educationExternalPlatformWarning(String disableCommand) {
		return educationWarning(
			disableCommand,
			"The user is trying to move you over to an external platform. ",
			"Scammers often do this, so proceed with caution. ",
			"If you're unsure whether it's a scam, treat it as one until proven otherwise. "
		);
	}

	public static MutableComponent educationSuspiciousLinkWarning(String disableCommand) {
		return educationWarning(
			disableCommand,
			"The message contains a suspicious link. ",
			"Never log into websites opened directly from chat links. ",
			"Open trusted sites manually and double-check the exact domain first. "
		);
	}

	public static MutableComponent educationUpfrontPaymentWarning(String disableCommand) {
		return educationWarning(
			disableCommand,
			"The user asks for payment before proof or delivery. ",
			"This is a common scam setup in trading chats. ",
			"Only trade with verified middlemen and never pay first without strong proof. "
		);
	}

	public static MutableComponent educationAccountDataWarning(String disableCommand) {
		return educationWarning(
			disableCommand,
			"The user asks for account or personal login data. ",
			"Never share your Microsoft login, email codes, or recovery information. ",
			"Legitimate players and staff do not need your credentials. "
		);
	}

	public static MutableComponent educationFakeMiddlemanWarning(String disableCommand) {
		return educationWarning(
			disableCommand,
			"The user claims a trusted middleman without reliable proof. ",
			"Scammers often fake middleman identities with screenshots or name lookalikes. ",
			"Verify middlemen only through official server channels before trading. "
		);
	}

	public static MutableComponent educationUrgencyWarning(String disableCommand) {
		return educationWarning(
			disableCommand,
			"The user is creating pressure and urgency. ",
			"Scammers rush decisions to prevent verification. ",
			"Slow down, verify details, and walk away if they keep pushing. "
		);
	}

	public static MutableComponent educationTrustManipulationWarning(String disableCommand) {
		return educationWarning(
			disableCommand,
			"The user is trying to force trust quickly. ",
			"Claims like 'trusted', 'friend of admin', or 'many vouches' can be faked. ",
			"Always verify reputation independently before sending anything. "
		);
	}

	public static MutableComponent educationTooGoodToBeTrueWarning(String disableCommand) {
		return educationWarning(
			disableCommand,
			"The offer looks too good to be true. ",
			"Unreal discounts, huge profit promises, or free rare items are common bait. ",
			"If the deal makes no sense economically, treat it as high risk. "
		);
	}

	public static MutableComponent educationDiscordHandleWarning(String disableCommand) {
		return educationWarning(
			disableCommand,
			"The chat contains a Discord handle in suspicious context. ",
			"Scammers often move victims to DMs where logs and moderation are weaker. ",
			"Verify identity via official communities before continuing outside Minecraft. "
		);
	}

	public static MutableComponent educationFunnelSequenceWarning(String disableCommand) {
		return educationWarning(
			disableCommand,
			"The conversation matches a staged scam funnel pattern. ",
			"These chats usually start harmless, then build trust, then ask for risky actions. ",
			"Stop at the first request for payment, account access, or off-platform contact. "
		);
	}

	public static MutableComponent educationMessageDisabled(String messageId) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Education message disabled: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(messageId == null ? "unknown" : messageId).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent educationMessageUnknown(String messageId) {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Unknown education message id.",
			"EDU-ID-001",
			messageId
		);
	}

	public static MutableComponent addCommandHelp() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Usage: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("/scamscreener add <player> [score] [reason]").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent removeCommandHelp() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Usage: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("/scamscreener remove <player>").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent aiCaptureCommandHelp() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Usage: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("/scamscreener ai flag <messageId> <legit|scam>").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent ruleCommandHelp() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Rule command usage:").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener rules list").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener rules disable <rule>").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- /scamscreener rules enable <rule>").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent disabledRulesList(Set<ScamRules.ScamRule> disabled) {
		String list = (disabled == null || disabled.isEmpty())
			? "none"
			: disabled.stream().map(Enum::name).sorted().collect(Collectors.joining(", "));
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Disabled rules: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(list).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent invalidRuleName() {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Invalid rule name.",
			"RULE-NAME-001",
			"Use autocomplete or /scamscreener rules list."
		);
	}

	public static MutableComponent ruleDisabled(ScamRules.ScamRule rule) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Rule disabled: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(rule.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent ruleEnabled(ScamRules.ScamRule rule) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Rule enabled: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(rule.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent ruleAlreadyDisabled(ScamRules.ScamRule rule) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Rule is already disabled: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(rule.name()).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent ruleNotDisabled(ScamRules.ScamRule rule) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Rule is not disabled: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(rule.name()).withStyle(ChatFormatting.YELLOW));
	}

	public static MutableComponent localAiModelReset() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Local AI model was reset to default weights.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent funnelMetricsSummary(FunnelMetricsService.Snapshot snapshot) {
		FunnelMetricsService.Snapshot safe = snapshot == null
			? new FunnelMetricsService.Snapshot(0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 5.0)
			: snapshot;
		String thresholdText = safe.alertThreshold() <= 0.0
			? "n/a"
			: String.format(Locale.ROOT, "%.0f", safe.alertThreshold());
		String marginText = String.format(Locale.ROOT, "%.0f", Math.max(0.0, safe.uncertainMargin()));

		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Funnel metrics (local):").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- evaluated messages: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(safe.evaluatedMessages())).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("\n- funnel detections: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(safe.funnelDetections())).withStyle(ChatFormatting.GOLD))
			.append(Component.literal(" (" + safe.detectionRatePercent() + ")").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- user-marked funnel cases: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(safe.userMarkedSamples())).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("\n- false positive rate (user-marked legit): ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(safe.falsePositivePercent()).withStyle(ChatFormatting.GOLD))
			.append(Component.literal(" (" + safe.userMarkedLegit() + "/" + safe.userMarkedSamples() + ")").withStyle(ChatFormatting.GRAY))
			.append(Component.literal("\n- uncertain threshold boundary cases: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(String.valueOf(safe.uncertainBoundaryCases())).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal(" (+/-" + marginText + " around " + thresholdText + ")").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent funnelMetricsReset() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Funnel metrics reset.").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent funnelMetricsCopiedToClipboard() {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Metrics copied to clipboard (Discord format).").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent funnelMetricsClipboardUnavailable() {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Failed to copy metrics.",
			"METRIC-CLIP-001",
			"Clipboard handler is unavailable."
		);
	}

	public static MutableComponent currentAutoCaptureAlertLevel(String level) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Auto-capture on alerts: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(level == null ? "HIGH" : level).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent updatedAutoCaptureAlertLevel(String level) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Auto-capture level updated to ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(level == null ? "HIGH" : level).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
			.append(Component.literal(".").withStyle(ChatFormatting.GRAY));
	}

	public static MutableComponent invalidAutoCaptureAlertLevel() {
		return buildError(
			PREFIX,
			PREFIX_LIGHT_RED,
			"Invalid auto-capture level.",
			"AI-CAPTURE-001",
			"Use: off, low, medium, high, critical."
		);
	}

	public static MutableComponent currentAlertRiskLevel(ScamRules.ScamRiskLevel level) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Current alert threshold: ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(level == null ? "MEDIUM" : level.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	public static MutableComponent updatedAlertRiskLevel(ScamRules.ScamRiskLevel level) {
		return prefixedMessage(PREFIX, PREFIX_LIGHT_RED)
			.append(Component.literal("Alert threshold updated to ").withStyle(ChatFormatting.GRAY))
			.append(Component.literal(level == null ? "MEDIUM" : level.name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
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
		Map<ScamRules.ScamRule, Double> ruleWeights = new LinkedHashMap<>();
		if (result.signals() != null) {
			result.signals().forEach(signal -> {
				if (signal == null || signal.ruleId() == null) {
					return;
				}
				ruleWeights.merge(signal.ruleId(), signal.weight(), Math::max);
			});
		}
		return behaviorRiskWarning(playerName, assessment, ruleWeights.isEmpty() ? null : ruleWeights);
	}

	public static MutableComponent behaviorRiskWarning(String playerName, ScamRules.ScamAssessment assessment) {
		return behaviorRiskWarning(playerName, assessment, null);
	}

	public static MutableComponent behaviorRiskWarning(String playerName, ScamRules.ScamAssessment assessment, Map<ScamRules.ScamRule, Double> ruleWeights) {
		ScamRules.ScamAssessment safeAssessment = assessment == null
			? new ScamRules.ScamAssessment(0, ScamRules.ScamRiskLevel.LOW, Set.of(), Map.of(), null, List.of())
			: assessment;
		String levelText = safeAssessment.riskLevel().name() + " RISK MESSAGE";
		String playerText = playerName == null ? "unknown" : playerName;
		String scoreText = String.valueOf(safeAssessment.riskScore());
		String playerScoreLine = playerText + " | " + scoreText;
		int scoreColor = scoreGradientColor(safeAssessment.riskScore());
		String alertContextId = AlertReviewRegistry.register(playerText, safeAssessment, ruleWeights);
		MutableComponent message = Component.empty()
			.append(Component.literal(WARNING_BORDER).withStyle(ChatFormatting.DARK_RED))
			.append(Component.literal("\n" + centeredBold(levelText)).withStyle(style -> style.withColor(levelColor(safeAssessment.riskLevel())).withBold(true)))
			.append(Component.literal("\n" + leftCenterPadding(playerScoreLine)))
			.append(Component.literal(playerText).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(scoreText).withStyle(style -> style.withColor(scoreColor)))
			.append(Component.literal("\n"))
			.append(actionLine(alertContextId));
		return message.append(Component.literal("\n" + WARNING_BORDER).withStyle(ChatFormatting.DARK_RED));
	}

	public static MutableComponent blacklistWarning(String playerName, String triggerReason, BlacklistManager.ScamEntry entry) {
		String safePlayer = playerName == null ? "n/a" : playerName;
		String scoreText = entry == null ? "n/a" : String.valueOf(entry.score());
		String reason = entry == null ? "n/a" : entry.reason();
		String addedAt = entry == null ? "n/a" : formatTimestamp(entry.addedAt());
		int scoreValue = entry == null ? 0 : entry.score();
		int scoreColor = scoreGradientColor(scoreValue);
		String playerScoreLine = safePlayer + " | " + scoreText;

		MutableComponent message = Component.empty()
			.append(Component.literal(WARNING_BORDER).withStyle(ChatFormatting.DARK_RED))
			.append(Component.literal("\n" + centeredBold("BLACKLIST WARNING")).withStyle(style -> style.withColor(ChatFormatting.DARK_RED).withBold(true)))
			.append(Component.literal("\n" + leftCenterPadding(playerScoreLine)))
			.append(Component.literal(safePlayer).withStyle(ChatFormatting.AQUA))
			.append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.literal(scoreText).withStyle(style -> style.withColor(scoreColor)))
			.append(Component.literal("\n" + centered(reason)).withStyle(ChatFormatting.YELLOW))
			.append(Component.literal("\n" + centered(addedAt)).withStyle(ChatFormatting.GREEN))
			.append(Component.literal("\n" + centered(triggerReason == null ? "n/a" : triggerReason)).withStyle(ChatFormatting.GOLD));

		return message.append(Component.literal("\n" + WARNING_BORDER).withStyle(ChatFormatting.DARK_RED));
	}
}

