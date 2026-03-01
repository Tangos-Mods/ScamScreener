package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;

/**
 * Final expensive stage reserved for model-based scoring.
 */
public final class ModelStage extends Stage {
    /**
     * Evaluates model-based detection checks.
     *
     * @param chatEvent the chat event received from the client
     * @return the neutral placeholder result until model logic is implemented
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        // The v2 stage skeleton is in place; actual model scoring is added later.
        return pass();
    }
}
