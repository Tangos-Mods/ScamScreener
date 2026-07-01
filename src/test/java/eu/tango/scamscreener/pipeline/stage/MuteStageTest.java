package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.core.PipelineEngine;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.pipeline.data.StageResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuteStageTest {
    @Test
    void systemMessagesBypassRiskChecksInsteadOfMuting() {
        AtomicBoolean nextStageCalled = new AtomicBoolean(false);
        PipelineEngine engine = new PipelineEngine(java.util.List.of(
            new MuteStage(),
            new TrackingStage(nextStageCalled)
        ));

        PipelineDecision decision = engine.evaluate(ChatEvent.messageOnly("[NPC] Kat: Your Ocelot is ready!", ChatSourceType.SYSTEM));

        assertEquals(PipelineDecision.Outcome.ALLOW, decision.getOutcome());
        assertEquals("MuteStage", decision.getDecidedByStage());
        assertEquals(java.util.List.of("MUTE_SYSTEM_BYPASS"), decision.getReasons());
        assertFalse(nextStageCalled.get());
    }

    @Test
    void playerMessagesContinuePastMuteStage() {
        StageResult result = new MuteStage().apply(ChatEvent.messageOnly("hello there", ChatSourceType.PLAYER));

        assertEquals(Stage.Decision.PASS, result.getDecision());
        assertEquals(0, result.getScoreDelta());
        assertEquals("", result.getReason());
    }

    @Test
    void harmlessShortPlayerMessagesBypassRiskChecks() {
        StageResult result = new MuteStage().apply(ChatEvent.messageOnly("gg", ChatSourceType.PLAYER));

        assertEquals(Stage.Decision.ALLOW, result.getDecision());
        assertEquals(0, result.getScoreDelta());
        assertEquals("MUTE_NOISE_BYPASS", result.getReason());
    }

    @Test
    void repeatedUnknownSourceMessagesTriggerDuplicateBypass() {
        MuteStage stage = new MuteStage();

        StageResult first = stage.apply(ChatEvent.messageOnly("Server restart soon", ChatSourceType.UNKNOWN));
        StageResult second = stage.apply(ChatEvent.messageOnly("Server restart soon", ChatSourceType.UNKNOWN));

        assertEquals(Stage.Decision.PASS, first.getDecision());
        assertEquals(Stage.Decision.ALLOW, second.getDecision());
        assertEquals("MUTE_DUPLICATE_BYPASS", second.getReason());
    }

    @Test
    void knownDungeonModPlayerMessagesBypassRiskChecks() {
        MuteStage stage = new MuteStage();

        for (String message : java.util.List.of(
            "[Skyblocker] 300 Score Reached!",
            "[Skyblocker] 270 Score Reached!",
            "Prince dead!",
            "Mimic dead!",
            "[Skyblocker] We only have 0 crypts out of 5, we need more!",
            "[Skyblocker] We only have 1 crypts out of 5, we need more!",
            "[Skyblocker] We only have 2 crypts out of 5, we need more!",
            "[Skyblocker] We only have 3 crypts out of 5, we need more!",
            "[Skyblocker] We only have 4 crypts out of 5, we need more!"
        )) {
            StageResult result = stage.apply(ChatEvent.messageOnly(message, ChatSourceType.PLAYER));

            assertEquals(Stage.Decision.ALLOW, result.getDecision(), message);
            assertEquals("MUTE_DUNGEON_MOD_BYPASS", result.getReason(), message);
        }
    }

    @Test
    void scrubbedSkyblockerPlayerMessagesBypassRiskChecks() {
        StageResult result = new MuteStage().apply(
            ChatEvent.messageOnly("[Skyblocker] We only have 5 crypts out of 5, we need more!", ChatSourceType.PLAYER)
        );

        assertEquals(Stage.Decision.ALLOW, result.getDecision());
        assertEquals("MUTE_DUNGEON_MOD_BYPASS", result.getReason());
    }

    private static final class TrackingStage extends Stage {
        private final AtomicBoolean called;

        private TrackingStage(AtomicBoolean called) {
            this.called = called;
        }

        @Override
        protected StageResult evaluate(ChatEvent chatEvent) {
            called.set(true);
            return score(999, "should-not-run");
        }
    }
}
