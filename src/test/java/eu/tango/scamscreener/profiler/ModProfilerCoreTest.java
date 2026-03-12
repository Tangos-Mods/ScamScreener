package eu.tango.scamscreener.profiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModProfilerCoreTest {
    @Test
    void snapshotKeepsLastActiveTickVisibleAfterIdleTick() {
        ModProfilerCore core = new ModProfilerCore(8);

        core.startTick(1_000_000L);
        core.recordPhase("pipeline.total", "Pipeline", 2_000_000L, 3_000_000L);
        core.recordEventSummary("Alice -> REVIEW (10)", 3_000_000L);
        core.endTick(4_000_000L);

        core.startTick(5_000_000L);
        core.endTick(6_000_000L);

        ModProfilerCore.Snapshot snapshot = core.snapshot(6_000_000L, 8);
        assertTrue(snapshot.hasData());
        assertEquals(2_000_000L, snapshot.lastTickNanos());
        assertEquals("Alice -> REVIEW (10)", snapshot.eventSummary());
        assertEquals(1, snapshot.activeTickCount());
        assertEquals(1, snapshot.phases().size());
        assertEquals("Pipeline", snapshot.phases().get(0).label());
    }

    @Test
    void snapshotAggregatesPhaseDurationsAcrossSamples() {
        ModProfilerCore core = new ModProfilerCore(8);

        core.startTick(1_000_000L);
        core.recordPhase("pipeline.total", "Pipeline", 1_000_000L, 2_000_000L);
        core.recordPhase("decision.messages", "  Decision Messages", 500_000L, 2_500_000L);
        core.endTick(3_000_000L);

        core.startTick(4_000_000L);
        core.recordPhase("pipeline.total", "Pipeline", 3_000_000L, 7_000_000L);
        core.endTick(7_500_000L);

        ModProfilerCore.Snapshot snapshot = core.snapshot(8_000_000L, 8);
        assertTrue(snapshot.hasData());
        assertEquals(2, snapshot.activeTickCount());
        assertEquals(3_000_000L, snapshot.lastTickNanos());
        assertEquals(3_000_000L, snapshot.maxTickNanos());

        ModProfilerCore.PhaseView pipelinePhase = snapshot.phases().stream()
            .filter(phase -> "pipeline.total".equals(phase.key()))
            .findFirst()
            .orElseThrow();
        assertEquals(3_000_000L, pipelinePhase.lastNanos());
        assertEquals(2_000_000L, pipelinePhase.averageNanos());
        assertEquals(3_000_000L, pipelinePhase.maxNanos());
        assertEquals(2, pipelinePhase.samples());
        assertEquals(2_000_000L, pipelinePhase.lifetimeAverageNanos());
        assertEquals(3_000_000L, pipelinePhase.lifetimeMaxNanos());
        assertEquals(2, pipelinePhase.lifetimeSamples());

        ModProfilerCore.PhaseView decisionPhase = snapshot.phases().stream()
            .filter(phase -> "decision.messages".equals(phase.key()))
            .findFirst()
            .orElseThrow();
        assertEquals(0L, decisionPhase.lastNanos());
        assertEquals(500_000L, decisionPhase.averageNanos());
        assertEquals(500_000L, decisionPhase.maxNanos());
        assertEquals(1, decisionPhase.samples());
        assertEquals(500_000L, decisionPhase.lifetimeAverageNanos());
        assertEquals(500_000L, decisionPhase.lifetimeMaxNanos());
        assertEquals(1, decisionPhase.lifetimeSamples());
    }

    @Test
    void snapshotTracksTickDurationsSeparatelyFromMeasuredWork() {
        ModProfilerCore core = new ModProfilerCore(8);

        core.startTick(1_000_000L);
        core.recordPhase("pipeline.total", "Pipeline", 1_500_000L, 3_000_000L);
        core.endTick(6_000_000L);

        core.startTick(10_000_000L);
        core.recordPhase("pipeline.total", "Pipeline", 2_000_000L, 13_000_000L);
        core.endTick(14_000_000L);

        ModProfilerCore.Snapshot snapshot = core.snapshot(14_000_000L, 8);
        assertEquals(2_000_000L, snapshot.lastTickNanos());
        assertEquals(4_000_000L, snapshot.lastTickDurationNanos());
        assertEquals(4_500_000L, snapshot.averageTickDurationNanos());
        assertEquals(5_000_000L, snapshot.maxTickDurationNanos());
        assertEquals(1_750_000L, snapshot.lifetimeAverageTickNanos());
        assertEquals(4_500_000L, snapshot.lifetimeAverageTickDurationNanos());
        assertEquals(2, snapshot.lifetimeActiveTickCount());
    }

    @Test
    void emptySnapshotStaysEmptyWithoutSamples() {
        ModProfilerCore core = new ModProfilerCore(8);

        ModProfilerCore.Snapshot snapshot = core.snapshot(1_000_000L, 8);
        assertFalse(snapshot.hasData());
        assertEquals(0, snapshot.activeTickCount());
        assertTrue(snapshot.phases().isEmpty());
    }

    @Test
    void resetClearsRetainedHistoryAndLifetimeAverages() {
        ModProfilerCore core = new ModProfilerCore(8);

        core.startTick(1_000_000L);
        core.recordPhase("pipeline.total", "Pipeline", 2_000_000L, 3_000_000L);
        core.recordEventSummary("Alice -> REVIEW (10)", 3_000_000L);
        core.endTick(5_000_000L);

        assertTrue(core.snapshot(5_000_000L, 8).hasData());

        core.reset();

        ModProfilerCore.Snapshot snapshot = core.snapshot(6_000_000L, 8);
        assertFalse(snapshot.hasData());
        assertEquals(0, snapshot.activeTickCount());
        assertEquals(0, snapshot.lifetimeActiveTickCount());
        assertEquals(0L, snapshot.lifetimeAverageTickNanos());
        assertEquals(0L, snapshot.lifetimeAverageTickDurationNanos());
        assertTrue(snapshot.phases().isEmpty());
    }

    @Test
    void snapshotKeepsLifetimePhaseRowsAfterRollingWindowDropsOldSamples() {
        ModProfilerCore core = new ModProfilerCore(1);

        core.startTick(1_000_000L);
        core.recordPhase("phase.alpha", "Alpha", 1_000_000L, 2_000_000L);
        core.endTick(3_000_000L);

        core.startTick(4_000_000L);
        core.recordPhase("phase.beta", "Beta", 2_000_000L, 6_000_000L);
        core.endTick(7_000_000L);

        ModProfilerCore.Snapshot snapshot = core.snapshot(8_000_000L, 8);

        ModProfilerCore.PhaseView alphaPhase = snapshot.phases().stream()
            .filter(phase -> "phase.alpha".equals(phase.key()))
            .findFirst()
            .orElseThrow();
        assertEquals(0L, alphaPhase.lastNanos());
        assertEquals(0L, alphaPhase.averageNanos());
        assertEquals(0, alphaPhase.samples());
        assertEquals(1_000_000L, alphaPhase.lifetimeAverageNanos());
        assertEquals(1, alphaPhase.lifetimeSamples());
    }
}
