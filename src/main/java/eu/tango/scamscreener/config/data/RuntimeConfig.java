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
    private AlertSettings alerts = new AlertSettings();
    private OutputSettings output = new OutputSettings();
    private ReviewSettings review = new ReviewSettings();
    private SafetySettings safety = new SafetySettings();
    private DebugSettings debug = new DebugSettings();

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
     * Returns the normalized alert settings.
     *
     * @return non-null alert settings
     */
    public AlertSettings alerts() {
        if (alerts == null) {
            alerts = new AlertSettings();
        }

        return alerts;
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
     * Returns the normalized review settings.
     *
     * @return non-null review settings
     */
    public ReviewSettings review() {
        if (review == null) {
            review = new ReviewSettings();
        }

        return review;
    }

    /**
     * Returns the normalized safety settings.
     *
     * @return non-null safety settings
     */
    public SafetySettings safety() {
        if (safety == null) {
            safety = new SafetySettings();
        }

        return safety;
    }

    /**
     * Returns the normalized debug settings.
     *
     * @return non-null debug settings
     */
    public DebugSettings debug() {
        if (debug == null) {
            debug = new DebugSettings();
        }

        return debug;
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
     * v1-style alert settings layered on top of the current pipeline outputs.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class AlertSettings {
        private AlertRiskLevel minimumRiskLevel = AlertRiskLevel.LOW;
        private AutoCaptureAlertLevel autoCaptureLevel = AutoCaptureAlertLevel.LOW;

        /**
         * Returns the normalized minimum visible alert threshold.
         *
         * @return the minimum visible alert threshold
         */
        public AlertRiskLevel minimumRiskLevel() {
            return minimumRiskLevel == null ? AlertRiskLevel.LOW : minimumRiskLevel;
        }

        /**
         * Returns the normalized review auto-capture level.
         *
         * @return the auto-capture level
         */
        public AutoCaptureAlertLevel autoCaptureLevel() {
            return autoCaptureLevel == null ? AutoCaptureAlertLevel.LOW : autoCaptureLevel;
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
        private boolean showAutoLeaveMessage = true;
        private boolean debugLogging = false;
    }

    /**
     * Review queue settings.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class ReviewSettings {
        private boolean captureEnabled = true;
        private int maxEntries = 200;

        /**
         * Returns the normalized review queue capacity.
         *
         * @return a bounded review queue size
         */
        public int maxEntries() {
            return Math.max(25, Math.min(500, maxEntries));
        }
    }

    /**
     * Restored non-pipeline safety settings from the v1 user flow.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class SafetySettings {
        private boolean autoLeaveOnBlacklist = false;
        private boolean muteFilterEnabled = false;
        private java.util.List<String> mutePatterns = new java.util.ArrayList<>();

        /**
         * Returns the normalized mute-pattern list.
         *
         * @return the configured mute patterns
         */
        public java.util.List<String> mutePatterns() {
            if (mutePatterns == null) {
                mutePatterns = new java.util.ArrayList<>();
            }

            return mutePatterns;
        }
    }

    /**
     * Restored v1 debug flag state.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class DebugSettings {
        private java.util.Map<String, Boolean> flags = new java.util.LinkedHashMap<>();

        /**
         * Returns the normalized debug-flag map.
         *
         * @return the configured debug flags
         */
        public java.util.Map<String, Boolean> flags() {
            if (flags == null) {
                flags = new java.util.LinkedHashMap<>();
            }

            return flags;
        }
    }

}
