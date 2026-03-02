package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.pipeline.state.FunnelStore;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunnelStageTest {
    @Test
    void scoresExternalPlatformAfterPriorContact() {
        UUID senderUuid = UUID.randomUUID();
        FunnelStage stage = new FunnelStage(new FunnelStore());

        StageResult first = stage.apply(new ChatEvent(
            "hello there",
            senderUuid,
            "Alpha",
            1_000L,
            ChatSourceType.PLAYER
        ));
        StageResult second = stage.apply(new ChatEvent(
            "add me on discord",
            senderUuid,
            "Alpha",
            2_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, first.getDecision());
        assertEquals(0, first.getScoreDelta());
        assertEquals(Stage.Decision.PASS, second.getDecision());
        assertEquals(8, second.getScoreDelta());
        assertTrue(second.getReason().contains("external platform after prior contact"));
    }

    @Test
    void scoresTrustToExternalToPaymentChain() {
        UUID senderUuid = UUID.randomUUID();
        FunnelStage stage = new FunnelStage(new FunnelStore());

        stage.apply(new ChatEvent("trust me", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));
        stage.apply(new ChatEvent("add me on discord", senderUuid, "Alpha", 2_000L, ChatSourceType.PLAYER));

        StageResult result = stage.apply(new ChatEvent(
            "pay first",
            senderUuid,
            "Alpha",
            3_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(28, result.getScoreDelta());
        assertTrue(result.getReason().contains("payment request after external platform"));
        assertTrue(result.getReason().contains("trust -> external platform -> request"));
    }

    @Test
    void ignoresSystemMessagesWithoutSenderContext() {
        StageResult result = new FunnelStage(new FunnelStore()).apply(
            ChatEvent.messageOnly("[NPC] Kat: Your Ocelot is ready!", ChatSourceType.SYSTEM)
        );

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(0, result.getScoreDelta());
        assertFalse(result.hasReason());
    }

    @Test
    void usesConfiguredFunnelScores() {
        UUID senderUuid = UUID.randomUUID();
        RulesConfig rulesConfig = new RulesConfig();
        rulesConfig.funnelStage().setExternalAfterContactScore(3);
        FunnelStage stage = new FunnelStage(new FunnelStore(), rulesConfig);

        stage.apply(new ChatEvent("hello there", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));
        StageResult result = stage.apply(new ChatEvent(
            "add me on discord",
            senderUuid,
            "Alpha",
            2_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(3, result.getScoreDelta());
    }

    @Test
    void usesConfiguredRuleStagePatternsForFunnelClassification() {
        UUID senderUuid = UUID.randomUUID();
        RulesConfig rulesConfig = new RulesConfig();
        rulesConfig.ruleStage().setExternalPlatformPattern("\\b(?:signal)\\b");
        FunnelStage stage = new FunnelStage(new FunnelStore(), rulesConfig);

        stage.apply(new ChatEvent("hello there", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));
        StageResult result = stage.apply(new ChatEvent(
            "signal",
            senderUuid,
            "Alpha",
            2_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(8, result.getScoreDelta());
        assertTrue(result.getReason().contains("external platform after prior contact"));
    }

    @Test
    void addsTrustBridgeBonusWhenExternalStepFollowsTrust() {
        UUID senderUuid = UUID.randomUUID();
        FunnelStage stage = new FunnelStage(new FunnelStore());

        stage.apply(new ChatEvent("trust me", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));
        StageResult result = stage.apply(new ChatEvent(
            "add me on discord",
            senderUuid,
            "Alpha",
            2_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(12, result.getScoreDelta());
        assertTrue(result.getReason().contains("external platform after trust framing"));
    }
}
