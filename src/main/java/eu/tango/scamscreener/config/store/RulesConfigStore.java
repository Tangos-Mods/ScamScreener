package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.config.migration.ConfigSchema;
import eu.tango.scamscreener.config.migration.SimpleVersionedConfigMigration;

import java.nio.file.Path;

/**
 * JSON-backed store for the deterministic and similarity rule config.
 */
public final class RulesConfigStore extends MigratingConfigStore<RulesConfig> {
    private static final SimpleVersionedConfigMigration<RulesConfig> MIGRATION =
        new SimpleVersionedConfigMigration<>(ConfigSchema.RULES, RulesConfig::new);

    /**
     * Creates the rules config store bound to {@code rules.json}.
     */
    public RulesConfigStore() {
        this(ConfigPaths.rulesFile());
    }

    RulesConfigStore(Path path) {
        super(path, RulesConfig.class, MIGRATION);
    }
}
