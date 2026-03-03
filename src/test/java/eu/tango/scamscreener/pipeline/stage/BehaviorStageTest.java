package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.pipeline.state.BehaviorStore;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorStageTest {
    @Test
    void scoresRepeatedMessagesFromSameSender() {
        UUID senderUuid = UUID.randomUUID();
        BehaviorStage stage = new BehaviorStage(new BehaviorStore());

        StageResult first = stage.apply(new ChatEvent(
            "Buy my free rank now",
            senderUuid,
            "Alpha",
            1_000L,
            ChatSourceType.PLAYER
        ));
        StageResult second = stage.apply(new ChatEvent(
            "Buy my free rank now",
            senderUuid,
            "Alpha",
            2_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, first.getDecision());
        assertEquals(0, first.getScoreDelta());
        assertEquals(Stage.Decision.PASS, second.getDecision());
        assertEquals(1, second.getScoreDelta());
        assertTrue(second.getReason().contains("Repeated contact message x2"));
    }

    @Test
    void scoresBurstContactAfterSeveralRecentMessages() {
        UUID senderUuid = UUID.randomUUID();
        BehaviorStage stage = new BehaviorStage(new BehaviorStore());

        stage.apply(new ChatEvent("message one", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));
        stage.apply(new ChatEvent("message two", senderUuid, "Alpha", 2_000L, ChatSourceType.PLAYER));
        stage.apply(new ChatEvent("message three", senderUuid, "Alpha", 3_000L, ChatSourceType.PLAYER));

        StageResult result = stage.apply(new ChatEvent(
            "message four",
            senderUuid,
            "Alpha",
            4_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(1, result.getScoreDelta());
        assertTrue(result.getReason().contains("Burst contact: 4 messages in short window"));
    }

    @Test
    void ignoresSystemMessagesWithoutSenderContext() {
        BehaviorStage stage = new BehaviorStage(new BehaviorStore());

        StageResult result = stage.apply(ChatEvent.messageOnly("[NPC] Kat: Your Ocelot is ready!", ChatSourceType.SYSTEM));

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(0, result.getScoreDelta());
        assertFalse(result.hasReason());
    }

    @Test
    void capsConfiguredBehaviorScoresAtOnePoint() {
        UUID senderUuid = UUID.randomUUID();
        RulesConfig rulesConfig = new RulesConfig();
        rulesConfig.behaviorStage().setRepeatedMessageScore(30);
        rulesConfig.behaviorStage().setBurstContactScore(0);
        BehaviorStage stage = new BehaviorStage(new BehaviorStore(), rulesConfig);

        stage.apply(new ChatEvent("Buy my free rank now", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));
        StageResult result = stage.apply(new ChatEvent(
            "Buy my free rank now",
            senderUuid,
            "Alpha",
            2_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(1, result.getScoreDelta());
    }

    @Test
    void addsComboBonusWhenRepeatedBurstAlign() {
        UUID senderUuid = UUID.randomUUID();
        BehaviorStage stage = new BehaviorStage(new BehaviorStore());

        stage.apply(new ChatEvent("same pitch now", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));
        stage.apply(new ChatEvent("same pitch now", senderUuid, "Alpha", 2_000L, ChatSourceType.PLAYER));
        stage.apply(new ChatEvent("same pitch now", senderUuid, "Alpha", 3_000L, ChatSourceType.PLAYER));

        StageResult result = stage.apply(new ChatEvent(
            "same pitch now",
            senderUuid,
            "Alpha",
            4_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(1, result.getScoreDelta());
        assertTrue(result.getReason().contains("Behavior combo"));
    }
}
