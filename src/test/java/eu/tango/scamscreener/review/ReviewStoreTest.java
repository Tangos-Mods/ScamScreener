package eu.tango.scamscreener.review;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.pipeline.data.StageResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReviewStoreTest {
    @Test
    void capturesReviewOutcomes() {
        ReviewStore store = new ReviewStore();
        PipelineDecision decision = new PipelineDecision(
            PipelineDecision.Outcome.REVIEW,
            42,
            "RuleStage",
            java.util.List.of(StageResult.score("RuleStage", 20, "RULE_EXTERNAL_PLATFORM")),
            java.util.List.of("RULE_MATCH")
        );

        boolean captured = store.capture(ChatEvent.messageOnly("please add me on discord"), decision).isPresent();

        assertTrue(captured);
        assertEquals(1, store.entries().size());
        assertEquals(42, store.entries().getFirst().getScore());
        assertEquals(ReviewVerdict.PENDING, store.entries().getFirst().getVerdict());
        assertEquals(1, store.entries().getFirst().getReasons().size());
        assertEquals(1, store.entries().getFirst().getStageResults().size());
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

    @Test
    void filtersAndSearchesStoredEntries() {
        ReviewStore store = new ReviewStore();
        PipelineDecision decision = new PipelineDecision(
            PipelineDecision.Outcome.REVIEW,
            18,
            "BehaviorStage",
            java.util.List.of(),
            java.util.List.of("BEHAVIOR_REPEAT")
        );

        store.capture(new ChatEvent("add me on discord", null, "Alpha", 100L), decision);
        store.capture(new ChatEvent("free coins", null, "Beta", 200L), decision);
        String newestId = store.entries().getFirst().getId();
        store.setVerdict(newestId, ReviewVerdict.RISK);

        assertEquals(1, store.entries(ReviewVerdict.RISK, "").size());
        assertEquals(1, store.entries(null, "discord").size());
        assertEquals("Alpha", store.entries(null, "alpha").getFirst().getSenderName());
        assertSame(store.entries().getFirst(), store.find(newestId).orElseThrow());
    }

    @Test
    void replaceAllRestoresEntriesAndKeepsIdsMonotonic() {
        ReviewStore store = new ReviewStore();
        ReviewEntry restored = new ReviewEntry(
            "review-8",
            null,
            "Gamma",
            "restored entry",
            12,
            "TrendStage",
            300L,
            java.util.List.of("TREND_REPEAT"),
            java.util.List.of(StageResult.score("TrendStage", 10, "TREND_REPEAT"))
        );
        restored.setVerdict(ReviewVerdict.SAFE);

        store.replaceAll(java.util.List.of(restored));

        assertEquals(1, store.entries().size());
        assertEquals(ReviewVerdict.SAFE, store.entries().getFirst().getVerdict());

        PipelineDecision decision = new PipelineDecision(
            PipelineDecision.Outcome.REVIEW,
            7,
            "RuleStage",
            java.util.List.of(),
            java.util.List.of()
        );
        ReviewEntry captured = store.capture(ChatEvent.messageOnly("next"), decision).orElseThrow();

        assertEquals("review-9", captured.getId());
    }

    @Test
    void reducingCapacityTrimsOldestEntries() {
        ReviewStore store = new ReviewStore();
        PipelineDecision decision = new PipelineDecision(
            PipelineDecision.Outcome.REVIEW,
            5,
            "RuleStage",
            java.util.List.of(),
            java.util.List.of()
        );

        store.capture(ChatEvent.messageOnly("one"), decision);
        store.capture(ChatEvent.messageOnly("two"), decision);
        store.capture(ChatEvent.messageOnly("three"), decision);
        store.setMaxEntries(2);

        assertEquals(2, store.entries().size());
        assertEquals("three", store.entries().get(0).getMessage());
        assertEquals("two", store.entries().get(1).getMessage());
    }
}
