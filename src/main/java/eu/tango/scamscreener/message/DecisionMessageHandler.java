package eu.tango.scamscreener.message;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.AlertRiskLevel;
import eu.tango.scamscreener.api.event.PipelineDecisionEvent;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.profiler.ScamScreenerProfiler;

/**
 * Bridges final pipeline decisions into user-facing chat messages and sounds.
 */
public final class DecisionMessageHandler {
    private static final int MIN_VISIBLE_REVIEW_WARNING_SCORE = 10;

    private static boolean initialized;

    private DecisionMessageHandler() {
    }

    /**
     * Registers the decision message handler once.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        PipelineDecisionEvent.EVENT.register(DecisionMessageHandler::onPipelineDecision);
    }

    private static void onPipelineDecision(ChatEvent chatEvent, PipelineDecision decision) {
        try (ScamScreenerProfiler.Scope ignored = ScamScreenerProfiler.getInstance().scope("decision.messages", "  Decision Messages")) {
            if (decision == null) {
                return;
            }

            RuntimeConfig runtimeConfig = ScamScreenerRuntime.getInstance().config();
            switch (decision.getOutcome()) {
                case REVIEW, BLOCK -> handleRiskDecision(chatEvent, decision, runtimeConfig);
                case BLACKLISTED -> handleBlacklistDecision(chatEvent, decision, runtimeConfig);
                default -> {
                }
            }
        }
    }

    private static void handleRiskDecision(ChatEvent chatEvent, PipelineDecision decision, RuntimeConfig runtimeConfig) {
        RuntimeConfig.OutputSettings output = runtimeConfig.output();
        AlertSeverity severity = AlertSeverity.fromDecision(decision);
        AlertRiskLevel minimumRiskLevel = runtimeConfig.alerts().minimumRiskLevel();
        boolean shouldNotify = severity.riskLevel().isAtLeast(minimumRiskLevel)
            && meetsVisibleRiskWarningThreshold(decision);

        if (output.isShowRiskWarningMessage() && shouldNotify) {
            MessageDispatcher.reply(DecisionMessages.riskWarning(chatEvent, decision));
            var educationFollowUp = EducationMessages.followUpFor(decision);
            if (educationFollowUp != null) {
                MessageDispatcher.reply(educationFollowUp);
            }
        }
        if (output.isPingOnRiskWarning() && shouldNotify) {
            NotificationService.playWarningTone();
        }
    }

    private static void handleBlacklistDecision(ChatEvent chatEvent, PipelineDecision decision, RuntimeConfig runtimeConfig) {
        RuntimeConfig.OutputSettings output = runtimeConfig.output();
        if (output.isShowBlacklistWarningMessage()) {
            MessageDispatcher.reply(DecisionMessages.blacklistWarning(chatEvent, decision));
        }
        if (output.isPingOnBlacklistWarning()) {
            NotificationService.playWarningTone();
        }
        if (runtimeConfig.safety().isAutoLeaveOnBlacklist()) {
            MessageDispatcher.sendCommand("p leave");
            if (output.isShowAutoLeaveMessage()) {
                MessageDispatcher.reply(ClientMessages.autoLeaveExecuted(chatEvent == null ? "" : chatEvent.getSenderName()));
            }
        }
    }

    private static boolean meetsVisibleRiskWarningThreshold(PipelineDecision decision) {
        if (decision == null) {
            return false;
        }

        if (decision.getOutcome() == PipelineDecision.Outcome.BLOCK) {
            return true;
        }

        return Math.max(0, decision.getTotalScore()) >= MIN_VISIBLE_REVIEW_WARNING_SCORE;
    }
}
