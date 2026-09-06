package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.config.migration.ConfigSchema;

import java.nio.file.Path;

/**
 * JSON-backed store for the deterministic and similarity rule config.
 */
public final class RulesConfigStore extends VersionedConfigStore<RulesConfig> {
    /**
     * Creates the rules config store bound to {@code rules.json}.
     */
    public RulesConfigStore() {
        this(ConfigPaths.rulesFile());
    }

    RulesConfigStore(Path path) {
        super(path, RulesConfig.class, ConfigSchema.RULES.currentVersion());
    }

    @Override
    protected RulesConfig createDefaultValue() {
        return new RulesConfig();
    }

    @Override
    protected RulesConfig preserveOutdatedValue(RulesConfig outdatedValue, RulesConfig defaultValue) {
        if (outdatedValue == null || outdatedValue.version() != 3) {
            return defaultValue;
        }

        RulesConfig.RuleStageSettings settings = outdatedValue.ruleStage();
        if (RulesConfig.RuleStageSettings.V3_EXTERNAL_PLATFORM_PATTERN.equals(settings.getExternalPlatformPattern())) {
            settings.setExternalPlatformPattern(RulesConfig.RuleStageSettings.DEFAULT_EXTERNAL_PLATFORM_PATTERN);
        }
        outdatedValue.setVersion(ConfigSchema.RULES.currentVersion());
        return outdatedValue;
    }
}
