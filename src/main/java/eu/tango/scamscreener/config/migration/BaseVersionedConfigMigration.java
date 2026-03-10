package eu.tango.scamscreener.config.migration;

import java.util.Objects;

/**
 * Shared base for versioned config-schema migrations.
 *
 * @param <T> the versioned config payload type
 */
public abstract class BaseVersionedConfigMigration<T extends VersionedConfig> {
    private final ConfigSchema schema;

    protected BaseVersionedConfigMigration(ConfigSchema schema) {
        this.schema = Objects.requireNonNull(schema, "schema");
    }

    /**
     * Migrates a loaded config payload to the current schema version.
     *
     * <p>Missing or older payloads are replaced wholesale with the current
     * default config.
     *
     * @param config the loaded config payload, or {@code null} for defaults
     * @return the migrated config and whether it changed
     */
    public final MigrationResult<T> migrateLoaded(T config) {
        int targetVersion = schema.currentVersion();
        T migrated = config;
        boolean changed = false;

        if (migrated == null || migrated.version() < targetVersion) {
            migrated = createDefaultConfig();
            changed = true;
        }

        changed |= stampVersion(migrated, targetVersion);
        changed |= normalize(migrated);
        return new MigrationResult<>(migrated, changed);
    }

    /**
     * Prepares an in-memory config payload for persistence.
     *
     * <p>This keeps the supplied values and only stamps the current schema
     * version.
     *
     * @param config the in-memory config payload, or {@code null} for defaults
     * @return the prepared config and whether it changed
     */
    public final MigrationResult<T> prepareForSave(T config) {
        T prepared = config == null ? createDefaultConfig() : config;
        boolean changed = config == null;
        int targetVersion = schema.currentVersion();

        changed |= stampVersion(prepared, targetVersion);
        changed |= normalize(prepared);
        return new MigrationResult<>(prepared, changed);
    }

    /**
     * Creates the default config payload used for missing files.
     *
     * @return a non-null default payload
     */
    protected abstract T createDefaultConfig();

    /**
     * Normalizes config state after migrations have run.
     *
     * @param config the config payload to normalize
     * @return {@code true} when normalization changed the payload
     */
    protected boolean normalize(T config) {
        return false;
    }

    private boolean stampVersion(T config, int targetVersion) {
        if (config.version() == targetVersion) {
            return false;
        }

        config.setVersion(targetVersion);
        return true;
    }

    public record MigrationResult<T>(T config, boolean changed) {
    }
}
