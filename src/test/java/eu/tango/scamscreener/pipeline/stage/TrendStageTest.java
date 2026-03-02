package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.pipeline.state.TrendStore;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrendStageTest {
    @Test
    void scoresSingleCrossSenderRepeat() {
        TrendStage stage = new TrendStage(new TrendStore());

        StageResult first = stage.apply(new ChatEvent(
            "add me on discord",
            UUID.randomUUID(),
            "Alpha",
            1_000L,
            ChatSourceType.PLAYER
        ));
        StageResult second = stage.apply(new ChatEvent(
            "add me on discord",
            UUID.randomUUID(),
            "Beta",
            2_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, first.getDecision());
        assertEquals(0, first.getScoreDelta());
        assertEquals(Stage.Decision.PASS, second.getDecision());
        assertEquals(10, second.getScoreDelta());
        assertTrue(second.getReason().contains("Cross-sender repeat"));
    }

    @Test
    void scoresMultiSenderWaveMoreStrongly() {
        TrendStage stage = new TrendStage(new TrendStore());

        stage.apply(new ChatEvent("same pitch", UUID.randomUUID(), "Alpha", 1_000L, ChatSourceType.PLAYER));
        stage.apply(new ChatEvent("same pitch", UUID.randomUUID(), "Beta", 2_000L, ChatSourceType.PLAYER));

        StageResult third = stage.apply(new ChatEvent(
            "same pitch",
            UUID.randomUUID(),
            "Gamma",
            3_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, third.getDecision());
        assertEquals(20, third.getScoreDelta());
        assertTrue(third.getReason().contains("Trend wave"));
    }

    @Test
    void ignoresBenignFirstMessage() {
        StageResult result = new TrendStage(new TrendStore()).apply(ChatEvent.messageOnly("hello there", ChatSourceType.PLAYER));

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(0, result.getScoreDelta());
        assertFalse(result.hasReason());
    }

    @Test
    void usesConfiguredTrendScores() {
        RulesConfig rulesConfig = new RulesConfig();
        rulesConfig.trendStage().setSingleSenderRepeatScore(4);
        TrendStage stage = new TrendStage(new TrendStore(), rulesConfig);

        stage.apply(new ChatEvent("same pitch", UUID.randomUUID(), "Alpha", 1_000L, ChatSourceType.PLAYER));
        StageResult result = stage.apply(new ChatEvent(
            "same pitch",
            UUID.randomUUID(),
            "Beta",
            2_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(4, result.getScoreDelta());
    }

    @Test
    void addsEscalationBonusForLargerTrendWaves() {
        TrendStage stage = new TrendStage(new TrendStore());

        stage.apply(new ChatEvent("same pitch", UUID.randomUUID(), "Alpha", 1_000L, ChatSourceType.PLAYER));
        stage.apply(new ChatEvent("same pitch", UUID.randomUUID(), "Beta", 2_000L, ChatSourceType.PLAYER));
        stage.apply(new ChatEvent("same pitch", UUID.randomUUID(), "Gamma", 3_000L, ChatSourceType.PLAYER));

        StageResult fourth = stage.apply(new ChatEvent(
            "same pitch",
            UUID.randomUUID(),
            "Delta",
            4_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, fourth.getDecision());
        assertEquals(25, fourth.getScoreDelta());
        assertTrue(fourth.getReason().contains("Trend escalation"));
    }

    @Test
    void groupsNearIdenticalMessagesByFingerprint() {
        TrendStage stage = new TrendStage(new TrendStore());

        stage.apply(new ChatEvent("add me on disc0rd", UUID.randomUUID(), "Alpha", 1_000L, ChatSourceType.PLAYER));
        StageResult result = stage.apply(new ChatEvent(
            "add me on discord",
            UUID.randomUUID(),
            "Beta",
            2_000L,
            ChatSourceType.PLAYER
        ));

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(10, result.getScoreDelta());
        assertTrue(result.getReason().contains("Cross-sender repeat"));
    }
}
