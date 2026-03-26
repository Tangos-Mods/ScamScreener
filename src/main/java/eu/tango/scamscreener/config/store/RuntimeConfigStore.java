package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.config.migration.ConfigSchema;

import java.nio.file.Path;

/**
 * JSON-backed store for the global runtime config.
 */
public final class RuntimeConfigStore extends VersionedConfigStore<RuntimeConfig> {
    /**
     * Creates the runtime config store bound to {@code runtime.json}.
     */
    public RuntimeConfigStore() {
        this(ConfigPaths.runtimeFile());
    }

    RuntimeConfigStore(Path path) {
        super(path, RuntimeConfig.class, ConfigSchema.RUNTIME.currentVersion());
    }

    @Override
    protected RuntimeConfig createDefaultValue() {
        return new RuntimeConfig();
    }
}
