package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.WhitelistConfig;
import eu.tango.scamscreener.lists.Whitelist;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JSON-backed store for the persisted whitelist.
 */
public final class WhitelistConfigStore extends BaseConfig<WhitelistConfig> {
    /**
     * Creates the whitelist config store bound to {@code whitelist.json}.
     */
    public WhitelistConfigStore() {
        super(ConfigPaths.whitelistFile(), WhitelistConfig.class);
    }

    /**
     * Loads the persisted whitelist contents into the provided runtime list.
     *
     * @param whitelist the runtime whitelist to populate
     */
    public void loadInto(Whitelist whitelist) {
        WhitelistConfig config = loadOrCreate();
        Set<UUID> uuids = new LinkedHashSet<>();
        for (String rawUuid : config.playerUuids()) {
            if (rawUuid == null || rawUuid.isBlank()) {
                continue;
            }

            try {
                uuids.add(UUID.fromString(rawUuid.trim()));
            } catch (IllegalArgumentException ignored) {
                // Invalid UUID entries are ignored instead of failing the whole file.
            }
        }

        whitelist.replaceAll(uuids, config.playerNames());
    }

    /**
     * Saves the provided whitelist to disk.
     *
     * @param whitelist the runtime whitelist to persist
     */
    public void saveFrom(Whitelist whitelist) {
        WhitelistConfig config = new WhitelistConfig();
        config.getPlayerUuids().addAll(
            whitelist.playerUuids().stream().map(UUID::toString).toList()
        );
        config.getPlayerNames().addAll(whitelist.playerNames());
        save(config);
    }

    @Override
    protected WhitelistConfig createDefault() {
        return new WhitelistConfig();
    }
}
