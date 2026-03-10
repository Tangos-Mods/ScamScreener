package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.migration.BaseVersionedConfigMigration;
import eu.tango.scamscreener.config.migration.VersionedConfig;
import lombok.NonNull;

import java.nio.file.Path;

/**
 * Shared JSON-backed store base for versioned config payloads.
 *
 * @param <T> the versioned config type
 */
public abstract class MigratingConfigStore<T extends VersionedConfig> extends BaseConfig<T> {
    private final BaseVersionedConfigMigration<T> migration;

    protected MigratingConfigStore(
        @NonNull Path path,
        @NonNull Class<T> valueType,
        @NonNull BaseVersionedConfigMigration<T> migration
    ) {
        super(path, valueType);
        this.migration = migration;
    }

    @Override
    protected final T createDefault() {
        return migration.prepareForSave(null).config();
    }

    @Override
    public synchronized T loadOrCreate() {
        return migrateLoaded(super.loadOrCreate());
    }

    @Override
    public synchronized T reload() {
        return migrateLoaded(super.reload());
    }

    @Override
    public synchronized void save(@NonNull T value) {
        super.save(migration.prepareForSave(value).config());
    }

    private T migrateLoaded(T loadedConfig) {
        BaseVersionedConfigMigration.MigrationResult<T> migrationResult = migration.migrateLoaded(loadedConfig);
        if (migrationResult.changed()) {
            super.save(migrationResult.config());
        }

        return migrationResult.config();
    }
}
