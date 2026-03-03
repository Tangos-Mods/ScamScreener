package eu.tango.scamscreener.message;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.AlertRiskLevel;
import eu.tango.scamscreener.api.event.PipelineDecisionEvent;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;

/**
 * Bridges final pipeline decisions into user-facing chat messages and sounds.
 */
public final class DecisionMessageHandler {
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
        if (decision == null) {
            return;
        }

        RuntimeConfig.OutputSettings output = ScamScreenerRuntime.getInstance().config().output();
        AlertRiskLevel minimumRiskLevel = ScamScreenerRuntime.getInstance().config().alerts().minimumRiskLevel();
        AlertSeverity severity = AlertSeverity.fromDecision(decision);
        switch (decision.getOutcome()) {
            case REVIEW, BLOCK -> {
                if (output.isShowRiskWarningMessage() && severity.riskLevel().isAtLeast(minimumRiskLevel)) {
                    MessageDispatcher.reply(DecisionMessages.riskWarning(chatEvent, decision));
                }
                if (output.isPingOnRiskWarning() && severity.riskLevel().isAtLeast(minimumRiskLevel)) {
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
