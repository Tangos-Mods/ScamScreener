package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.RulesConfig;
import lombok.NonNull;

import java.nio.file.Path;

/**
 * JSON-backed store for the deterministic and similarity rule config.
 */
public final class RulesConfigStore extends BaseConfig<RulesConfig> {
    /**
     * Creates the rules config store bound to {@code rules.json}.
     */
    public RulesConfigStore() {
        this(ConfigPaths.rulesFile());
    }

    RulesConfigStore(Path path) {
        super(path, RulesConfig.class);
    }

    @Override
    protected RulesConfig createDefault() {
        return RulesConfigMigration.migrate(new RulesConfig()).config();
    }

    @Override
    public synchronized RulesConfig loadOrCreate() {
        return migrateLoaded(super.loadOrCreate());
    }

    @Override
    public synchronized RulesConfig reload() {
        return migrateLoaded(super.reload());
    }

    @Override
    public synchronized void save(@NonNull RulesConfig value) {
        super.save(RulesConfigMigration.migrate(value).config());
    }

    private RulesConfig migrateLoaded(RulesConfig loadedConfig) {
        RulesConfigMigration.MigrationResult migration = RulesConfigMigration.migrate(loadedConfig);
        if (migration.changed()) {
            super.save(migration.config());
        }

        return migration.config();
    }
}
