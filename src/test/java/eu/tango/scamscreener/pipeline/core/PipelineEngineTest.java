package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.pipeline.data.StageResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipelineEngineTest {
    @Test
    void evaluatesStagesInOrderAndAccumulatesScore() {
        List<String> callOrder = new ArrayList<>();
        PipelineEngine engine = new PipelineEngine(List.of(
            new RecordingStage("MuteStage", callOrder, (name, message) -> StageResult.score(name, 2, "mute-check")),
            new RecordingStage("RuleStage", callOrder, (name, message) -> StageResult.score(name, 3, "rule-hit")),
            new RecordingStage("ModelStage", callOrder, (name, message) -> StageResult.pass(name))
        ), 10);

        PipelineDecision decision = engine.evaluate("hello world");

        assertEquals(List.of("MuteStage", "RuleStage", "ModelStage"), callOrder);
        assertEquals(PipelineDecision.Outcome.IGNORE, decision.getOutcome());
        assertEquals(5, decision.getTotalScore());
        assertEquals(3, decision.getStageResults().size());
        assertEquals(List.of("mute-check", "rule-hit"), decision.getReasons());
        assertTrue(decision.getDecidedByStage().isEmpty());
    }

    @Test
    void returnsReviewWhenScoreReachesThreshold() {
        PipelineEngine engine = new PipelineEngine(List.of(
            new RecordingStage("RuleStage", null, (name, message) -> StageResult.score(name, 5, "rule-hit"))
        ), 5);

        PipelineDecision decision = engine.evaluate("hello world");

        assertEquals(PipelineDecision.Outcome.REVIEW, decision.getOutcome());
        assertEquals(5, decision.getTotalScore());
        assertEquals(List.of("rule-hit"), decision.getReasons());
    }

    @Test
    void stopsImmediatelyWhenStageMutes() {
        List<String> callOrder = new ArrayList<>();
        PipelineEngine engine = new PipelineEngine(List.of(
            new RecordingStage("MuteStage", callOrder, (name, message) -> StageResult.mute(name, "muted")),
            new RecordingStage("RuleStage", callOrder, (name, message) -> StageResult.score(name, 50, "should-not-run"))
        ));

        PipelineDecision decision = engine.evaluate("hello world");

        assertEquals(List.of("MuteStage"), callOrder);
        assertEquals(PipelineDecision.Outcome.MUTED, decision.getOutcome());
        assertEquals("MuteStage", decision.getDecidedByStage());
        assertEquals(0, decision.getTotalScore());
        assertEquals(List.of("muted"), decision.getReasons());
    }

    @Test
    void stopsImmediatelyWhenStageWhitelists() {
        List<String> callOrder = new ArrayList<>();
        PipelineEngine engine = new PipelineEngine(List.of(
            new RecordingStage("PlayerListStage", callOrder, (name, message) -> StageResult.whitelist(name, "trusted")),
            new RecordingStage("RuleStage", callOrder, (name, message) -> StageResult.block(name, 100, "should-not-run"))
        ));

        PipelineDecision decision = engine.evaluate("hello world");

        assertEquals(List.of("PlayerListStage"), callOrder);
        assertEquals(PipelineDecision.Outcome.WHITELISTED, decision.getOutcome());
        assertEquals("PlayerListStage", decision.getDecidedByStage());
        assertEquals(List.of("trusted"), decision.getReasons());
    }

    @Test
    void stopsImmediatelyWhenStageBlacklists() {
        List<String> callOrder = new ArrayList<>();
        PipelineEngine engine = new PipelineEngine(List.of(
            new RecordingStage("PlayerListStage", callOrder, (name, message) -> StageResult.blacklist(name, "blocked-player")),
            new RecordingStage("RuleStage", callOrder, (name, message) -> StageResult.score(name, 50, "should-not-run"))
        ));

        PipelineDecision decision = engine.evaluate("hello world");

        assertEquals(List.of("PlayerListStage"), callOrder);
        assertEquals(PipelineDecision.Outcome.BLACKLISTED, decision.getOutcome());
        assertEquals("PlayerListStage", decision.getDecidedByStage());
        assertEquals(0, decision.getTotalScore());
        assertEquals(List.of("blocked-player"), decision.getReasons());
    }

    @Test
    void stopsImmediatelyWhenStageBlocks() {
        List<String> callOrder = new ArrayList<>();
        PipelineEngine engine = new PipelineEngine(List.of(
            new RecordingStage("RuleStage", callOrder, (name, message) -> StageResult.score(name, 20, "rule-hit")),
            new RecordingStage("BehaviorStage", callOrder, (name, message) -> StageResult.block(name, 40, "behavior-block")),
            new RecordingStage("ModelStage", callOrder, (name, message) -> StageResult.score(name, 10, "should-not-run"))
        ), 999);

        PipelineDecision decision = engine.evaluate("hello world");

        assertEquals(List.of("RuleStage", "BehaviorStage"), callOrder);
        assertEquals(PipelineDecision.Outcome.BLOCK, decision.getOutcome());
        assertEquals("BehaviorStage", decision.getDecidedByStage());
        assertEquals(60, decision.getTotalScore());
        assertEquals(List.of("rule-hit", "behavior-block"), decision.getReasons());
    }

    @FunctionalInterface
    private interface ResultFactory {
        StageResult create(String stageName, String message);
    }

    private static final class RecordingStage extends Stage {
        private final String stageName;
        private final List<String> callOrder;
        private final ResultFactory resultFactory;

        private RecordingStage(String stageName, List<String> callOrder, ResultFactory resultFactory) {
            this.stageName = stageName;
            this.callOrder = callOrder;
            this.resultFactory = resultFactory;
        }

        @Override
        public String name() {
            return stageName;
        }

        @Override
        protected StageResult evaluate(ChatEvent chatEvent) {
            if (callOrder != null) {
                // Record stage execution so the engine order can be asserted.
                callOrder.add(stageName);
            }

            return resultFactory.create(stageName, chatEvent == null ? "" : chatEvent.getRawMessage());
        }
    }
}
