package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;

/**
 * Core rule-based detection stage for exact and regex-style matches.
 */
public final class RuleStage extends Stage {
    /**
     * Evaluates deterministic rule checks.
     *
     * @param chatEvent the chat event received from the client
     * @return the neutral placeholder result until rule logic is implemented
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        // The v2 stage skeleton is in place; actual rule behavior is added later.
        return pass();
    }
}
