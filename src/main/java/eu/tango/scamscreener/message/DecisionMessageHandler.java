package eu.tango.scamscreener.message;

import eu.tango.scamscreener.ScamScreenerRuntime;
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
        switch (decision.getOutcome()) {
            case REVIEW, BLOCK -> {
                if (output.isShowRiskWarningMessage()) {
                    MessageDispatcher.reply(DecisionMessages.riskWarning(chatEvent, decision));
                }
                if (output.isPingOnRiskWarning()) {
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
            }
            default -> {
            }
        }
    }
}
