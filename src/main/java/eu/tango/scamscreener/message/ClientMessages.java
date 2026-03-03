package eu.tango.scamscreener.message;

import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.config.data.AlertRiskLevel;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;

/**
 * Shared v1-style prefixed chat lines for local command feedback.
 */
public final class ClientMessages {
    private static final String PREFIX = "[ScamScreener] ";

    private ClientMessages() {
    }

    public static MutableText uiUnavailable() {
        return error("ScamScreener UI is not available right now.");
    }

    public static MutableText alertContextMissing() {
        return error("Alert context expired. Wait for a fresh warning and click again.");
    }

    public static MutableText whitelistUpdateFailed() {
        return error("Whitelist update failed. Provide a valid player name or UUID.");
    }

    public static MutableText whitelistUpdated(String target) {
        return prefixed()
            .append(Text.literal("Whitelist updated: ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(target)).formatted(Formatting.AQUA))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText whitelistEntryMissing(String target) {
        return error("No whitelist entry found for " + displayValue(target) + ".");
    }

    public static MutableText whitelistRemoved(String target) {
        return prefixed()
            .append(Text.literal("Whitelist entry removed: ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(target)).formatted(Formatting.AQUA))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText whitelistCleared() {
        return prefixed().append(Text.literal("Whitelist cleared.").formatted(Formatting.GRAY));
    }

    public static MutableText blacklistUpdateFailed() {
        return error("Blacklist update failed. Provide a valid player name or UUID.");
    }

    public static MutableText blacklistUpdated(String target, int score) {
        return prefixed()
            .append(Text.literal("Blacklist updated: ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(target)).formatted(Formatting.AQUA))
            .append(Text.literal(" (score ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(Math.max(0, score))).formatted(Formatting.DARK_RED, Formatting.BOLD))
            .append(Text.literal(").").formatted(Formatting.GRAY));
    }

    public static MutableText blacklistEntryMissing(String target) {
        return error("No blacklist entry found for " + displayValue(target) + ".");
    }

    public static MutableText blacklistRemoved(String target) {
        return prefixed()
            .append(Text.literal("Blacklist entry removed: ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(target)).formatted(Formatting.AQUA))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText blacklistCleared() {
        return prefixed().append(Text.literal("Blacklist cleared.").formatted(Formatting.GRAY));
    }

    public static MutableText currentAlertLevel(AlertRiskLevel level) {
        return prefixed()
            .append(Text.literal("Current alert threshold: ").formatted(Formatting.GRAY))
            .append(Text.literal((level == null ? AlertRiskLevel.LOW : level).name()).formatted(Formatting.GOLD, Formatting.BOLD))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText updatedAlertLevel(AlertRiskLevel level) {
        return prefixed()
            .append(Text.literal("Alert threshold set to ").formatted(Formatting.GRAY))
            .append(Text.literal((level == null ? AlertRiskLevel.LOW : level).name()).formatted(Formatting.GOLD, Formatting.BOLD))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText invalidAlertLevel() {
        return error("Invalid level. Use LOW, MEDIUM, HIGH or CRITICAL.");
    }

    public static MutableText autoLeaveStatus(boolean enabled) {
        return prefixed()
            .append(Text.literal("Auto /p leave on blacklist: ").formatted(Formatting.GRAY))
            .append(Text.literal(enabled ? "ON" : "OFF").formatted(enabled ? Formatting.GREEN : Formatting.RED, Formatting.BOLD))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText autoLeaveEnabled() {
        return prefixed().append(Text.literal("Auto /p leave on blacklist enabled.").formatted(Formatting.GRAY));
    }

    public static MutableText autoLeaveDisabled() {
        return prefixed().append(Text.literal("Auto /p leave on blacklist disabled.").formatted(Formatting.GRAY));
    }

    public static MutableText autoLeaveExecuted(String playerName) {
        return prefixed()
            .append(Text.literal("Auto /p leave executed after blacklist warning from ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(playerName)).formatted(Formatting.AQUA))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText muteEnabled() {
        return prefixed().append(Text.literal("Mute filter enabled.").formatted(Formatting.GRAY));
    }

    public static MutableText muteDisabled() {
        return prefixed().append(Text.literal("Mute filter disabled.").formatted(Formatting.GRAY));
    }

    public static MutableText mutePatternAdded(String pattern) {
        return prefixed()
            .append(Text.literal("Mute pattern added: ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(pattern)).formatted(Formatting.YELLOW))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText mutePatternAlreadyExists(String pattern) {
        return error("Mute pattern already exists: " + displayValue(pattern) + ".");
    }

    public static MutableText mutePatternInvalid(String pattern) {
        return error("Invalid mute regex: " + displayValue(pattern) + ".");
    }

    public static MutableText mutePatternRemoved(String pattern) {
        return prefixed()
            .append(Text.literal("Mute pattern removed: ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(pattern)).formatted(Formatting.YELLOW))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText mutePatternNotFound(String pattern) {
        return error("No mute pattern found for " + displayValue(pattern) + ".");
    }

    public static MutableText debugStatus(Map<String, Boolean> states) {
        StringBuilder summary = new StringBuilder();
        if (states != null) {
            for (Map.Entry<String, Boolean> entry : states.entrySet()) {
                if (summary.length() > 0) {
                    summary.append(", ");
                }
                summary.append(entry.getKey()).append('=').append(Boolean.TRUE.equals(entry.getValue()) ? "on" : "off");
            }
        }

        if (summary.length() == 0) {
            summary.append("none");
        }

        return prefixed()
            .append(Text.literal("Debug flags: ").formatted(Formatting.GRAY))
            .append(Text.literal(summary.toString()).formatted(Formatting.YELLOW))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText debugUpdated(String message) {
        return prefixed()
            .append(Text.literal("Debug updated: ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(message)).formatted(Formatting.YELLOW))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText debugKeyUnknown(String key) {
        return error("Unknown debug key: " + displayValue(key) + ".");
    }

    public static MutableText versionInfo() {
        return prefixed()
            .append(Text.literal("Version ").formatted(Formatting.GRAY))
            .append(Text.literal(ScamScreenerMod.VERSION).formatted(Formatting.AQUA, Formatting.BOLD))
            .append(Text.literal(" on Minecraft ").formatted(Formatting.GRAY))
            .append(Text.literal(ScamScreenerMod.MINECRAFT).formatted(Formatting.AQUA))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText previewStarted() {
        return prefixed().append(Text.literal("Preview dry run started.").formatted(Formatting.GRAY));
    }

    public static MutableText previewFinished() {
        return prefixed().append(Text.literal("Preview dry run finished.").formatted(Formatting.GRAY));
    }

    public static MutableText commandHelp() {
        return prefixed().append(Text.literal(
            "Commands: add/remove/list, whitelist, blacklist, review, alertlevel, autoleave, mute, unmute, debug, settings."
        ).formatted(Formatting.GRAY));
    }

    public static MutableText reviewSelectionRequired() {
        return error("No reviewed messages selected. Mark at least one line as scam or legit.");
    }

    public static MutableText reviewMessagesSaved(int scamCount, int legitCount) {
        return prefixed()
            .append(Text.literal("Saved reviewed messages. scam=").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(Math.max(0, scamCount))).formatted(Formatting.DARK_RED, Formatting.BOLD))
            .append(Text.literal(", legit=").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(Math.max(0, legitCount))).formatted(Formatting.GREEN, Formatting.BOLD))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    private static MutableText error(String message) {
        return prefixed().append(Text.literal(message == null ? "" : message).formatted(Formatting.RED));
    }

    private static MutableText prefixed() {
        return Text.literal(PREFIX).formatted(Formatting.DARK_RED);
    }

    private static String displayValue(String value) {
        if (value == null || value.isBlank()) {
            return "<unknown>";
        }

        return value.trim();
    }
}
