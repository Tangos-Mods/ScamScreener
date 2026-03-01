package eu.tango.scamscreener.config.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Runtime-wide configuration loaded from {@code runtime.json}.
 *
 * <p>The first version stays intentionally small. More stage-specific settings
 * can be added here later or split into dedicated files once they carry enough
 * weight to justify that move.
 */
@Getter
@Setter
@NoArgsConstructor
public final class RuntimeConfig {
    private PipelineSettings pipeline = new PipelineSettings();
    private OutputSettings output = new OutputSettings();
    private StageSettings stages = new StageSettings();

    /**
     * Returns the normalized pipeline settings.
     *
     * @return non-null pipeline settings
     */
    public PipelineSettings pipeline() {
        if (pipeline == null) {
            pipeline = new PipelineSettings();
        }

        return pipeline;
    }

    /**
     * Returns the normalized output settings.
     *
     * @return non-null output settings
     */
    public OutputSettings output() {
        if (output == null) {
            output = new OutputSettings();
        }

        return output;
    }

    /**
     * Returns the normalized stage settings.
     *
     * @return non-null stage settings
     */
    public StageSettings stages() {
        if (stages == null) {
            stages = new StageSettings();
        }

        return stages;
    }

    /**
     * Top-level pipeline settings.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class PipelineSettings {
        private int reviewThreshold = 1;

        /**
         * Returns the normalized review threshold used by the engine.
         *
         * @return a non-negative review threshold
         */
        public int reviewThreshold() {
            return Math.max(0, reviewThreshold);
        }
    }

    /**
     * User-facing output settings.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class OutputSettings {
        private boolean showRiskWarningMessage = true;
        private boolean pingOnRiskWarning = true;
        private boolean showBlacklistWarningMessage = true;
        private boolean pingOnBlacklistWarning = true;
        private boolean debugLogging = false;
    }

    /**
     * Simple stage toggle settings.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class StageSettings {
        private boolean modelEnabled = true;
    }
}
