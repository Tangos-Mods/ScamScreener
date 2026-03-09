package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import eu.tango.scamscreener.pipeline.data.StageResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleStageTest {
    @Test
    void scoresObviousRiskPatterns() {
        ChatEvent event = ChatEvent.messageOnly(
            "Add me on Discord test#1234 and pay first for free rank right now",
            ChatSourceType.PLAYER
        );

        StageResult result = new RuleStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(115, result.getScoreDelta());
        assertTrue(result.getReason().contains("External platform push"));
        assertTrue(result.getReason().contains("Upfront payment wording"));
        assertTrue(result.getReason().contains("Too-good-to-be-true wording"));
        assertTrue(result.getReason().contains("Urgency wording"));
        assertTrue(result.getReason().contains("Discord handle with platform mention"));
    }

    @Test
    void ignoresPlainUrgencyWithoutOtherRiskSignals() {
        ChatEvent event = ChatEvent.messageOnly("I need this right now please", ChatSourceType.PLAYER);

        StageResult result = new RuleStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(0, result.getScoreDelta());
        assertFalse(result.hasReason());
    }

    @Test
    void ignoresBareCallAndUrgencyWithoutOtherRiskSignals() {
        ChatEvent event = ChatEvent.messageOnly("can you call me right now", ChatSourceType.PLAYER);

        StageResult result = new RuleStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(0, result.getScoreDelta());
        assertFalse(result.hasReason());
    }

    @Test
    void suppressesTradeUrgencyWithoutSuspiciousContext() {
        ChatEvent event = ChatEvent.messageOnly("quick payment please", ChatSourceType.PLAYER);

        StageResult result = new RuleStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(0, result.getScoreDelta());
        assertFalse(result.hasReason());
    }

    @Test
    void keepsUrgencyWhenTradeContextAlsoHasSuspiciousSignals() {
        ChatEvent event = ChatEvent.messageOnly("add me on discord for quick payment", ChatSourceType.PLAYER);

        StageResult result = new RuleStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(25, result.getScoreDelta());
        assertTrue(result.getReason().contains("External platform push"));
        assertTrue(result.getReason().contains("Urgency wording"));
    }

    @Test
    void scoresTrustWithoutOtherSuspiciousContext() {
        ChatEvent event = ChatEvent.messageOnly("trust me i am legit", ChatSourceType.PLAYER);

        StageResult result = new RuleStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(10, result.getScoreDelta());
        assertTrue(result.getReason().contains("Trust manipulation wording"));
    }

    @Test
    void scoresCoercionThreats() {
        ChatEvent event = ChatEvent.messageOnly("you will not get your stuff back", ChatSourceType.PLAYER);

        StageResult result = new RuleStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(20, result.getScoreDelta());
        assertTrue(result.getReason().contains("Coercion or extortion wording"));
    }

    @Test
    void scoresLinkRedirectCombo() {
        ChatEvent event = ChatEvent.messageOnly(
            "join my discord https://discord.gg/test",
            ChatSourceType.PLAYER
        );

        StageResult result = new RuleStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(45, result.getScoreDelta());
        assertTrue(result.getReason().contains("Suspicious link"));
        assertTrue(result.getReason().contains("External platform push"));
        assertTrue(result.getReason().contains("Link plus off-platform redirect"));
    }

    @Test
    void scoresTrustPaymentCombo() {
        ChatEvent event = ChatEvent.messageOnly("trust me and pay first", ChatSourceType.PLAYER);

        StageResult result = new RuleStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(50, result.getScoreDelta());
        assertTrue(result.getReason().contains("Trust manipulation wording"));
        assertTrue(result.getReason().contains("Upfront payment wording"));
        assertTrue(result.getReason().contains("Trust framing plus upfront payment"));
    }

    @Test
    void scoresUrgencyAccountCombo() {
        ChatEvent event = ChatEvent.messageOnly(
            "need it right now send your verification code",
            ChatSourceType.PLAYER
        );

        StageResult result = new RuleStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(60, result.getScoreDelta());
        assertTrue(result.getReason().contains("Urgency wording"));
        assertTrue(result.getReason().contains("Sensitive account wording"));
        assertTrue(result.getReason().contains("Urgency paired with sensitive account request"));
    }

    @Test
    void scoresMiddlemanProofCombo() {
        ChatEvent event = ChatEvent.messageOnly("middleman here i have proof", ChatSourceType.PLAYER);

        StageResult result = new RuleStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(35, result.getScoreDelta());
        assertTrue(result.getReason().contains("Middleman claim"));
        assertTrue(result.getReason().contains("Proof or vouch bait"));
        assertTrue(result.getReason().contains("Middleman claim plus proof bait"));
    }

    @Test
    void ignoresBenignPlayerChat() {
        ChatEvent event = ChatEvent.messageOnly("Ich spiele Hypixel Skyblock", ChatSourceType.PLAYER);

        StageResult result = new RuleStage().apply(event);

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(0, result.getScoreDelta());
        assertFalse(result.hasReason());
    }
}
