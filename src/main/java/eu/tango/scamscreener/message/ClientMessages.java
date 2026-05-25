package eu.tango.scamscreener.message;

import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.config.data.AlertRiskLevel;
import eu.tango.scamscreener.review.ReviewVerdict;
import eu.tango.scamscreener.training.TrainingCaseExportService;
import eu.tango.scamscreener.training.TrainingHubClient;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.util.Map;

/**
 * Shared v1-style prefixed chat lines for local command feedback.
 */
public final class ClientMessages {
    private static final String PREFIX = "[ScamScreener] ";
    private static final String SCAM_WIKI_URL = "https://hypixelskyblock.minecraft.wiki/w/Scams";

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
            "Commands: enable, disable, whitelist, blacklist, review, review export, training upload, training reminder, alertlevel, autoleave, mute, unmute, edu disable, debug, metrics, profiler, rules, runtime, messages, settings."
        ).withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent educationCommandHelp() {
        return prefixed()
            .append(Component.literal("Education command usage:").withStyle(ChatFormatting.GRAY))
            .append(Component.literal("\n- /scamscreener edu disable <messageId>").withStyle(ChatFormatting.GRAY));
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
        return prefixed()
            .append(Component.literal("Education message disabled: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(displayValue(messageId)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    public static MutableComponent educationMessageUnknown(String messageId) {
        return error("Unknown education message id: " + displayValue(messageId) + ".");
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

    public static MutableComponent trainingUploadStarted(int caseCount) {
        return prefixed()
            .append(Component.literal("Training upload started in the background for ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(Math.max(0, caseCount))).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal(" reviewed cases.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent trainingUploadAlreadyRunning() {
        return prefixed().append(Component.literal("A training upload is already running.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent trainingUploadNoCasesAvailable() {
        return error("No reviewed SAFE/RISK cases are available for upload.");
    }

    public static MutableComponent trainingUploadReminder(int caseCount) {
        return prefixed()
            .append(Component.literal("You have ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(Math.max(0, caseCount))).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
            .append(Component.literal(" saved cases. Upload them anonymously to help making this mod better ").withStyle(ChatFormatting.GRAY))
            .append(actionTag(
                "upload",
                ChatFormatting.GREEN,
                "Start an anonymous Training Hub upload.",
                "/ss training upload"
            ))
            .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
            .append(actionTag(
                "don't show again",
                ChatFormatting.GRAY,
                "Disable future training upload reminders.",
                "/ss training reminder off"
            ));
    }

    public static MutableComponent trainingUploadReminderEnabled() {
        return prefixed().append(Component.literal("Training upload reminder enabled.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent trainingUploadReminderDisabled() {
        return prefixed().append(Component.literal("Training upload reminder disabled.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent trainingUploadRetryScheduled(String message, int retriesRemaining, int retryDelaySeconds) {
        String retrySuffix = retriesRemaining == 1 ? " time" : " times";
        return prefixed()
            .append(Component.literal("Training upload failed: ").withStyle(ChatFormatting.RED))
            .append(Component.literal(displayValue(message)).withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(". Retrying automatically ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(Math.max(0, retriesRemaining))).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal(" more" + retrySuffix + " with ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(String.valueOf(Math.max(1, retryDelaySeconds)) + "s").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
            .append(Component.literal(" between attempts.").withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent trainingUploadAborted(String message) {
        return error("Training upload aborted: " + displayValue(message) + ".");
    }

    public static MutableComponent trainingUploadCompleted(TrainingHubClient.UploadResult result) {
        if (result == null) {
            return prefixed().append(Component.literal("Training upload finished.").withStyle(ChatFormatting.GRAY));
        }

        return switch (result.status() == null ? "" : result.status()) {
            case "accepted" -> prefixed()
                .append(Component.literal("Training upload accepted: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(Math.max(0, result.caseCount()))).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal(" cases (").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(Math.max(0, result.insertedCases()))).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal(" inserted, ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(Math.max(0, result.updatedCases()))).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .append(Component.literal(" updated).").withStyle(ChatFormatting.GRAY));
            case "duplicate" -> prefixed()
                .append(Component.literal("Training upload duplicate: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(Math.max(0, result.caseCount()))).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal(" cases already existed on the server.").withStyle(ChatFormatting.GRAY));
            case "quota-exceeded" -> prefixed()
                .append(Component.literal("Training upload stopped: ").withStyle(ChatFormatting.RED))
                .append(Component.literal(displayValue(result.detail())).withStyle(ChatFormatting.YELLOW));
            default -> prefixed().append(Component.literal("Training upload finished.").withStyle(ChatFormatting.GRAY));
        };
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

    private static MutableComponent educationWarning(String disableCommand, String... guidanceParts) {
        MutableComponent line = prefixed();
        if (guidanceParts != null) {
            for (String guidancePart : guidanceParts) {
                if (guidancePart == null || guidancePart.isBlank()) {
                    continue;
                }
                line.append(Component.literal(guidancePart).withStyle(ChatFormatting.GRAY));
            }
        }

        line.append(Component.literal("More info and help can be found ").withStyle(ChatFormatting.GRAY))
            .append(urlActionTag(
                "here",
                ChatFormatting.YELLOW,
                Component.literal("Open Community Wiki to learn more about Scams").withStyle(ChatFormatting.GRAY),
                SCAM_WIKI_URL
            ))
            .append(Component.literal(". ").withStyle(ChatFormatting.GRAY))
            .append(actionTag(
                "disable info message",
                ChatFormatting.DARK_GRAY,
                "Disable this message.",
                disableCommand
            ));
        return line;
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
