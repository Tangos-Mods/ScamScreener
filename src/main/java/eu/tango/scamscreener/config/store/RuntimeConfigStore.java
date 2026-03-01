package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.RuntimeConfig;

/**
 * JSON-backed store for the global runtime config.
 */
public final class RuntimeConfigStore extends BaseConfig<RuntimeConfig> {
    /**
     * Creates the runtime config store bound to {@code runtime.json}.
     */
    public RuntimeConfigStore() {
        super(ConfigPaths.runtimeFile(), RuntimeConfig.class);
    }

    @Override
    protected RuntimeConfig createDefault() {
        return new RuntimeConfig();
    }
}
