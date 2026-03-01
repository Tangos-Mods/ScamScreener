package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;

/**
 * Stateful behavior stage reserved for sender-specific heuristics.
 */
public final class BehaviorStage extends Stage {
    /**
     * Evaluates behavior-based detection checks.
     *
     * @param chatEvent the chat event received from the client
     * @return the neutral placeholder result until behavior logic is implemented
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        // The v2 stage skeleton is in place; actual behavior analysis is added later.
        return pass();
    }
}
