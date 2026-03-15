package eu.tango.scamscreener.message;

import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.config.data.AlertRiskLevel;
import eu.tango.scamscreener.review.ReviewVerdict;
import eu.tango.scamscreener.training.TrainingCaseExportService;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
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
            .append(Text.literal((level == null ? AlertRiskLevel.MEDIUM : level).name()).formatted(Formatting.GOLD, Formatting.BOLD))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText updatedAlertLevel(AlertRiskLevel level) {
        return prefixed()
            .append(Text.literal("Alert threshold set to ").formatted(Formatting.GRAY))
            .append(Text.literal((level == null ? AlertRiskLevel.MEDIUM : level).name()).formatted(Formatting.GOLD, Formatting.BOLD))
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

    public static MutableText scamScreenerEnabled() {
        return prefixed().append(Text.literal("ScamScreener enabled.").formatted(Formatting.GRAY));
    }

    public static MutableText scamScreenerDisabled() {
        return prefixed().append(Text.literal("ScamScreener disabled.").formatted(Formatting.GRAY));
    }

    public static MutableText scamScreenerDisabledJoinNotice() {
        return prefixed()
            .append(Text.literal("ScamScreener is disabled. ").formatted(Formatting.GRAY))
            .append(actionTag("Click", Formatting.GREEN, "Enable ScamScreener.", "/ss enable"))
            .append(Text.literal(" to enable it again.").formatted(Formatting.GRAY));
    }

    public static MutableText updateAvailable(String currentVersion, String latestVersion, String modrinthUrl, String changelog) {
        return prefixed()
            .append(Text.literal("Update Available ").formatted(Formatting.GRAY))
            .append(Text.literal(displayVersionOnly(currentVersion)).formatted(Formatting.YELLOW))
            .append(Text.literal(" -> ").formatted(Formatting.DARK_GRAY))
            .append(Text.literal(displayVersionOnly(latestVersion)).formatted(Formatting.GREEN, Formatting.BOLD))
            .append(Text.literal(". ").formatted(Formatting.GRAY))
            .append(urlActionTag("click", Formatting.YELLOW, changelogHoverText(changelog), modrinthUrl))
            .append(Text.literal(" to open on Modrinth").formatted(Formatting.GRAY));
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

    public static MutableText profilerStatus(boolean enabled) {
        return prefixed()
            .append(Text.literal("Profiler HUD: ").formatted(Formatting.GRAY))
            .append(Text.literal(enabled ? "ON" : "OFF").formatted(enabled ? Formatting.GREEN : Formatting.RED, Formatting.BOLD))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText profilerEnabled() {
        return prefixed().append(Text.literal("Profiler HUD enabled.").formatted(Formatting.GRAY));
    }

    public static MutableText profilerDisabled() {
        return prefixed().append(Text.literal("Profiler HUD disabled.").formatted(Formatting.GRAY));
    }

    public static MutableText profilerWebOpened(String url) {
        return prefixed()
            .append(Text.literal("Profiler web view opened. ").formatted(Formatting.GRAY))
            .append(urlActionTag(
                "click",
                Formatting.YELLOW,
                Text.literal(displayValue(url)).formatted(Formatting.GRAY),
                url
            ))
            .append(Text.literal(" to open it again manually.").formatted(Formatting.GRAY));
    }

    public static MutableText profilerWebMissingDependency(String url) {
        return prefixed()
            .append(Text.literal("Tango Web API is missing. ").formatted(Formatting.RED))
            .append(urlActionTag(
                "click",
                Formatting.YELLOW,
                Text.literal("Download Tango Web API from Modrinth.").formatted(Formatting.GRAY),
                url
            ))
            .append(Text.literal(" to download it from Modrinth.").formatted(Formatting.RED));
    }

    public static MutableText profilerWebUnavailable(String message) {
        return error("Web profiler unavailable: " + displayValue(message) + ".");
    }

    public static MutableText profilerWebOpenFailed(String message) {
        return error("Could not open the profiler web view: " + displayValue(message) + ".");
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

    public static MutableText commandHelp() {
        return prefixed().append(Text.literal(
            "Commands: enable, disable, whitelist, blacklist, review, review export, alertlevel, autoleave, mute, unmute, debug, metrics, profiler, rules, runtime, messages, settings."
        ).formatted(Formatting.GRAY));
    }

    public static MutableText trainingCasesExported(TrainingCaseExportService.TrainingCaseExportResult result) {
        int caseCount = result == null ? 0 : Math.max(0, result.exportedCaseCount());
        String exportPath = result == null || result.trainingCasesFile() == null
            ? "<unknown>"
            : result.trainingCasesFile().toString();

        return prefixed()
            .append(Text.literal("Exported ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(caseCount)).formatted(Formatting.AQUA, Formatting.BOLD))
            .append(Text.literal(" reviewed cases for Training Hub to ").formatted(Formatting.GRAY))
            .append(Text.literal(exportPath).formatted(Formatting.YELLOW))
            .append(Text.literal(". ").formatted(Formatting.GRAY))
            .append(Text.literal("[Open Hub]").formatted(Formatting.YELLOW, Formatting.ITALIC));
    }

    public static MutableText trainingCasesExportStarted() {
        return prefixed().append(Text.literal("Training export started in the background.").formatted(Formatting.GRAY));
    }

    public static MutableText trainingCasesExportFailed(String message) {
        return error("Training export failed: " + displayValue(message) + ".");
    }

    public static MutableText trainingHubOpenFailed(String message) {
        return error("Could not open Training Hub: " + displayValue(message) + ".");
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

    public static MutableText caseReviewNeedsCaseSelection() {
        return error("No case selected. Mark at least one message as Context or Signal.");
    }

    public static MutableText caseReviewNeedsSignalSelection() {
        return error("Risk review needs at least one Signal message.");
    }

    public static MutableText caseReviewSaved(int includedCount, int signalCount, ReviewVerdict verdict) {
        return prefixed()
            .append(Text.literal("Case review saved. verdict=").formatted(Formatting.GRAY))
            .append(Text.literal(displayVerdict(verdict)).formatted(verdictColor(verdict), Formatting.BOLD))
            .append(Text.literal(", included=").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(Math.max(0, includedCount))).formatted(Formatting.AQUA, Formatting.BOLD))
            .append(Text.literal(", signals=").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(Math.max(0, signalCount))).formatted(Formatting.GOLD, Formatting.BOLD))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    private static MutableText error(String message) {
        return prefixed().append(Text.literal(message == null ? "" : message).formatted(Formatting.RED));
    }

    private static MutableText prefixed() {
        return Text.literal(PREFIX).formatted(Formatting.DARK_RED);
    }

    private static MutableText actionTag(String label, Formatting color, String hover, String command) {
        return actionTag(
            label,
            color,
            hover == null || hover.isBlank() ? null : Text.literal(hover),
            command == null || command.isBlank() ? null : new ClickEvent.RunCommand(command)
        );
    }

    private static MutableText urlActionTag(String label, Formatting color, Text hover, String url) {
        return actionTag(
            label,
            color,
            hover,
            url == null || url.isBlank() ? null : new ClickEvent.OpenUrl(URI.create(url))
        );
    }

    static MutableText changelogHoverText(String changelog) {
        String normalized = changelog == null ? "" : changelog.replace("\r\n", "\n").replace('\r', '\n');
        String[] rawLines = normalized.split("\n", -1);
        int lineCount = rawLines.length;
        while (lineCount > 0 && rawLines[lineCount - 1].isBlank()) {
            lineCount--;
        }

        if (lineCount == 0) {
            return Text.literal("No changelog available.").formatted(Formatting.DARK_GRAY, Formatting.ITALIC);
        }

        MutableText hover = Text.literal("");
        int previewLines = Math.min(10, lineCount);
        for (int index = 0; index < previewLines; index++) {
            if (index > 0) {
                hover.append(Text.literal("\n"));
            }
            hover.append(Text.literal(rawLines[index]).formatted(Formatting.GRAY));
        }
        if (lineCount > previewLines) {
            hover.append(Text.literal("\n"));
            hover.append(Text.literal("and many more...").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        }

        return hover;
    }

    private static MutableText actionTag(String label, Formatting color, Text hover, ClickEvent clickEvent) {
        Style style = Style.EMPTY.withColor(color);
        if (hover != null) {
            style = style.withHoverEvent(new HoverEvent.ShowText(hover));
        }
        if (clickEvent != null) {
            style = style.withClickEvent(clickEvent);
        } else {
            style = style.withStrikethrough(true);
        }

        return Text.literal("[" + label + "]").setStyle(style);
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

    private static Formatting verdictColor(ReviewVerdict verdict) {
        if (verdict == null) {
            return Formatting.GRAY;
        }

        return switch (verdict) {
            case PENDING -> Formatting.GRAY;
            case RISK -> Formatting.DARK_RED;
            case SAFE -> Formatting.GREEN;
            case IGNORED -> Formatting.YELLOW;
        };
    }
}
