package eu.tango.scamscreener.review;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewStoreTest {
    @Test
    void capturesReviewOutcomes() {
        ReviewStore store = new ReviewStore();
        PipelineDecision decision = new PipelineDecision(
            PipelineDecision.Outcome.REVIEW,
            42,
            "RuleStage",
            java.util.List.of(),
            java.util.List.of("RULE_MATCH")
        );

        boolean captured = store.capture(ChatEvent.messageOnly("please add me on discord"), decision).isPresent();

        assertTrue(captured);
        assertEquals(1, store.entries().size());
        assertEquals(42, store.entries().getFirst().getScore());
        assertEquals(ReviewVerdict.PENDING, store.entries().getFirst().getVerdict());
    }

    @Test
    void ignoresNonReviewOutcomes() {
        ReviewStore store = new ReviewStore();
        PipelineDecision decision = new PipelineDecision(
            PipelineDecision.Outcome.IGNORE,
            0,
            "",
            java.util.List.of(),
            java.util.List.of()
        );

        boolean captured = store.capture(ChatEvent.messageOnly("hello"), decision).isPresent();

        assertFalse(captured);
        assertTrue(store.entries().isEmpty());
    }
}
