package eu.tango.scamscreener.profiler.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.tango.scamscreener.profiler.ModProfilerCore;
import eu.tango.scamscreener.profiler.ScamScreenerProfiler;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfilerExportPayloadTest {
    @Test
    void exportJsonUsesSsppSchemaAndIncludesProfilerData() {
        ModProfilerCore core = new ModProfilerCore(8);
        core.startTick(1_000_000L);
        core.recordPhase("pipeline.total", "Pipeline", 2_000_000L, 3_000_000L);
        core.recordEventSummary("Alice -> REVIEW (36)", 3_000_000L);
        core.endTick(5_000_000L);

        String json = ProfilerExportPayload.exportJson(
            1_710_000_000_000L,
            "1.2.3",
            "1.21.11",
            true,
            true,
            core.snapshot(5_000_000L, 16),
            List.of(new ScamScreenerProfiler.EventView(
                1_710_000_000_123L,
                "Alice",
                "add me on discord",
                "Alice -> REVIEW (36)",
                "REVIEW",
                36,
                "Context",
                2.0D,
                4.0D,
                20.0D,
                List.of(new ScamScreenerProfiler.EventPhaseView("pipeline.total", "Pipeline", 2.0D))
            ))
        );

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals("scamscreener_performance_profile", root.get("format").getAsString());
        assertEquals(1, root.get("schemaVersion").getAsInt());
        assertEquals(1_710_000_000_000L, root.get("exportedAtMillis").getAsLong());

        JsonObject metadata = root.getAsJsonObject("metadata");
        assertEquals("1.2.3", metadata.get("modVersion").getAsString());
        assertEquals("1.21.11", metadata.get("minecraftVersion").getAsString());

        JsonObject runtime = root.getAsJsonObject("runtime");
        assertTrue(runtime.get("recordingEnabled").getAsBoolean());
        assertEquals("Alice -> REVIEW (36)", runtime.get("eventSummary").getAsString());

        JsonObject metrics = root.getAsJsonObject("metrics");
        assertEquals(50.0D, metrics.get("normalMspt").getAsDouble());
        assertEquals(20, metrics.get("normalTps").getAsInt());
        assertEquals(1, metrics.get("lifetimeActiveTickCount").getAsInt());

        JsonArray phases = root.getAsJsonArray("phases");
        assertEquals(1, phases.size());
        JsonObject phase = phases.get(0).getAsJsonObject();
        assertEquals("pipeline.total", phase.get("key").getAsString());
        assertEquals(2_000_000L, phase.get("lastNanos").getAsLong());
        assertEquals(1, phase.get("lifetimeSamples").getAsInt());

        JsonArray recentEvents = root.getAsJsonArray("recentEvents");
        assertEquals(1, recentEvents.size());
        JsonObject event = recentEvents.get(0).getAsJsonObject();
        assertEquals("Alice", event.get("sender").getAsString());
        assertEquals("REVIEW", event.get("outcome").getAsString());
    }

    @Test
    void exportFileNameUsesSsppExtension() {
        assertTrue(ProfilerExportPayload.exportFileName(1_710_000_000_000L).endsWith(".sspp"));
    }
}
