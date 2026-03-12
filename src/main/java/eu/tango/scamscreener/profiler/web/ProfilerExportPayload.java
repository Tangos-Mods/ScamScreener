package eu.tango.scamscreener.profiler.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.profiler.ModProfilerCore;
import eu.tango.scamscreener.profiler.ScamScreenerProfiler;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ProfilerExportPayload {
    static final String MIME_TYPE = "application/x-scamscreener-performance-profile+json";
    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();
    private static final String FORMAT = "scamscreener_performance_profile";
    private static final int SCHEMA_VERSION = 1;
    private static final int NORMAL_TPS = 20;
    private static final int NORMAL_MSPT = 50;
    private static final int MAX_EXPORT_PHASES = 128;
    private static final int MAX_EXPORT_EVENTS = 48;
    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter
        .ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT)
        .withZone(ZoneOffset.UTC);

    private ProfilerExportPayload() {
    }

    static ExportFile exportFile() {
        ScamScreenerProfiler profiler = ScamScreenerProfiler.getInstance();
        long exportedAtMillis = System.currentTimeMillis();
        String json = exportJson(
            exportedAtMillis,
            ScamScreenerMod.VERSION,
            ScamScreenerMod.MINECRAFT,
            profiler.isEnabled(),
            profiler.isRecordingEnabled(),
            profiler.snapshot(MAX_EXPORT_PHASES),
            profiler.recentEvents(MAX_EXPORT_EVENTS)
        );
        return new ExportFile(
            exportFileName(exportedAtMillis),
            MIME_TYPE,
            json.getBytes(StandardCharsets.UTF_8)
        );
    }

    static String exportFileName(long exportedAtMillis) {
        return "scamscreener-profiler-" + FILE_NAME_FORMAT.format(Instant.ofEpochMilli(exportedAtMillis)) + ".sspp";
    }

    static String exportJson(
        long exportedAtMillis,
        String modVersion,
        String minecraftVersion,
        boolean hudEnabled,
        boolean recordingEnabled,
        ModProfilerCore.Snapshot snapshot,
        List<ScamScreenerProfiler.EventView> events
    ) {
        long lastTickNanos = Math.max(0L, snapshot.lastTickNanos());
        long lifetimeAverageTickNanos = Math.max(0L, snapshot.lifetimeAverageTickNanos());
        List<PhaseExport> phases = new ArrayList<>(snapshot.phases().size());
        for (ModProfilerCore.PhaseView phase : snapshot.phases()) {
            phases.add(new PhaseExport(
                phase.key(),
                phase.label(),
                phase.lastNanos(),
                nanosToMillis(phase.lastNanos()),
                phase.averageNanos(),
                nanosToMillis(phase.averageNanos()),
                phase.maxNanos(),
                nanosToMillis(phase.maxNanos()),
                phase.samples(),
                phase.lifetimeAverageNanos(),
                nanosToMillis(phase.lifetimeAverageNanos()),
                phase.lifetimeMaxNanos(),
                nanosToMillis(phase.lifetimeMaxNanos()),
                phase.lifetimeSamples(),
                lastTickNanos <= 0L ? 0.0D : Math.min(1.0D, phase.lastNanos() / (double) lastTickNanos),
                lifetimeAverageTickNanos <= 0L ? 0.0D : Math.min(1.0D, phase.lifetimeAverageNanos() / (double) lifetimeAverageTickNanos)
            ));
        }

        List<EventExport> exportedEvents = new ArrayList<>(events.size());
        for (ScamScreenerProfiler.EventView event : events) {
            List<EventPhaseExport> eventPhases = new ArrayList<>(event.phases().size());
            for (ScamScreenerProfiler.EventPhaseView phase : event.phases()) {
                eventPhases.add(new EventPhaseExport(phase.key(), phase.label(), phase.millis()));
            }
            exportedEvents.add(new EventExport(
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

        double lastTickMillis = nanosToMillis(snapshot.lastTickNanos());
        double averageTickMillis = nanosToMillis(snapshot.averageTickNanos());
        double maxTickMillis = nanosToMillis(snapshot.maxTickNanos());
        double lastTickDurationMillis = nanosToMillis(snapshot.lastTickDurationNanos());
        double averageTickDurationMillis = nanosToMillis(snapshot.averageTickDurationNanos());
        double maxTickDurationMillis = nanosToMillis(snapshot.maxTickDurationNanos());
        double lifetimeAverageTickMillis = nanosToMillis(snapshot.lifetimeAverageTickNanos());
        double lifetimeAverageTickDurationMillis = nanosToMillis(snapshot.lifetimeAverageTickDurationNanos());

        return GSON.toJson(new ExportDocument(
            FORMAT,
            SCHEMA_VERSION,
            exportedAtMillis,
            new ExportMetadata(
                "ScamScreener",
                modVersion == null ? "unknown" : modVersion,
                minecraftVersion == null ? "unknown" : minecraftVersion,
                "This file captures rolling and lifetime profiler data for developer analysis.",
                new EnvironmentMetadata(
                    System.getProperty("java.version", "unknown"),
                    System.getProperty("os.name", "unknown"),
                    System.getProperty("os.version", "unknown")
                )
            ),
            new RuntimeState(
                snapshot.hasData(),
                hudEnabled,
                recordingEnabled,
                snapshot.eventSummary() == null ? "" : snapshot.eventSummary()
            ),
            new TickMetrics(
                NORMAL_MSPT,
                NORMAL_TPS,
                snapshot.activeTickCount(),
                snapshot.lifetimeActiveTickCount(),
                snapshot.lastTickNanos(),
                lastTickMillis,
                snapshot.averageTickNanos(),
                averageTickMillis,
                snapshot.maxTickNanos(),
                maxTickMillis,
                snapshot.lastTickDurationNanos(),
                lastTickDurationMillis,
                snapshot.averageTickDurationNanos(),
                averageTickDurationMillis,
                snapshot.maxTickDurationNanos(),
                maxTickDurationMillis,
                msptToTps(lastTickDurationMillis),
                msptToTps(averageTickDurationMillis),
                snapshot.lifetimeAverageTickNanos(),
                lifetimeAverageTickMillis,
                snapshot.lifetimeAverageTickDurationNanos(),
                lifetimeAverageTickDurationMillis,
                msptToTps(lifetimeAverageTickDurationMillis)
            ),
            phases,
            exportedEvents
        ));
    }

    private static double nanosToMillis(long nanos) {
        return Math.max(0L, nanos) / 1_000_000.0D;
    }

    private static double msptToTps(double mspt) {
        if (mspt <= 0.0D) {
            return NORMAL_TPS;
        }

        return Math.min(NORMAL_TPS, 1000.0D / mspt);
    }

    record ExportFile(String fileName, String mimeType, byte[] bytes) {
    }

    private record ExportDocument(
        String format,
        int schemaVersion,
        long exportedAtMillis,
        ExportMetadata metadata,
        RuntimeState runtime,
        TickMetrics metrics,
        List<PhaseExport> phases,
        List<EventExport> recentEvents
    ) {
    }

    private record ExportMetadata(
        String modName,
        String modVersion,
        String minecraftVersion,
        String description,
        EnvironmentMetadata environment
    ) {
    }

    private record EnvironmentMetadata(
        String javaVersion,
        String osName,
        String osVersion
    ) {
    }

    private record RuntimeState(
        boolean hasData,
        boolean hudEnabled,
        boolean recordingEnabled,
        String eventSummary
    ) {
    }

    private record TickMetrics(
        double normalMspt,
        int normalTps,
        int rollingActiveTickCount,
        int lifetimeActiveTickCount,
        long lastModTickNanos,
        double lastModTickMillis,
        long rollingAverageModTickNanos,
        double rollingAverageModTickMillis,
        long rollingMaxModTickNanos,
        double rollingMaxModTickMillis,
        long lastClientTickDurationNanos,
        double lastClientTickDurationMillis,
        long rollingAverageClientTickDurationNanos,
        double rollingAverageClientTickDurationMillis,
        long rollingMaxClientTickDurationNanos,
        double rollingMaxClientTickDurationMillis,
        double currentTps,
        double rollingAverageTps,
        long lifetimeAverageModTickNanos,
        double lifetimeAverageModTickMillis,
        long lifetimeAverageClientTickDurationNanos,
        double lifetimeAverageClientTickDurationMillis,
        double lifetimeAverageTps
    ) {
    }

    private record PhaseExport(
        String key,
        String label,
        long lastNanos,
        double lastMillis,
        long rollingAverageNanos,
        double rollingAverageMillis,
        long rollingMaxNanos,
        double rollingMaxMillis,
        int rollingSamples,
        long lifetimeAverageNanos,
        double lifetimeAverageMillis,
        long lifetimeMaxNanos,
        double lifetimeMaxMillis,
        int lifetimeSamples,
        double shareOfLastTick,
        double shareOfLifetimeAverageTick
    ) {
    }

    private record EventExport(
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
        List<EventPhaseExport> phases
    ) {
    }

    private record EventPhaseExport(String key, String label, double millis) {
    }
}
