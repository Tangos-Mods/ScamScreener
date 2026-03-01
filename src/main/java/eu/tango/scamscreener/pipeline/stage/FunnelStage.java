package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;

/**
 * Stateful funnel stage reserved for multi-step scam progression detection.
 */
public final class FunnelStage extends Stage {
    /**
     * Evaluates funnel-sequence detection checks.
     *
     * @param chatEvent the chat event received from the client
     * @return the neutral placeholder result until funnel logic is implemented
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        // The v2 stage skeleton is in place; actual funnel analysis is added later.
        return pass();
    }
}
