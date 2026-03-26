package eu.tango.scamscreener.message;

import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.config.data.AlertRiskLevel;
import eu.tango.scamscreener.review.ReviewVerdict;
import eu.tango.scamscreener.training.TrainingCaseExportService;
import java.net.URI;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Shared v1-style prefixed chat lines for local command feedback.
 */
public final class ClientMessages {
    private static final String PREFIX = "[ScamScreener] ";

    private ClientMessages() {
    }

    public static MutableComponent uiUnavailable() {
        return error("ScamScreener UI is not available right now.");
    }

    public static MutableComponent alertContextMissing() {
        return error("Alert context expired. Wait for a fresh warning and click again.");
    }

    public static MutableComponent whitelistUpdateFailed() {
        return error("Whitelist update failed. Provide a valid player name or UUID.");
    }

    public static MutableComponent whitelistUpdated(String target) {
        return prefixed()
            .append(Component.literal("Whitelist updated: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(displayValue(target)).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent whitelistEntryMissing(String target) {
        return error("No whitelist entry found for " + displayValue(target) + ".");
    }

    public static MutableComponent whitelistRemoved(String target) {
        return prefixed()
            .append(Component.literal("Whitelist entry removed: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(displayValue(target)).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent whitelistCleared() {
        return prefixed().append(Component.literal("Whitelist cleared.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent blacklistUpdateFailed() {
        return error("Blacklist update failed. Provide a valid player name or UUID.");
    }

    public static MutableComponent blacklistUpdated(String target, int score) {
        return prefixed()
            .append(Component.literal("Blacklist updated: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(displayValue(target)).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" (score ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(Math.max(0, score))).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
            .append(Component.literal(").").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent blacklistEntryMissing(String target) {
        return error("No blacklist entry found for " + displayValue(target) + ".");
    }

    public static MutableComponent blacklistRemoved(String target) {
        return prefixed()
            .append(Component.literal("Blacklist entry removed: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(displayValue(target)).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent blacklistCleared() {
        return prefixed().append(Component.literal("Blacklist cleared.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent currentAlertLevel(AlertRiskLevel level) {
        return prefixed()
            .append(Component.literal("Current alert threshold: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal((level == null ? AlertRiskLevel.MEDIUM : level).name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent updatedAlertLevel(AlertRiskLevel level) {
        return prefixed()
            .append(Component.literal("Alert threshold set to ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal((level == null ? AlertRiskLevel.MEDIUM : level).name()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent invalidAlertLevel() {
        return error("Invalid level. Use LOW, MEDIUM, HIGH or CRITICAL.");
    }

    public static MutableComponent autoLeaveStatus(boolean enabled) {
        return prefixed()
            .append(Component.literal("Auto /p leave on blacklist: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(enabled ? "ON" : "OFF").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent autoLeaveEnabled() {
        return prefixed().append(Component.literal("Auto /p leave on blacklist enabled.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent autoLeaveDisabled() {
        return prefixed().append(Component.literal("Auto /p leave on blacklist disabled.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent scamScreenerEnabled() {
        return prefixed().append(Component.literal("ScamScreener enabled.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent scamScreenerDisabled() {
        return prefixed().append(Component.literal("ScamScreener disabled.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent scamScreenerDisabledJoinNotice() {
        return prefixed()
            .append(Component.literal("ScamScreener is disabled. ").withStyle(ChatFormatting.GRAY))
            .append(actionTag("Click", ChatFormatting.GREEN, "Enable ScamScreener.", "/ss enable"))
            .append(Component.literal(" to enable it again.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent updateAvailable(String currentVersion, String latestVersion, String modrinthUrl, String changelog) {
        return prefixed()
            .append(Component.literal("Update Available ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(displayVersionOnly(currentVersion)).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" -> ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(displayVersionOnly(latestVersion)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
            .append(Component.literal(". ").withStyle(ChatFormatting.GRAY))
            .append(urlActionTag("click", ChatFormatting.YELLOW, changelogHoverText(changelog), modrinthUrl))
            .append(Component.literal(" to open on Modrinth").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent autoLeaveExecuted(String playerName) {
        return prefixed()
            .append(Component.literal("Auto /p leave executed after blacklist warning from ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(displayValue(playerName)).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent muteEnabled() {
        return prefixed().append(Component.literal("Mute filter enabled.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent muteDisabled() {
        return prefixed().append(Component.literal("Mute filter disabled.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent mutePatternAdded(String pattern) {
        return prefixed()
            .append(Component.literal("Mute pattern added: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(displayValue(pattern)).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent mutePatternAlreadyExists(String pattern) {
        return error("Mute pattern already exists: " + displayValue(pattern) + ".");
    }

    public static MutableComponent mutePatternInvalid(String pattern) {
        return error("Invalid mute regex: " + displayValue(pattern) + ".");
    }

    public static MutableComponent mutePatternRemoved(String pattern) {
        return prefixed()
            .append(Component.literal("Mute pattern removed: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(displayValue(pattern)).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent mutePatternNotFound(String pattern) {
        return error("No mute pattern found for " + displayValue(pattern) + ".");
    }

    public static MutableComponent profilerStatus(boolean enabled) {
        return prefixed()
            .append(Component.literal("Profiler HUD: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(enabled ? "ON" : "OFF").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent profilerEnabled() {
        return prefixed().append(Component.literal("Profiler HUD enabled.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent profilerDisabled() {
        return prefixed().append(Component.literal("Profiler HUD disabled.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent profilerWebOpened(String url) {
        return prefixed()
            .append(Component.literal("Profiler web view opened. ").withStyle(ChatFormatting.GRAY))
            .append(urlActionTag(
                "click",
                ChatFormatting.YELLOW,
                Component.literal(displayValue(url)).withStyle(ChatFormatting.GRAY),
                url
            ))
            .append(Component.literal(" to open it again manually.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent profilerWebMissingDependency(String url) {
        return prefixed()
            .append(Component.literal("Tango Web API is missing. ").withStyle(ChatFormatting.RED))
            .append(urlActionTag(
                "click",
                ChatFormatting.YELLOW,
                Component.literal("Download Tango Web API from Modrinth.").withStyle(ChatFormatting.GRAY),
                url
            ))
            .append(Component.literal(" to download it from Modrinth.").withStyle(ChatFormatting.RED));
    }

    public static MutableComponent profilerWebUnavailable(String message) {
        return error("Web profiler unavailable: " + displayValue(message) + ".");
    }

    public static MutableComponent profilerWebOpenFailed(String message) {
        return error("Could not open the profiler web view: " + displayValue(message) + ".");
    }

    public static MutableComponent debugStatus(Map<String, Boolean> states) {
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
            .append(Component.literal("Debug flags: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(summary.toString()).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent debugUpdated(String message) {
        return prefixed()
            .append(Component.literal("Debug updated: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(displayValue(message)).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent debugKeyUnknown(String key) {
        return error("Unknown debug key: " + displayValue(key) + ".");
    }

    public static MutableComponent versionInfo() {
        return prefixed()
            .append(Component.literal("Version ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(ScamScreenerMod.VERSION).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal(" on Minecraft ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(ScamScreenerMod.MINECRAFT).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent commandHelp() {
        return prefixed().append(Component.literal(
            "Commands: enable, disable, whitelist, blacklist, review, review export, alertlevel, autoleave, mute, unmute, debug, metrics, profiler, rules, runtime, messages, settings."
        ).withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent trainingCasesExported(TrainingCaseExportService.TrainingCaseExportResult result) {
        int caseCount = result == null ? 0 : Math.max(0, result.exportedCaseCount());
        String exportPath = result == null || result.trainingCasesFile() == null
            ? "<unknown>"
            : result.trainingCasesFile().toString();

        return prefixed()
            .append(Component.literal("Exported ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(caseCount)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal(" reviewed cases for Training Hub to ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(exportPath).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(". ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("[Open Hub]").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }

    public static MutableComponent trainingCasesExportStarted() {
        return prefixed().append(Component.literal("Training export started in the background.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent trainingCasesExportFailed(String message) {
        return error("Training export failed: " + displayValue(message) + ".");
    }

    public static MutableComponent trainingHubOpenFailed(String message) {
        return error("Could not open Training Hub: " + displayValue(message) + ".");
    }

    public static MutableComponent reviewSelectionRequired() {
        return error("No reviewed messages selected. Mark at least one line as scam or legit.");
    }

    public static MutableComponent reviewMessagesSaved(int scamCount, int legitCount) {
        return prefixed()
            .append(Component.literal("Saved reviewed messages. scam=").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(Math.max(0, scamCount))).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
            .append(Component.literal(", legit=").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(Math.max(0, legitCount))).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent caseReviewNeedsCaseSelection() {
        return error("No case selected. Mark at least one message as Context or Signal.");
    }

    public static MutableComponent caseReviewNeedsSignalSelection() {
        return error("Risk review needs at least one Signal message.");
    }

    public static MutableComponent caseReviewSaved(int includedCount, int signalCount, ReviewVerdict verdict) {
        return prefixed()
            .append(Component.literal("Case review saved. verdict=").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(displayVerdict(verdict)).withStyle(verdictColor(verdict), ChatFormatting.BOLD))
            .append(Component.literal(", included=").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(Math.max(0, includedCount))).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal(", signals=").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(Math.max(0, signalCount))).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal(".").withStyle(ChatFormatting.GRAY));
    }

    private static MutableComponent error(String message) {
        return prefixed().append(Component.literal(message == null ? "" : message).withStyle(ChatFormatting.RED));
    }

    private static MutableComponent prefixed() {
        return Component.literal(PREFIX).withStyle(ChatFormatting.DARK_RED);
    }

    private static MutableComponent actionTag(String label, ChatFormatting color, String hover, String command) {
        return actionTag(
            label,
            color,
            hover == null || hover.isBlank() ? null : Component.literal(hover),
            command == null || command.isBlank() ? null : new ClickEvent.RunCommand(command)
        );
    }

    private static MutableComponent urlActionTag(String label, ChatFormatting color, Component hover, String url) {
        return actionTag(
            label,
            color,
            hover,
            url == null || url.isBlank() ? null : new ClickEvent.OpenUrl(URI.create(url))
        );
    }

    static MutableComponent changelogHoverText(String changelog) {
        String normalized = changelog == null ? "" : changelog.replace("\r\n", "\n").replace('\r', '\n');
        String[] rawLines = normalized.split("\n", -1);
        int lineCount = rawLines.length;
        while (lineCount > 0 && rawLines[lineCount - 1].isBlank()) {
            lineCount--;
        }

        if (lineCount == 0) {
            return Component.literal("No changelog available.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
        }

        MutableComponent hover = Component.literal("");
        int previewLines = Math.min(10, lineCount);
        for (int index = 0; index < previewLines; index++) {
            if (index > 0) {
                hover.append(Component.literal("\n"));
            }
            hover.append(Component.literal(rawLines[index]).withStyle(ChatFormatting.GRAY));
        }
        if (lineCount > previewLines) {
            hover.append(Component.literal("\n"));
            hover.append(Component.literal("and many more...").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        return hover;
    }

    private static MutableComponent actionTag(String label, ChatFormatting color, Component hover, ClickEvent clickEvent) {
        Style style = Style.EMPTY.withColor(color);
        if (hover != null) {
            style = style.withHoverEvent(new HoverEvent.ShowText(hover));
        }
        if (clickEvent != null) {
            style = style.withClickEvent(clickEvent);
        } else {
            style = style.withStrikethrough(true);
        }

        return Component.literal("[" + label + "]").setStyle(style);
    }

    private static String displayValue(String value) {
        if (value == null || value.isBlank()) {
            return "<unknown>";
        }

        return value.trim();
    }

    private static String displayVersionOnly(String version) {
        String value = displayValue(version);
        int separator = value.indexOf('+');
        if (separator <= 0) {
            return value;
        }

        return value.substring(0, separator);
    }

    private static String displayVerdict(ReviewVerdict verdict) {
        if (verdict == null) {
            return "OPEN";
        }

        return switch (verdict) {
            case PENDING -> "OPEN";
            case RISK -> "RISK";
            case SAFE -> "SAFE";
            case IGNORED -> "DISMISSED";
        };
    }

    private static ChatFormatting verdictColor(ReviewVerdict verdict) {
        if (verdict == null) {
            return ChatFormatting.GRAY;
        }

        return switch (verdict) {
            case PENDING -> ChatFormatting.GRAY;
            case RISK -> ChatFormatting.DARK_RED;
            case SAFE -> ChatFormatting.GREEN;
            case IGNORED -> ChatFormatting.YELLOW;
        };
    }
}
