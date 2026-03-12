package eu.tango.scamscreener.profiler;

import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.training.TrainingCaseMappings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Client-thread profiler for ScamScreener hot paths with a lightweight HUD overlay.
 */
public final class ScamScreenerProfiler {
    private static final ScamScreenerProfiler INSTANCE = new ScamScreenerProfiler();
    private static final int MAX_EVENT_LOG_ENTRIES = 48;
    private static final int MAX_EVENT_PHASES = 48;
    private static final int MAX_EVENT_PHASE_BREAKDOWN = 4;
    private static final int HUD_X = 8;
    private static final int HUD_Y = 8;
    private static final int HUD_LINE_HEIGHT = 10;
    private static final int HUD_PADDING = 4;
    private static final int HISTORY_TICKS = 120;
    private static final int MAX_HUD_PHASES = 8;

    private final ModProfilerCore core = new ModProfilerCore(HISTORY_TICKS);
    private final Object logLock = new Object();
    private final ArrayDeque<EventLogEntry> recentEvents = new ArrayDeque<>();
    private final LinkedHashMap<String, Long> lastLoggedPhaseTotals = new LinkedHashMap<>();
    private boolean initialized;
    private long currentTickSequence;
    private long lastLoggedTickSequence = -1L;

    private ScamScreenerProfiler() {
    }

    /**
     * Returns the shared profiler instance.
     *
     * @return the profiler singleton
     */
    public static ScamScreenerProfiler getInstance() {
        return INSTANCE;
    }

    /**
     * Registers the profiler tick hooks and HUD layer once.
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        ClientTickEvents.START_CLIENT_TICK.register(client -> onTickStart(System.nanoTime()));
        ClientTickEvents.END_CLIENT_TICK.register(client -> onTickEnd(System.nanoTime()));
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.CHAT,
            ScamScreenerMod.id("scamscreener", "profiler_hud"),
            this::renderHud
        );
    }

    /**
     * Opens one profiling scope when the profiler is enabled.
     *
     * @param phaseKey the stable phase identifier
     * @param label the display label for the phase
     * @return a scope that records the elapsed time on close
     */
    public Scope scope(String phaseKey, String label) {
        if (!isRecordingEnabled()) {
            return NoopScope.INSTANCE;
        }

        return new ActiveScope(core, phaseKey, label, System.nanoTime());
    }

    /**
     * Stores one readable summary for the current active tick.
     *
     * @param summary the summary to expose on the overlay
     */
    public void recordSummary(String summary) {
        if (!isRecordingEnabled()) {
            return;
        }

        core.recordEventSummary(summary, System.nanoTime());
    }

    /**
     * Builds and stores a summary from one pipeline result.
     *
     * @param chatEvent the processed chat event
     * @param decision the produced pipeline decision
     */
    public void recordDecision(ChatEvent chatEvent, PipelineDecision decision) {
        if (!isRecordingEnabled() || decision == null) {
            return;
        }

        long nowNanos = System.nanoTime();
        String summary = buildDecisionSummary(chatEvent, decision);
        if (!summary.isBlank()) {
            core.recordEventSummary(summary, nowNanos);
        }

        appendEventLog(chatEvent, decision, core.snapshot(nowNanos, MAX_EVENT_PHASES));
    }

    /**
     * Returns the current snapshot for screens and tests.
     *
     * @return the latest profiler snapshot
     */
    public ModProfilerCore.Snapshot snapshot() {
        return snapshot(MAX_HUD_PHASES);
    }

    /**
     * Returns the current snapshot with a caller-defined phase limit.
     *
     * @param maxPhases the maximum number of phases to include
     * @return the latest profiler snapshot
     */
    public ModProfilerCore.Snapshot snapshot(int maxPhases) {
        return core.snapshot(System.nanoTime(), maxPhases);
    }

    /**
     * Returns a recent chat-event cost log for the web profiler.
     *
     * @param maxEntries the maximum number of rows to return
     * @return the most recent event rows, newest first
     */
    public List<EventView> recentEvents(int maxEntries) {
        int boundedLimit = Math.max(0, maxEntries);
        synchronized (logLock) {
            List<EventView> events = new ArrayList<>(Math.min(boundedLimit, recentEvents.size()));
            int index = 0;
            for (EventLogEntry entry : recentEvents) {
                if (index++ >= boundedLimit) {
                    break;
                }
                events.add(entry.view());
            }
            return List.copyOf(events);
        }
    }

    /**
     * Indicates whether the profiler should actively record and render samples.
     *
     * @return {@code true} when the profiler HUD is enabled in runtime config
     */
    public boolean isEnabled() {
        try {
            return ScamScreenerRuntime.getInstance().config().profiler().isHudEnabled();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Indicates whether the profiler should record samples.
     *
     * @return {@code true} when the profiler is enabled in runtime config
     */
    public boolean isRecordingEnabled() {
        return isEnabled();
    }

    /**
     * Applies a profiler-enabled state change and drops retained samples when disabled.
     *
     * @param enabled the new profiler state
     */
    public void onEnabledStateChanged(boolean enabled) {
        if (enabled) {
            return;
        }

        reset();
    }

    /**
     * Clears retained profiler samples and event log state without changing the enabled toggle.
     */
    public void reset() {
        core.reset();
        synchronized (logLock) {
            recentEvents.clear();
            lastLoggedPhaseTotals.clear();
            currentTickSequence = 0L;
            lastLoggedTickSequence = -1L;
        }
    }

    private void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (!isEnabled()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null || client.options.hudHidden) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        if (textRenderer == null) {
            return;
        }

        List<HudLine> lines = buildHudLines(snapshot());
        if (lines.isEmpty()) {
            return;
        }

        int width = 0;
        for (HudLine line : lines) {
            width = Math.max(width, textRenderer.getWidth(line.text()));
        }

        int height = lines.size() * HUD_LINE_HEIGHT;
        int left = HUD_X;
        int top = HUD_Y;
        context.fill(
            left - HUD_PADDING,
            top - HUD_PADDING,
            left + width + HUD_PADDING,
            top + height + HUD_PADDING,
            0x90000000
        );

        int y = top;
        for (HudLine line : lines) {
            context.drawTextWithShadow(textRenderer, line.text(), left, y, line.color());
            y += HUD_LINE_HEIGHT;
        }
    }

    private List<HudLine> buildHudLines(ModProfilerCore.Snapshot snapshot) {
        List<HudLine> lines = new ArrayList<>();
        if (snapshot == null || !snapshot.hasData()) {
            lines.add(new HudLine("ScamScreener Profiler | waiting for samples", 0xFFE0E0E0));
            return lines;
        }

        lines.add(new HudLine(
            "ScamScreener Profiler | last " + formatMillis(snapshot.lastTickNanos())
                + " | avg " + formatMillis(snapshot.averageTickNanos())
                + " | max " + formatMillis(snapshot.maxTickNanos())
                + " | active " + snapshot.activeTickCount(),
            colorForDuration(snapshot.lastTickNanos())
        ));

        if (snapshot.eventSummary() != null && !snapshot.eventSummary().isBlank()) {
            lines.add(new HudLine("Last: " + truncate(snapshot.eventSummary(), 72), 0xFFBFBFBF));
        }

        for (ModProfilerCore.PhaseView phase : snapshot.phases()) {
            lines.add(new HudLine(
                truncate(
                    phase.label()
                        + "  " + formatMillis(phase.lastNanos())
                        + " | avg " + formatMillis(phase.averageNanos())
                        + " | max " + formatMillis(phase.maxNanos())
                        + " | " + phase.samples() + "x",
                    84
                ),
                colorForDuration(phase.lastNanos() > 0L ? phase.lastNanos() : phase.averageNanos())
            ));
        }

        return lines;
    }

    private static String resolveStageLabel(String stageNameOrId) {
        String stageLabel = TrainingCaseMappings.stageLabel(stageNameOrId);
        if (!"Unknown Stage".equals(stageLabel)) {
            return stageLabel;
        }

        return stageNameOrId == null ? "Unknown" : stageNameOrId.trim();
    }

    private static String formatMillis(long nanos) {
        return String.format(Locale.ROOT, "%.2f ms", nanos / 1_000_000.0D);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.length() <= Math.max(1, maxLength)) {
            return normalized;
        }

        return normalized.substring(0, Math.max(1, maxLength) - 3) + "...";
    }

    private static int colorForDuration(long nanos) {
        double millis = nanos / 1_000_000.0D;
        if (millis >= 5.0D) {
            return 0xFFFF6B6B;
        }
        if (millis >= 2.0D) {
            return 0xFFFFB347;
        }
        if (millis >= 0.5D) {
            return 0xFFFFE08A;
        }

        return 0xFFE0E0E0;
    }

    private void onTickStart(long startNanos) {
        if (!isRecordingEnabled()) {
            core.discardActiveTick();
            return;
        }

        synchronized (logLock) {
            currentTickSequence++;
            if (currentTickSequence != lastLoggedTickSequence) {
                lastLoggedPhaseTotals.clear();
            }
        }
        core.startTick(startNanos);
    }

    private void onTickEnd(long endNanos) {
        if (!isRecordingEnabled()) {
            core.discardActiveTick();
            return;
        }

        core.endTick(endNanos);
    }

    private void appendEventLog(ChatEvent chatEvent, PipelineDecision decision, ModProfilerCore.Snapshot snapshot) {
        if (snapshot == null || decision == null) {
            return;
        }

        String sender = chatEvent == null || chatEvent.getSenderName().isBlank()
            ? "unknown"
            : chatEvent.getSenderName().trim();
        String message = truncate(chatEvent == null ? "" : chatEvent.getRawMessage(), 120);
        String decidedBy = decision.getDecidedByStage() == null || decision.getDecidedByStage().isBlank()
            ? ""
            : resolveStageLabel(decision.getDecidedByStage());
        EventDelta delta = computeEventDelta(snapshot);
        EventView view = new EventView(
            System.currentTimeMillis(),
            sender,
            message,
            buildDecisionSummary(chatEvent, decision),
            decision.getOutcome().name(),
            Math.max(0, decision.getTotalScore()),
            decidedBy,
            nanosToMillis(delta.measuredNanos()),
            nanosToMillis(snapshot.lastTickDurationNanos()),
            millisToTps(nanosToMillis(snapshot.lastTickDurationNanos())),
            delta.phases()
        );

        synchronized (logLock) {
            recentEvents.addFirst(new EventLogEntry(view));
            while (recentEvents.size() > MAX_EVENT_LOG_ENTRIES) {
                recentEvents.removeLast();
            }
        }
    }

    private EventDelta computeEventDelta(ModProfilerCore.Snapshot snapshot) {
        long tickSequence;
        synchronized (logLock) {
            tickSequence = currentTickSequence;
        }

        LinkedHashMap<String, Long> currentTotals = new LinkedHashMap<>();
        List<EventPhaseView> deltaPhases = new ArrayList<>();
        long measuredNanos = 0L;

        synchronized (logLock) {
            if (lastLoggedTickSequence != tickSequence) {
                lastLoggedPhaseTotals.clear();
            }

            for (ModProfilerCore.PhaseView phase : snapshot.phases()) {
                long currentTotal = Math.max(0L, phase.lastNanos());
                currentTotals.put(phase.key(), currentTotal);

                long previousTotal = lastLoggedPhaseTotals.getOrDefault(phase.key(), 0L);
                long deltaNanos = Math.max(0L, currentTotal - previousTotal);
                if (deltaNanos <= 0L) {
                    continue;
                }

                measuredNanos += deltaNanos;
                deltaPhases.add(new EventPhaseView(phase.key(), phase.label(), nanosToMillis(deltaNanos)));
            }

            lastLoggedPhaseTotals.clear();
            lastLoggedPhaseTotals.putAll(currentTotals);
            lastLoggedTickSequence = tickSequence;
        }

        deltaPhases.sort(Comparator.comparingDouble(EventPhaseView::millis).reversed());
        if (deltaPhases.size() > MAX_EVENT_PHASE_BREAKDOWN) {
            deltaPhases = new ArrayList<>(deltaPhases.subList(0, MAX_EVENT_PHASE_BREAKDOWN));
        }

        return new EventDelta(measuredNanos, List.copyOf(deltaPhases));
    }

    private static String buildDecisionSummary(ChatEvent chatEvent, PipelineDecision decision) {
        if (decision == null) {
            return "";
        }

        String sender = chatEvent == null || chatEvent.getSenderName().isBlank()
            ? "unknown"
            : chatEvent.getSenderName().trim();
        StringBuilder summary = new StringBuilder(sender)
            .append(" -> ")
            .append(decision.getOutcome().name())
            .append(" (")
            .append(Math.max(0, decision.getTotalScore()))
            .append(')');
        if (decision.getDecidedByStage() != null && !decision.getDecidedByStage().isBlank()) {
            summary.append(" via ");
            summary.append(resolveStageLabel(decision.getDecidedByStage()));
        }
        if (chatEvent != null && chatEvent.getRawMessage() != null && !chatEvent.getRawMessage().isBlank()) {
            summary.append(" | ");
            summary.append(truncate(chatEvent.getRawMessage(), 72));
        }

        return summary.toString();
    }

    private static double nanosToMillis(long nanos) {
        return Math.max(0L, nanos) / 1_000_000.0D;
    }

    private static double millisToTps(double millis) {
        if (millis <= 0.0D) {
            return 20.0D;
        }

        return Math.min(20.0D, 1000.0D / millis);
    }

    /**
     * Auto-closeable profiling scope.
     */
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    private record HudLine(String text, int color) {
    }

    private record ActiveScope(ModProfilerCore core, String phaseKey, String label, long startedAtNanos) implements Scope {
        @Override
        public void close() {
            long finishedAtNanos = System.nanoTime();
            core.recordPhase(phaseKey, label, finishedAtNanos - startedAtNanos, finishedAtNanos);
        }
    }

    public record EventView(
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
        List<EventPhaseView> phases
    ) {
    }

    public record EventPhaseView(String key, String label, double millis) {
    }

    private record EventDelta(long measuredNanos, List<EventPhaseView> phases) {
    }

    private record EventLogEntry(EventView view) {
    }

    private enum NoopScope implements Scope {
        INSTANCE;

        @Override
        public void close() {
        }
    }
}
