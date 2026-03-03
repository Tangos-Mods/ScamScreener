package eu.tango.scamscreener.message;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.review.ReviewEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertContextRegistryTest {
    @BeforeEach
    void setUp() {
        AlertContextRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        AlertContextRegistry.clear();
    }

    @Test
    void storesConcreteRuleDetailsAndFindsLatestPlayerContext() {
        PipelineDecision firstDecision = new PipelineDecision(
            PipelineDecision.Outcome.REVIEW,
            42,
            "RuleStage",
            List.of(StageResult.score(
                "RuleStage",
                42,
                "Suspicious link: \"discord.gg\"; External platform push: \"discord\""
            )),
            List.of("Suspicious link: \"discord.gg\"")
        );
        String firstId = AlertContextRegistry.register(
            new ChatEvent("first message", null, "Sam", 1_000L, ChatSourceType.PLAYER),
            firstDecision
        );

        PipelineDecision secondDecision = new PipelineDecision(
            PipelineDecision.Outcome.BLOCK,
            80,
            "BehaviorStage",
            List.of(StageResult.score("BehaviorStage", 38, "Repeated contact message x3")),
            List.of("Repeated contact message x3")
        );
        String secondId = AlertContextRegistry.register(
            new ChatEvent("second message", null, "Sam", 2_000L, ChatSourceType.PLAYER),
            secondDecision
        );

        AlertContextRegistry.AlertContext firstContext = AlertContextRegistry.find(firstId).orElseThrow();
        assertEquals(List.of("first message"), firstContext.capturedMessages());
        assertEquals(2, firstContext.ruleDetails().size());
        assertEquals("RuleStage", firstContext.ruleDetails().get(0).source());
        assertEquals("Suspicious link: \"discord.gg\"", firstContext.ruleDetails().get(0).detail());

        AlertContextRegistry.AlertContext latestContext = AlertContextRegistry.findMostRecentForPlayer("sam").orElseThrow();
        assertEquals(secondId, latestContext.id());
        assertEquals(List.of("second message"), latestContext.capturedMessages());
        assertFalse(AlertContextRegistry.findMostRecentForPlayer("missing").isPresent());
        assertTrue(AlertContextRegistry.recentPlayerNames().contains("Sam"));
    }

    @Test
    void createsSyntheticAlertInfoContextFromReviewEntry() {
        ReviewEntry entry = new ReviewEntry(
            "review-1",
            null,
            "Sam",
            "§bSam : legit middleman",
            35,
            "LevenshteinStage",
            3_000L,
            List.of("Similarity match: \"middleman\""),
            List.of(StageResult.score("LevenshteinStage", 35, "Similarity match: \"middleman\""))
        );

        AlertContextRegistry.AlertContext context = AlertContextRegistry.createReviewContext(entry).orElseThrow();
        assertEquals("Sam", context.displayPlayerName());
        assertEquals("review-1", context.linkedReviewEntryId());
        assertEquals(List.of("§bSam : legit middleman"), context.capturedMessages());
        assertEquals("LevenshteinStage", context.ruleDetails().getFirst().source());
        assertEquals("Similarity match: \"middleman\"", context.ruleDetails().getFirst().detail());
    }
}
