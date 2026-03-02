package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import eu.tango.scamscreener.pipeline.data.StageResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevenshteinStageTest {
    @Test
    void scoresFuzzyMatchesInsideLongerMessages() {
        ChatEvent event = ChatEvent.messageOnly(
            "If you want, pleaze add me on disc0rd later.",
            ChatSourceType.PLAYER
        );

        StageResult result = new LevenshteinStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(10, result.getScoreDelta());
        assertTrue(result.getReason().contains("SIM_EXTERNAL_PLATFORM"));
        assertTrue(result.getReason().contains("add me on discord"));
    }

    @Test
    void accumulatesBestMatchesAcrossCategories() {
        ChatEvent event = ChatEvent.messageOnly(
            "Pleaze add me on disc0rd for trusted middleman",
            ChatSourceType.PLAYER
        );

        StageResult result = new LevenshteinStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(20, result.getScoreDelta());
        assertTrue(result.getReason().contains("SIM_EXTERNAL_PLATFORM"));
        assertTrue(result.getReason().contains("SIM_TRUST_MANIPULATION"));
    }

    @Test
    void ignoresShortOrBenignMessages() {
        StageResult shortResult = new LevenshteinStage().apply(ChatEvent.messageOnly("ok", ChatSourceType.PLAYER));
        StageResult benignResult = new LevenshteinStage().apply(ChatEvent.messageOnly("Ich spiele Hypixel Skyblock", ChatSourceType.PLAYER));

        assertEquals(0, shortResult.getScoreDelta());
        assertFalse(shortResult.hasReason());
        assertEquals(0, benignResult.getScoreDelta());
        assertFalse(benignResult.hasReason());
    }

    @Test
    void normalizesCommonLeetspeakForSimilarityMatching() {
        ChatEvent event = ChatEvent.messageOnly("fr33 c01ns for you", ChatSourceType.PLAYER);

        StageResult result = new LevenshteinStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertTrue(result.getScoreDelta() >= 8);
        assertTrue(result.getReason().contains("SIM_TOO_GOOD"));
    }
}
