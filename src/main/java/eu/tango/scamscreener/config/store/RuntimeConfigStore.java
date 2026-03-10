package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.config.migration.ConfigSchema;
import eu.tango.scamscreener.config.migration.SimpleVersionedConfigMigration;

import java.nio.file.Path;

/**
 * JSON-backed store for the global runtime config.
 */
public final class RuntimeConfigStore extends MigratingConfigStore<RuntimeConfig> {
    private static final SimpleVersionedConfigMigration<RuntimeConfig> MIGRATION =
        new SimpleVersionedConfigMigration<>(ConfigSchema.RUNTIME, RuntimeConfig::new);

    /**
     * Creates the runtime config store bound to {@code runtime.json}.
     */
    public RuntimeConfigStore() {
        this(ConfigPaths.runtimeFile());
    }

    RuntimeConfigStore(Path path) {
        super(path, RuntimeConfig.class, MIGRATION);
    }
}
