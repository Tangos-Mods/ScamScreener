package eu.tango.scamscreener.message;

import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.config.data.AlertRiskLevel;
import eu.tango.scamscreener.review.ReviewVerdict;
import eu.tango.scamscreener.training.TrainingCaseExportService;
import eu.tango.scamscreener.training.TrainingHubClient;
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
    private static final String COMMUNITY_WIKI_URL_SCAM = "https://hypixelskyblock.minecraft.wiki/w/Scams";

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
            "Commands: enable, disable, whitelist, blacklist, review, review export, training upload, training reminder, alertlevel, autoleave, mute, unmute, edu disable, debug, metrics, profiler, rules, runtime, messages, settings."
        ).formatted(Formatting.GRAY));
    }

    public static MutableText educationCommandHelp() {
        return prefixed()
            .append(Text.literal("Education command usage:").formatted(Formatting.GRAY))
            .append(Text.literal("\n- /scamscreener edu disable <messageId>").formatted(Formatting.GRAY));
    }

    public static MutableText educationExternalPlatformWarning(String disableCommand) {
        return educationWarning(
            disableCommand,
            "The user is trying to move you over to an external platform. ",
            "Scammers often do this, so proceed with caution. ",
            "If you're unsure whether it's a scam, treat it as one until proven otherwise. "
        );
    }

    public static MutableText educationSuspiciousLinkWarning(String disableCommand) {
        return educationWarning(
            disableCommand,
            "The message contains a suspicious link. ",
            "Never log into websites opened directly from chat links. ",
            "Open trusted sites manually and double-check the exact domain first. "
        );
    }

    public static MutableText educationUpfrontPaymentWarning(String disableCommand) {
        return educationWarning(
            disableCommand,
            "The user asks for payment before proof or delivery. ",
            "This is a common scam setup in trading chats. ",
            "Only trade with verified middlemen and never pay first without strong proof. "
        );
    }

    public static MutableText educationAccountDataWarning(String disableCommand) {
        return educationWarning(
            disableCommand,
            "The user asks for account or personal login data. ",
            "Never share your Microsoft login, email codes, or recovery information. ",
            "Legitimate players and staff do not need your credentials. "
        );
    }

    public static MutableText educationFakeMiddlemanWarning(String disableCommand) {
        return educationWarning(
            disableCommand,
            "The user claims a trusted middleman without reliable proof. ",
            "Scammers often fake middleman identities with screenshots or name lookalikes. ",
            "Verify middlemen only through official server channels before trading. "
        );
    }

    public static MutableText educationUrgencyWarning(String disableCommand) {
        return educationWarning(
            disableCommand,
            "The user is creating pressure and urgency. ",
            "Scammers rush decisions to prevent verification. ",
            "Slow down, verify details, and walk away if they keep pushing. "
        );
    }

    public static MutableText educationTrustManipulationWarning(String disableCommand) {
        return educationWarning(
            disableCommand,
            "The user is trying to force trust quickly. ",
            "Claims like 'trusted', 'friend of admin', or 'many vouches' can be faked. ",
            "Always verify reputation independently before sending anything. "
        );
    }

    public static MutableText educationTooGoodToBeTrueWarning(String disableCommand) {
        return educationWarning(
            disableCommand,
            "The offer looks too good to be true. ",
            "Unreal discounts, huge profit promises, or free rare items are common bait. ",
            "If the deal makes no sense economically, treat it as high risk. "
        );
    }

    public static MutableText educationDiscordHandleWarning(String disableCommand) {
        return educationWarning(
            disableCommand,
            "The chat contains a Discord handle in suspicious context. ",
            "Scammers often move victims to DMs where logs and moderation are weaker. ",
            "Verify identity via official communities before continuing outside Minecraft. "
        );
    }

    public static MutableText educationFunnelSequenceWarning(String disableCommand) {
        return educationWarning(
            disableCommand,
            "The conversation matches a staged scam funnel pattern. ",
            "These chats usually start harmless, then build trust, then ask for risky actions. ",
            "Stop at the first request for payment, account access, or off-platform contact. "
        );
    }

    public static MutableText educationMessageDisabled(String messageId) {
        return prefixed()
            .append(Text.literal("Education message disabled: ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(messageId)).formatted(Formatting.GOLD, Formatting.BOLD));
    }

    public static MutableText educationMessageUnknown(String messageId) {
        return error("Unknown education message id: " + displayValue(messageId) + ".");
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

    public static MutableText trainingUploadStarted(int caseCount) {
        return prefixed()
            .append(Text.literal("Training upload started in the background for ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(Math.max(0, caseCount))).formatted(Formatting.AQUA, Formatting.BOLD))
            .append(Text.literal(" reviewed cases.").formatted(Formatting.GRAY));
    }

    public static MutableText trainingUploadAlreadyRunning() {
        return prefixed().append(Text.literal("A training upload is already running.").formatted(Formatting.GRAY));
    }

    public static MutableText trainingUploadNoCasesAvailable() {
        return error("No reviewed SAFE/RISK cases are available for upload.");
    }

    public static MutableText trainingUploadRetryScheduled(String message, int retriesRemaining, int retryDelaySeconds) {
        String retrySuffix = retriesRemaining == 1 ? " time" : " times";
        return prefixed()
            .append(Text.literal("Training upload failed: ").formatted(Formatting.RED))
            .append(Text.literal(displayValue(message)).formatted(Formatting.YELLOW))
            .append(Text.literal(". Retrying automatically ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(Math.max(0, retriesRemaining))).formatted(Formatting.GOLD, Formatting.BOLD))
            .append(Text.literal(" more" + retrySuffix + " with ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(Math.max(1, retryDelaySeconds)) + "s").formatted(Formatting.GOLD, Formatting.BOLD))
            .append(Text.literal(" between attempts.").formatted(Formatting.GRAY));
    }

    public static MutableText trainingUploadAborted(String message) {
        return error("Training upload aborted: " + displayValue(message) + ".");
    }

    public static MutableText trainingUploadCompleted(TrainingHubClient.UploadResult result) {
        if (result == null) {
            return prefixed().append(Text.literal("Training upload finished.").formatted(Formatting.GRAY));
        }

        return switch (result.status() == null ? "" : result.status()) {
            case "accepted" -> prefixed()
                .append(Text.literal("Training upload accepted: ").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(Math.max(0, result.caseCount()))).formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal(" cases (").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(Math.max(0, result.insertedCases()))).formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal(" inserted, ").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(Math.max(0, result.updatedCases()))).formatted(Formatting.YELLOW, Formatting.BOLD))
                .append(Text.literal(" updated).").formatted(Formatting.GRAY));
            case "duplicate" -> prefixed()
                .append(Text.literal("Training upload duplicate: ").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(Math.max(0, result.caseCount()))).formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal(" cases already existed on the server.").formatted(Formatting.GRAY));
            case "quota-exceeded" -> prefixed()
                .append(Text.literal("Training upload stopped: ").formatted(Formatting.RED))
                .append(Text.literal(displayValue(result.detail())).formatted(Formatting.YELLOW));
            default -> prefixed().append(Text.literal("Training upload finished.").formatted(Formatting.GRAY));
        };
    }

    public static MutableText trainingUploadReminder(int caseCount) {
        return prefixed()
            .append(Text.literal("You have ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(Math.max(0, caseCount))).formatted(Formatting.AQUA, Formatting.BOLD))
            .append(Text.literal(" saved cases. Upload them anonymously to help making this mod better ").formatted(Formatting.GRAY))
            .append(actionTag(
                "upload",
                Formatting.GREEN,
                "Start an anonymous Training Hub upload.",
                "/ss training upload"
            ))
            .append(Text.literal(" ").formatted(Formatting.GRAY))
            .append(actionTag(
                "don't show again",
                Formatting.GRAY,
                "Disable future training upload reminders.",
                "/ss training reminder off"
            ));
    }

    public static MutableText trainingUploadReminderEnabled() {
        return prefixed().append(Text.literal("Training upload reminder enabled.").formatted(Formatting.GRAY));
    }

    public static MutableText trainingUploadReminderDisabled() {
        return prefixed().append(Text.literal("Training upload reminder disabled.").formatted(Formatting.GRAY));
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

    private static MutableText educationWarning(String disableCommand, String... guidanceParts) {
        MutableText line = prefixed();
        if (guidanceParts != null) {
            for (String guidancePart : guidanceParts) {
                if (guidancePart == null || guidancePart.isBlank()) {
                    continue;
                }
                line.append(Text.literal(guidancePart).formatted(Formatting.GRAY));
            }
        }

        line.append(Text.literal("More info and help can be found ").formatted(Formatting.GRAY))
            .append(urlActionTag(
                "here",
                Formatting.YELLOW,
                Text.literal("Open Community Wiki to learn more about Scams").formatted(Formatting.GRAY),
                COMMUNITY_WIKI_URL_SCAM
            ))
            .append(Text.literal(". ").formatted(Formatting.GRAY))
            .append(actionTag(
                "disable info message",
                Formatting.DARK_GRAY,
                "Disable this message.",
                disableCommand
            ));
        return line;
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
