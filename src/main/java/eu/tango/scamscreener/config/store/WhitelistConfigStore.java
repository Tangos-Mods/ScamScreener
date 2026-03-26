package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.WhitelistConfig;
import eu.tango.scamscreener.config.migration.ConfigSchema;
import eu.tango.scamscreener.lists.Whitelist;

import java.util.LinkedHashSet;
import java.util.List;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * JSON-backed store for the persisted whitelist.
 */
public final class WhitelistConfigStore extends VersionedConfigStore<WhitelistConfig> {
    /**
     * Creates the whitelist config store bound to {@code whitelist.json}.
     */
    public WhitelistConfigStore() {
        this(ConfigPaths.whitelistFile());
    }

    WhitelistConfigStore(Path path) {
        super(path, WhitelistConfig.class, ConfigSchema.WHITELIST.currentVersion());
    }

    @Override
    protected WhitelistConfig createDefaultValue() {
        return new WhitelistConfig();
    }

    /**
     * Loads the persisted whitelist contents into the provided runtime list.
     *
     * @param whitelist the runtime whitelist to populate
     */
    public void loadInto(Whitelist whitelist) {
        applyToWhitelist(loadOrCreate(), whitelist);
    }

    static void applyToWhitelist(WhitelistConfig config, Whitelist whitelist) {
        if (whitelist == null) {
            return;
        }

        Set<UUID> uuids = new LinkedHashSet<>();
        for (String rawUuid : config == null ? List.<String>of() : config.playerUuids()) {
            if (rawUuid == null || rawUuid.isBlank()) {
                continue;
            }

            try {
                uuids.add(UUID.fromString(rawUuid.trim()));
            } catch (IllegalArgumentException ignored) {
                // Invalid UUID entries are ignored instead of failing the whole file.
            }
        }

        whitelist.replaceAll(uuids, config == null ? List.of() : config.playerNames());
    }

    /**
     * Saves the provided whitelist to disk.
     *
     * @param whitelist the runtime whitelist to persist
     */
    public void saveFrom(Whitelist whitelist) {
        save(fromWhitelist(whitelist));
    }

    /**
     * Saves the provided whitelist to disk on the async config worker.
     *
     * @param whitelist the runtime whitelist to persist
     */
    public void saveFromAsync(Whitelist whitelist) {
        saveAsync(fromWhitelist(whitelist));
    }

    public static WhitelistConfig fromWhitelist(Whitelist whitelist) {
        WhitelistConfig config = new WhitelistConfig();
        if (whitelist == null) {
            return config;
        }

        config.getPlayerUuids().addAll(whitelist.playerUuids().stream().map(UUID::toString).toList());
        config.getPlayerNames().addAll(whitelist.playerNames());
        return config;
    }

}
