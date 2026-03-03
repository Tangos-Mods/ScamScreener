package eu.tango.scamscreener.review;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.api.event.PipelineDecisionEvent;
import eu.tango.scamscreener.config.data.AutoCaptureAlertLevel;
import eu.tango.scamscreener.message.AlertSeverity;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;

/**
 * Captures review-worthy pipeline outcomes into the shared review store.
 */
public final class ReviewCaptureHandler {
    private static boolean initialized;

    private ReviewCaptureHandler() {
    }

    /**
     * Registers the review capture listener once.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        PipelineDecisionEvent.EVENT.register(ReviewCaptureHandler::onPipelineDecision);
    }

    private static void onPipelineDecision(ChatEvent chatEvent, PipelineDecision decision) {
        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        if (!runtime.config().review().isCaptureEnabled()) {
            return;
        }
        AutoCaptureAlertLevel autoCaptureLevel = runtime.config().alerts().autoCaptureLevel();
        if (!autoCaptureLevel.captures(AlertSeverity.fromDecision(decision).riskLevel())) {
            return;
        }

        runtime.reviewStore().capture(chatEvent, decision);
    }
}
