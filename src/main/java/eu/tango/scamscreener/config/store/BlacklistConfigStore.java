package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.BlacklistConfig;
import eu.tango.scamscreener.config.migration.ConfigSchema;
import eu.tango.scamscreener.config.migration.SimpleVersionedConfigMigration;
import eu.tango.scamscreener.lists.Blacklist;

import java.nio.file.Path;

/**
 * JSON-backed store for the persisted blacklist.
 */
public final class BlacklistConfigStore extends MigratingConfigStore<BlacklistConfig> {
    private static final SimpleVersionedConfigMigration<BlacklistConfig> MIGRATION =
        new SimpleVersionedConfigMigration<>(ConfigSchema.BLACKLIST, BlacklistConfig::new);

    /**
     * Creates the blacklist config store bound to {@code blacklist.json}.
     */
    public BlacklistConfigStore() {
        this(ConfigPaths.blacklistFile());
    }

    BlacklistConfigStore(Path path) {
        super(path, BlacklistConfig.class, MIGRATION);
    }

    /**
     * Loads the persisted blacklist contents into the provided runtime list.
     *
     * @param blacklist the runtime blacklist to populate
     */
    public void loadInto(Blacklist blacklist) {
        applyToBlacklist(loadOrCreate(), blacklist);
    }

    static void applyToBlacklist(BlacklistConfig config, Blacklist blacklist) {
        if (blacklist == null) {
            return;
        }

        blacklist.replaceAll(config == null ? java.util.List.of() : config.entries());
    }

    /**
     * Saves the provided blacklist to disk.
     *
     * @param blacklist the runtime blacklist to persist
     */
    public void saveFrom(Blacklist blacklist) {
        save(fromBlacklist(blacklist));
    }

    /**
     * Saves the provided blacklist to disk on the async config worker.
     *
     * @param blacklist the runtime blacklist to persist
     */
    public void saveFromAsync(Blacklist blacklist) {
        saveAsync(fromBlacklist(blacklist));
    }

    public static BlacklistConfig fromBlacklist(Blacklist blacklist) {
        BlacklistConfig config = new BlacklistConfig();
        if (blacklist == null) {
            return config;
        }

        config.getEntries().addAll(blacklist.entries());
        return config;
    }

}
