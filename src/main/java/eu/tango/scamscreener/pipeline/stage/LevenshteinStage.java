package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;

/**
 * Similarity stage reserved for fuzzy phrase matching.
 */
public final class LevenshteinStage extends Stage {
    /**
     * Evaluates similarity-based detection checks.
     *
     * @param chatEvent the chat event received from the client
     * @return the neutral placeholder result until similarity logic is implemented
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        // The v2 stage skeleton is in place; actual similarity behavior is added later.
        return pass();
    }
}
