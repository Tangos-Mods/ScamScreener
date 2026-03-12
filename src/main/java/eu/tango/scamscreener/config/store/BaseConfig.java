package eu.tango.scamscreener.config.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import eu.tango.scamscreener.ScamScreenerMod;
import lombok.NonNull;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared JSON-backed base class for file-based config stores.
 *
 * <p>Concrete configs only need to provide their target path and default
 * value. The common file creation, loading, fallback and saving behavior lives
 * here so it does not get reimplemented for every config file.
 *
 * @param <T> the config value type stored in the backing JSON file
 */
public abstract class BaseConfig<T> {
    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();
    private final Path path;
    private final Class<T> valueType;
    private T cachedValue;

    /**
     * Creates a new JSON-backed config store.
     *
     * @param path the target file path for this config
     * @param valueType the concrete type stored in the file
     */
    protected BaseConfig(@NonNull Path path, @NonNull Class<T> valueType) {
        this.path = path;
        this.valueType = valueType;
    }

    /**
     * Loads the config from disk or creates a default file when missing.
     *
     * @return the loaded or default config value
     */
    public synchronized T loadOrCreate() {
        if (cachedValue != null) {
            return cachedValue;
        }

        cachedValue = readOrCreate();
        return cachedValue;
    }

    /**
     * Forces a reload from disk, falling back to defaults when needed.
     *
     * @return the freshly loaded config value
     */
    public synchronized T reload() {
        cachedValue = readOrCreate();
        return cachedValue;
    }

    /**
     * Persists the provided config value and updates the cache.
     *
     * @param value the config value to save
     */
    public synchronized void save(@NonNull T value) {
        writeSerializedValueSync(serializeValue(value));
        cachedValue = value;
    }

    /**
     * Persists the provided config value on the async config worker and updates the cache immediately.
     *
     * @param value the config value to save
     */
    public synchronized void saveAsync(@NonNull T value) {
        writeSerializedValueAsync(serializeValue(value));
        cachedValue = value;
    }

    /**
     * Returns the config file path.
     *
     * @return the backing JSON path
     */
    protected final Path path() {
        return path;
    }

    /**
     * Creates the default value used for missing or invalid files.
     *
     * @return the default config value
     */
    protected abstract T createDefault();

    private T readOrCreate() {
        AsyncFileWorkQueue.flush(path);
        if (Files.notExists(path)) {
            T defaultValue = createDefault();
            writeSerializedValueSync(serializeValue(defaultValue));
            return defaultValue;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            T loadedValue = GSON.fromJson(reader, valueType);
            if (loadedValue != null) {
                return loadedValue;
            }
        } catch (IOException | JsonParseException exception) {
            ScamScreenerMod.LOGGER.warn("Failed to read config from {}. Falling back to defaults.", path, exception);
        }

        T defaultValue = createDefault();
        writeSerializedValueSync(serializeValue(defaultValue));
        return defaultValue;
    }

    private String serializeValue(T value) {
        return GSON.toJson(value);
    }

    private void writeSerializedValueSync(String serializedValue) {
        AsyncFileWorkQueue.submitAndWait(path, serializedValue);
    }

    private void writeSerializedValueAsync(String serializedValue) {
        AsyncFileWorkQueue.submit(path, serializedValue);
    }
}
