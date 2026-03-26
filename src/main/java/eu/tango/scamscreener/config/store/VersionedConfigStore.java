package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.migration.VersionedConfig;
import lombok.NonNull;

import java.nio.file.Path;

/**
 * Small JSON-backed store for versioned config payloads.
 *
 * @param <T> the versioned config type
 */
public abstract class VersionedConfigStore<T extends VersionedConfig> extends BaseConfig<T> {
    private final int currentVersion;

    protected VersionedConfigStore(
        @NonNull Path path,
        @NonNull Class<T> valueType,
        int currentVersion
    ) {
        super(path, valueType);
        this.currentVersion = Math.max(0, currentVersion);
    }

    @Override
    protected final T createDefault() {
        return stampVersion(createDefaultValue());
    }

    protected abstract T createDefaultValue();

    @Override
    public synchronized T loadOrCreate() {
        return normalizeLoaded(super.loadOrCreate());
    }

    @Override
    public synchronized T reload() {
        return normalizeLoaded(super.reload());
    }

    @Override
    public synchronized void save(@NonNull T value) {
        super.save(stampVersion(value));
    }

    @Override
    public synchronized void saveAsync(@NonNull T value) {
        super.saveAsync(stampVersion(value));
    }

    private T normalizeLoaded(T loadedValue) {
        if (loadedValue == null || loadedValue.version() < currentVersion) {
            T defaultValue = createDefault();
            super.save(defaultValue);
            return defaultValue;
        }

        if (loadedValue.version() == currentVersion) {
            return loadedValue;
        }

        T preparedValue = stampVersion(loadedValue);
        super.save(preparedValue);
        return preparedValue;
    }

    private T stampVersion(T value) {
        T preparedValue = value == null ? createDefaultValue() : value;
        preparedValue.setVersion(currentVersion);
        return preparedValue;
    }
}
