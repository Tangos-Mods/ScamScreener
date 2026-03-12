package eu.tango.scamscreener.profiler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small tick-scoped profiler core independent from Minecraft rendering APIs.
 */
public final class ModProfilerCore {
    private static final Snapshot EMPTY_SNAPSHOT = new Snapshot(0L, 0L, 0L, 0L, 0L, 0L, 0, 0L, 0L, 0, "", List.of());

    private final int historyLimit;
    private final ArrayDeque<TickSnapshot> history = new ArrayDeque<>();
    private final LinkedHashMap<String, PhaseAggregate> lifetimePhaseAggregates = new LinkedHashMap<>();
    private TickAccumulator activeTick;
    private TickSnapshot lastActiveTick = TickSnapshot.empty();
    private long lifetimeMeasuredNanos;
    private long lifetimeTickDurationNanos;
    private int lifetimeActiveTickCount;

    /**
     * Creates the profiler core with the default rolling history size.
     */
    public ModProfilerCore() {
        this(120);
    }

    /**
     * Creates the profiler core with an explicit history size.
     *
     * @param historyLimit the maximum number of active tick samples to retain
     */
    public ModProfilerCore(int historyLimit) {
        this.historyLimit = Math.max(1, historyLimit);
    }

    /**
     * Starts a fresh client tick sample.
     *
     * @param startNanos the tick start timestamp in nanos
     */
    public synchronized void startTick(long startNanos) {
        commitActiveTick(startNanos);
        activeTick = new TickAccumulator(Math.max(0L, startNanos));
    }

    /**
     * Finishes the current client tick sample.
     *
     * @param endNanos the tick end timestamp in nanos
     */
    public synchronized void endTick(long endNanos) {
        commitActiveTick(endNanos);
    }

    /**
     * Drops the current in-flight tick without committing it to history.
     */
    public synchronized void discardActiveTick() {
        activeTick = null;
    }

    /**
     * Records one measured phase duration.
     *
     * @param key the stable phase key
     * @param label the display label for the phase
     * @param durationNanos the measured duration in nanos
     * @param endNanos the end timestamp in nanos
     */
    public synchronized void recordPhase(String key, String label, long durationNanos, long endNanos) {
        if (durationNanos <= 0L) {
            return;
        }

        TickAccumulator tick = ensureActiveTick(Math.max(0L, endNanos - durationNanos));
        tick.record(normalizeKey(key), normalizeLabel(label, key), durationNanos);
    }

    /**
     * Stores a human-readable summary for the current active tick.
     *
     * @param summary the summary text to surface in snapshots
     * @param nowNanos the current timestamp in nanos
     */
    public synchronized void recordEventSummary(String summary, long nowNanos) {
        String normalizedSummary = normalizeSummary(summary);
        if (normalizedSummary.isBlank()) {
            return;
        }

        ensureActiveTick(nowNanos).setEventSummary(normalizedSummary);
    }

    /**
     * Returns the latest profiler snapshot.
     *
     * @param nowNanos the current timestamp in nanos
     * @param maxPhases the maximum number of phase rows to include
     * @return the current aggregated snapshot
     */
    public synchronized Snapshot snapshot(long nowNanos, int maxPhases) {
        TickSnapshot currentTick = activeTick == null ? TickSnapshot.empty() : activeTick.snapshot(nowNanos);
        TickSnapshot visibleTick = currentTick.hasMeasurements()
            ? currentTick
            : (lastActiveTick.hasMeasurements() ? lastActiveTick : currentTick);

        List<TickSnapshot> aggregateTicks = new ArrayList<>(history);
        if (currentTick.hasMeasurements()) {
            aggregateTicks.add(currentTick);
        }

        long totalMeasuredNanos = 0L;
        long maxMeasuredNanos = 0L;
        long totalTickDurationNanos = 0L;
        long maxTickDurationNanos = 0L;
        int activeTickCount = 0;
        Map<String, PhaseAggregate> phaseAggregates = new LinkedHashMap<>();
        for (TickSnapshot tick : aggregateTicks) {
            if (!tick.hasMeasurements()) {
                continue;
            }

            activeTickCount++;
            totalMeasuredNanos += tick.totalMeasuredNanos();
            maxMeasuredNanos = Math.max(maxMeasuredNanos, tick.totalMeasuredNanos());
            totalTickDurationNanos += tick.tickDurationNanos();
            maxTickDurationNanos = Math.max(maxTickDurationNanos, tick.tickDurationNanos());
            for (PhaseSample sample : tick.phases().values()) {
                phaseAggregates.computeIfAbsent(sample.key(), ignored -> new PhaseAggregate(sample.key(), sample.label()))
                    .record(sample.totalNanos());
            }
        }

        LinkedHashMap<String, PhaseAggregate> lifetimeAggregates = copyPhaseAggregates(lifetimePhaseAggregates);
        if (currentTick.hasMeasurements()) {
            for (PhaseSample sample : currentTick.phases().values()) {
                lifetimeAggregates.computeIfAbsent(sample.key(), ignored -> new PhaseAggregate(sample.key(), sample.label()))
                    .record(sample.totalNanos());
            }
        }

        if (!visibleTick.hasMeasurements() && phaseAggregates.isEmpty() && lifetimeAggregates.isEmpty()) {
            return EMPTY_SNAPSHOT;
        }

        List<PhaseView> phases = buildPhaseViews(visibleTick, phaseAggregates, lifetimeAggregates, maxPhases);
        long averageMeasuredNanos = activeTickCount <= 0 ? 0L : totalMeasuredNanos / activeTickCount;
        long averageTickDurationNanos = activeTickCount <= 0 ? 0L : totalTickDurationNanos / activeTickCount;
        long lifetimeMeasuredWithCurrent = lifetimeMeasuredNanos;
        long lifetimeTickDurationWithCurrent = lifetimeTickDurationNanos;
        int lifetimeCountWithCurrent = lifetimeActiveTickCount;
        if (currentTick.hasMeasurements()) {
            lifetimeMeasuredWithCurrent += currentTick.totalMeasuredNanos();
            lifetimeTickDurationWithCurrent += currentTick.tickDurationNanos();
            lifetimeCountWithCurrent++;
        }
        return new Snapshot(
            visibleTick.totalMeasuredNanos(),
            averageMeasuredNanos,
            maxMeasuredNanos,
            visibleTick.tickDurationNanos(),
            averageTickDurationNanos,
            maxTickDurationNanos,
            activeTickCount,
            lifetimeCountWithCurrent <= 0 ? 0L : lifetimeMeasuredWithCurrent / lifetimeCountWithCurrent,
            lifetimeCountWithCurrent <= 0 ? 0L : lifetimeTickDurationWithCurrent / lifetimeCountWithCurrent,
            lifetimeCountWithCurrent,
            visibleTick.eventSummary(),
            phases
        );
    }

    /**
     * Clears retained history, the active tick, and lifetime aggregates.
     */
    public synchronized void reset() {
        history.clear();
        activeTick = null;
        lastActiveTick = TickSnapshot.empty();
        lifetimeMeasuredNanos = 0L;
        lifetimeTickDurationNanos = 0L;
        lifetimeActiveTickCount = 0;
        lifetimePhaseAggregates.clear();
    }

    private List<PhaseView> buildPhaseViews(
        TickSnapshot visibleTick,
        Map<String, PhaseAggregate> phaseAggregates,
        Map<String, PhaseAggregate> lifetimeAggregates,
        int maxPhases
    ) {
        LinkedHashMap<String, PhaseAggregate> allAggregates = new LinkedHashMap<>(lifetimeAggregates);
        for (PhaseAggregate aggregate : phaseAggregates.values()) {
            allAggregates.putIfAbsent(aggregate.key(), aggregate);
        }

        List<PhaseView> phaseViews = new ArrayList<>(allAggregates.size());
        for (PhaseAggregate aggregate : allAggregates.values()) {
            PhaseAggregate rollingAggregate = phaseAggregates.get(aggregate.key());
            PhaseAggregate lifetimeAggregate = lifetimeAggregates.get(aggregate.key());
            PhaseSample currentSample = visibleTick.phases().get(aggregate.key());
            phaseViews.add(new PhaseView(
                aggregate.key(),
                lifetimeAggregate == null ? aggregate.label() : lifetimeAggregate.label(),
                currentSample == null ? 0L : currentSample.totalNanos(),
                rollingAggregate == null ? 0L : rollingAggregate.averageNanos(),
                rollingAggregate == null ? 0L : rollingAggregate.maxNanos(),
                rollingAggregate == null ? 0 : rollingAggregate.samples(),
                lifetimeAggregate == null ? 0L : lifetimeAggregate.averageNanos(),
                lifetimeAggregate == null ? 0L : lifetimeAggregate.maxNanos(),
                lifetimeAggregate == null ? 0 : lifetimeAggregate.samples()
            ));
        }

        phaseViews.sort(Comparator
            .comparingInt((PhaseView view) -> visibleTick.phaseOrder(view.key()))
            .thenComparing(Comparator.comparingLong(PhaseView::lastNanos).reversed())
            .thenComparing(Comparator.comparingLong(PhaseView::lifetimeAverageNanos).reversed())
            .thenComparing(PhaseView::label));

        int boundedMaxPhases = Math.max(0, maxPhases);
        if (boundedMaxPhases > 0 && phaseViews.size() > boundedMaxPhases) {
            return List.copyOf(phaseViews.subList(0, boundedMaxPhases));
        }

        return List.copyOf(phaseViews);
    }

    private TickAccumulator ensureActiveTick(long syntheticStartNanos) {
        if (activeTick != null) {
            return activeTick;
        }

        activeTick = new TickAccumulator(Math.max(0L, syntheticStartNanos));
        return activeTick;
    }

    private void commitActiveTick(long endNanos) {
        if (activeTick == null) {
            return;
        }

        TickSnapshot snapshot = activeTick.snapshot(endNanos);
        if (snapshot.hasMeasurements()) {
            history.addLast(snapshot);
            while (history.size() > historyLimit) {
                history.removeFirst();
            }
            lastActiveTick = snapshot;
            lifetimeMeasuredNanos += snapshot.totalMeasuredNanos();
            lifetimeTickDurationNanos += snapshot.tickDurationNanos();
            lifetimeActiveTickCount++;
            for (PhaseSample sample : snapshot.phases().values()) {
                lifetimePhaseAggregates.computeIfAbsent(sample.key(), ignored -> new PhaseAggregate(sample.key(), sample.label()))
                    .record(sample.totalNanos());
            }
        }
        activeTick = null;
    }

    private static LinkedHashMap<String, PhaseAggregate> copyPhaseAggregates(Map<String, PhaseAggregate> source) {
        LinkedHashMap<String, PhaseAggregate> copy = new LinkedHashMap<>();
        for (PhaseAggregate aggregate : source.values()) {
            copy.put(aggregate.key(), aggregate.copy());
        }
        return copy;
    }

    private static String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            return "unknown";
        }

        return key.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizeLabel(String label, String fallbackKey) {
        if (label != null && !label.isBlank()) {
            return label;
        }

        return fallbackKey == null ? "Unknown" : fallbackKey.trim();
    }

    private static String normalizeSummary(String summary) {
        if (summary == null) {
            return "";
        }

        return summary.replace('\r', ' ').replace('\n', ' ').trim();
    }

    /**
     * Immutable aggregated snapshot surfaced to renderers and screens.
     *
     * @param lastTickNanos the measured time of the visible tick
     * @param averageTickNanos the average measured time across the retained active ticks
     * @param maxTickNanos the maximum measured time across the retained active ticks
     * @param lastTickDurationNanos the visible tick duration based on tick start/end boundaries
     * @param averageTickDurationNanos the average retained tick duration
     * @param maxTickDurationNanos the maximum retained tick duration
     * @param activeTickCount the number of active ticks in the retained history
     * @param lifetimeAverageTickNanos the average measured time across all active ticks since profiler start
     * @param lifetimeAverageTickDurationNanos the average tick duration across all active ticks since profiler start
     * @param lifetimeActiveTickCount the total number of active ticks recorded since profiler start
     * @param eventSummary the last event summary attached to the visible tick
     * @param phases the aggregated phase rows
     */
    public record Snapshot(
        long lastTickNanos,
        long averageTickNanos,
        long maxTickNanos,
        long lastTickDurationNanos,
        long averageTickDurationNanos,
        long maxTickDurationNanos,
        int activeTickCount,
        long lifetimeAverageTickNanos,
        long lifetimeAverageTickDurationNanos,
        int lifetimeActiveTickCount,
        String eventSummary,
        List<PhaseView> phases
    ) {
        /**
         * Indicates whether at least one active tick sample is present.
         *
         * @return {@code true} when the snapshot contains measurements
         */
        public boolean hasData() {
            return lastTickNanos > 0L || lastTickDurationNanos > 0L || (phases != null && !phases.isEmpty());
        }
    }

    /**
     * Immutable phase row included in a snapshot.
     *
     * @param key the stable phase key
     * @param label the display label
     * @param lastNanos the visible-tick duration
     * @param averageNanos the rolling average duration across retained samples
     * @param maxNanos the rolling maximum duration across retained samples
     * @param samples the number of retained samples contributing to the rolling aggregates
     * @param lifetimeAverageNanos the average duration across all samples since profiler start
     * @param lifetimeMaxNanos the maximum duration across all samples since profiler start
     * @param lifetimeSamples the total number of samples recorded since profiler start
     */
    public record PhaseView(
        String key,
        String label,
        long lastNanos,
        long averageNanos,
        long maxNanos,
        int samples,
        long lifetimeAverageNanos,
        long lifetimeMaxNanos,
        int lifetimeSamples
    ) {
    }

    private static final class TickAccumulator {
        private final long startNanos;
        private final LinkedHashMap<String, PhaseAccumulator> phases = new LinkedHashMap<>();
        private String eventSummary = "";

        private TickAccumulator(long startNanos) {
            this.startNanos = Math.max(0L, startNanos);
        }

        private void record(String key, String label, long durationNanos) {
            phases.computeIfAbsent(key, ignored -> new PhaseAccumulator(key, label)).record(durationNanos);
        }

        private void setEventSummary(String eventSummary) {
            this.eventSummary = eventSummary == null ? "" : eventSummary;
        }

        private TickSnapshot snapshot(long endNanos) {
            long totalMeasuredNanos = 0L;
            LinkedHashMap<String, PhaseSample> phaseSamples = new LinkedHashMap<>();
            int order = 0;
            for (PhaseAccumulator accumulator : phases.values()) {
                PhaseSample sample = accumulator.snapshot(order++);
                phaseSamples.put(sample.key(), sample);
                totalMeasuredNanos += sample.totalNanos();
            }

            long tickDurationNanos = Math.max(totalMeasuredNanos, Math.max(0L, endNanos) - startNanos);
            return new TickSnapshot(tickDurationNanos, totalMeasuredNanos, eventSummary, phaseSamples);
        }
    }

    private static final class PhaseAccumulator {
        private final String key;
        private final String label;
        private long totalNanos;
        private int samples;

        private PhaseAccumulator(String key, String label) {
            this.key = key;
            this.label = label;
        }

        private void record(long durationNanos) {
            totalNanos += Math.max(0L, durationNanos);
            samples++;
        }

        private PhaseSample snapshot(int order) {
            return new PhaseSample(key, label, totalNanos, samples, order);
        }
    }

    private static final class PhaseAggregate {
        private final String key;
        private final String label;
        private long totalNanos;
        private long maxNanos;
        private int samples;

        private PhaseAggregate(String key, String label) {
            this.key = key;
            this.label = label;
        }

        private void record(long durationNanos) {
            totalNanos += Math.max(0L, durationNanos);
            maxNanos = Math.max(maxNanos, Math.max(0L, durationNanos));
            samples++;
        }

        private String key() {
            return key;
        }

        private String label() {
            return label;
        }

        private long averageNanos() {
            return samples <= 0 ? 0L : totalNanos / samples;
        }

        private long maxNanos() {
            return maxNanos;
        }

        private int samples() {
            return samples;
        }

        private PhaseAggregate copy() {
            PhaseAggregate copy = new PhaseAggregate(key, label);
            copy.totalNanos = totalNanos;
            copy.maxNanos = maxNanos;
            copy.samples = samples;
            return copy;
        }
    }

    private record TickSnapshot(
        long tickDurationNanos,
        long totalMeasuredNanos,
        String eventSummary,
        LinkedHashMap<String, PhaseSample> phases
    ) {
        private static TickSnapshot empty() {
            return new TickSnapshot(0L, 0L, "", new LinkedHashMap<>());
        }

        private boolean hasMeasurements() {
            return totalMeasuredNanos > 0L && !phases.isEmpty();
        }

        private int phaseOrder(String key) {
            PhaseSample sample = phases.get(key);
            return sample == null ? Integer.MAX_VALUE : sample.order();
        }
    }

    private record PhaseSample(
        String key,
        String label,
        long totalNanos,
        int samples,
        int order
    ) {
    }
}
