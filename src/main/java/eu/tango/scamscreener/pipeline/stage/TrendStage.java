package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;

/**
 * Stateful trend stage reserved for cross-message pattern detection.
 */
public final class TrendStage extends Stage {
    /**
     * Evaluates trend-based detection checks.
     *
     * @param chatEvent the chat event received from the client
     * @return the neutral placeholder result until trend logic is implemented
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        // The v2 stage skeleton is in place; actual trend analysis is added later.
        return pass();
    }
}
