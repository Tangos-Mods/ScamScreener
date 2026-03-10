package eu.tango.scamscreener.config.migration;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Default no-op migration for configs that only need schema versioning for now.
 *
 * @param <T> the versioned config payload type
 */
public final class SimpleVersionedConfigMigration<T extends VersionedConfig> extends BaseVersionedConfigMigration<T> {
    private final Supplier<T> defaultFactory;

    public SimpleVersionedConfigMigration(ConfigSchema schema, Supplier<T> defaultFactory) {
        super(schema);
        this.defaultFactory = Objects.requireNonNull(defaultFactory, "defaultFactory");
    }

    @Override
    protected T createDefaultConfig() {
        return defaultFactory.get();
    }
}
