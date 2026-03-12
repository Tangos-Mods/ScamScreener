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

            RuntimeConfig.OutputSettings output = ScamScreenerRuntime.getInstance().config().output();
            AlertRiskLevel minimumRiskLevel = ScamScreenerRuntime.getInstance().config().alerts().minimumRiskLevel();
            AlertSeverity severity = AlertSeverity.fromDecision(decision);
            switch (decision.getOutcome()) {
                case REVIEW, BLOCK -> {
                    if (output.isShowRiskWarningMessage()
                        && severity.riskLevel().isAtLeast(minimumRiskLevel)
                        && meetsVisibleRiskWarningThreshold(decision)) {
                        MessageDispatcher.reply(DecisionMessages.riskWarning(chatEvent, decision));
                    }
                    if (output.isPingOnRiskWarning()
                        && severity.riskLevel().isAtLeast(minimumRiskLevel)
                        && meetsVisibleRiskWarningThreshold(decision)) {
                        NotificationService.playWarningTone();
                    }
                }
                case BLACKLISTED -> {
                    if (output.isShowBlacklistWarningMessage()) {
                        MessageDispatcher.reply(DecisionMessages.blacklistWarning(chatEvent, decision));
                    }
                    if (output.isPingOnBlacklistWarning()) {
                        NotificationService.playWarningTone();
                    }
                    if (ScamScreenerRuntime.getInstance().config().safety().isAutoLeaveOnBlacklist()) {
                        MessageDispatcher.sendCommand("p leave");
                        if (output.isShowAutoLeaveMessage()) {
                            MessageDispatcher.reply(ClientMessages.autoLeaveExecuted(chatEvent == null ? "" : chatEvent.getSenderName()));
                        }
                    }
                }
                default -> {
                }
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
