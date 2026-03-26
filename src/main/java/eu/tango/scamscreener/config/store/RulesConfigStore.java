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
}
