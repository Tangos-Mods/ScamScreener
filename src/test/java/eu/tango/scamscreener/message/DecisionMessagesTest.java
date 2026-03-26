package eu.tango.scamscreener.message;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import net.minecraft.text.MutableText;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionMessagesTest {
    @AfterEach
    void tearDown() {
        AlertContextRegistry.clear();
    }

    @Test
    void riskWarningShowsReviewActionLabel() {
        MutableText message = DecisionMessages.riskWarning(
            ChatEvent.messageOnly("visit my island"),
            new PipelineDecision(PipelineDecision.Outcome.REVIEW, 42, "RuleStage", List.of(), List.of("Matched heuristic"))
        );

        String rendered = message.getString();
        assertTrue(rendered.contains("[review] [info]"));
        assertFalse(rendered.contains("[manage]"));
    }
}
