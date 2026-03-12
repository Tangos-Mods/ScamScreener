package eu.tango.scamscreener.profiler.web;

import com.google.gson.Gson;
import eu.tango.scamscreener.profiler.ModProfilerCore;
import eu.tango.scamscreener.profiler.ScamScreenerProfiler;

import java.util.ArrayList;
import java.util.List;

final class ProfilerWebPayload {
    private static final Gson GSON = new Gson();
    private static final int MAX_WEB_PHASES = 24;
    private static final int MAX_WEB_EVENTS = 24;
    private static final double NORMAL_MSPT = 50.0D;
    private static final double NORMAL_TPS = 20.0D;

    private ProfilerWebPayload() {
    }

    static String snapshotJson() {
        ScamScreenerProfiler profiler = ScamScreenerProfiler.getInstance();
        ModProfilerCore.Snapshot snapshot = profiler.snapshot(MAX_WEB_PHASES);
        long lastTickNanos = Math.max(0L, snapshot.lastTickNanos());
        long lifetimeAverageTickNanos = Math.max(0L, snapshot.lifetimeAverageTickNanos());
        List<PhasePayload> phases = new ArrayList<>(snapshot.phases().size());
        for (ModProfilerCore.PhaseView phase : snapshot.phases()) {
            phases.add(new PhasePayload(
                phase.key(),
                phase.label(),
                phase.samples(),
                phase.lastNanos(),
                nanosToMillis(phase.lastNanos()),
                phase.averageNanos(),
                nanosToMillis(phase.averageNanos()),
                phase.maxNanos(),
                nanosToMillis(phase.maxNanos()),
                phase.lifetimeAverageNanos(),
                nanosToMillis(phase.lifetimeAverageNanos()),
                phase.lifetimeMaxNanos(),
                nanosToMillis(phase.lifetimeMaxNanos()),
                phase.lifetimeSamples(),
                lastTickNanos <= 0L ? 0.0D : Math.min(1.0D, phase.lastNanos() / (double) lastTickNanos),
                lifetimeAverageTickNanos <= 0L ? 0.0D : Math.min(1.0D, phase.lifetimeAverageNanos() / (double) lifetimeAverageTickNanos)
            ));
        }

        List<EventPayload> events = new ArrayList<>(profiler.recentEvents(MAX_WEB_EVENTS).size());
        for (ScamScreenerProfiler.EventView event : profiler.recentEvents(MAX_WEB_EVENTS)) {
            List<EventPhasePayload> eventPhases = new ArrayList<>(event.phases().size());
            for (ScamScreenerProfiler.EventPhaseView phase : event.phases()) {
                eventPhases.add(new EventPhasePayload(phase.key(), phase.label(), phase.millis()));
            }
            events.add(new EventPayload(
                event.capturedAtMillis(),
                event.sender(),
                event.message(),
                event.summary(),
                event.outcome(),
                event.score(),
                event.decidedBy(),
                event.measuredMillis(),
                event.tickMillis(),
                event.tps(),
                eventPhases
            ));
        }

        double currentMspt = nanosToMillis(snapshot.lastTickDurationNanos());
        double averageMspt = nanosToMillis(snapshot.averageTickDurationNanos());
        double maxMspt = nanosToMillis(snapshot.maxTickDurationNanos());
        double lifetimeModAverageMs = nanosToMillis(snapshot.lifetimeAverageTickNanos());
        double lifetimeMspt = nanosToMillis(snapshot.lifetimeAverageTickDurationNanos());
        return GSON.toJson(new SnapshotPayload(
            System.currentTimeMillis(),
            snapshot.hasData(),
            profiler.isEnabled(),
            profiler.isRecordingEnabled(),
            snapshot.activeTickCount(),
            snapshot.lastTickNanos(),
            nanosToMillis(snapshot.lastTickNanos()),
            snapshot.averageTickNanos(),
            nanosToMillis(snapshot.averageTickNanos()),
            snapshot.maxTickNanos(),
            nanosToMillis(snapshot.maxTickNanos()),
            snapshot.lastTickDurationNanos(),
            currentMspt,
            snapshot.averageTickDurationNanos(),
            averageMspt,
            snapshot.maxTickDurationNanos(),
            maxMspt,
            msptToTps(currentMspt),
            msptToTps(averageMspt),
            NORMAL_MSPT,
            NORMAL_TPS,
            snapshot.lifetimeAverageTickNanos(),
            lifetimeModAverageMs,
            snapshot.lifetimeAverageTickDurationNanos(),
            lifetimeMspt,
            msptToTps(lifetimeMspt),
            snapshot.lifetimeActiveTickCount(),
            snapshot.eventSummary() == null ? "" : snapshot.eventSummary(),
            phases,
            events
        ));
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0D;
    }

    private static double msptToTps(double mspt) {
        if (mspt <= 0.0D) {
            return NORMAL_TPS;
        }

        return Math.min(NORMAL_TPS, 1000.0D / mspt);
    }

    private record SnapshotPayload(
        long capturedAtMillis,
        boolean hasData,
        boolean hudEnabled,
        boolean recordingEnabled,
        int activeTickCount,
        long lastTickNanos,
        double lastTickMillis,
        long averageTickNanos,
        double averageTickMillis,
        long maxTickNanos,
        double maxTickMillis,
        long lastTickDurationNanos,
        double lastTickDurationMillis,
        long averageTickDurationNanos,
        double averageTickDurationMillis,
        long maxTickDurationNanos,
        double maxTickDurationMillis,
        double currentTps,
        double averageTps,
        double normalMspt,
        double normalTps,
        long lifetimeAverageTickNanos,
        double lifetimeAverageTickMillis,
        long lifetimeAverageTickDurationNanos,
        double lifetimeAverageTickDurationMillis,
        double lifetimeAverageTps,
        int lifetimeActiveTickCount,
        String eventSummary,
        List<PhasePayload> phases,
        List<EventPayload> events
    ) {
    }

    private record PhasePayload(
        String key,
        String label,
        int samples,
        long lastNanos,
        double lastMillis,
        long averageNanos,
        double averageMillis,
        long maxNanos,
        double maxMillis,
        long lifetimeAverageNanos,
        double lifetimeAverageMillis,
        long lifetimeMaxNanos,
        double lifetimeMaxMillis,
        int lifetimeSamples,
        double shareOfLastTick,
        double shareOfLifetimeAverageTick
    ) {
    }

    private record EventPayload(
        long capturedAtMillis,
        String sender,
        String message,
        String summary,
        String outcome,
        int score,
        String decidedBy,
        double measuredMillis,
        double tickMillis,
        double tps,
        List<EventPhasePayload> phases
    ) {
    }

    private record EventPhasePayload(String key, String label, double millis) {
    }
}
