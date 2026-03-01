package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;

/**
 * First pipeline stage reserved for mute and suppression logic.
 */
public final class MuteStage extends Stage {
    /**
     * Evaluates mute-specific rules.
     *
     * @param chatEvent the chat event received from the client
     * @return the neutral placeholder result until mute logic is implemented
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        // The v2 stage skeleton is in place; actual mute behavior is added later.
        return pass();
    }
}
