package eu.tango.scamscreener.review;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.api.event.PipelineDecisionEvent;
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
        ScamScreenerRuntime.getInstance().reviewStore().capture(chatEvent, decision);
    }
}
