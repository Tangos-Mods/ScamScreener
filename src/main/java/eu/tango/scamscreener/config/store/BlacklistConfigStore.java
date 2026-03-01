package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.BlacklistConfig;
import eu.tango.scamscreener.lists.Blacklist;

/**
 * JSON-backed store for the persisted blacklist.
 */
public final class BlacklistConfigStore extends BaseConfig<BlacklistConfig> {
    /**
     * Creates the blacklist config store bound to {@code blacklist.json}.
     */
    public BlacklistConfigStore() {
        super(ConfigPaths.blacklistFile(), BlacklistConfig.class);
    }

    /**
     * Loads the persisted blacklist contents into the provided runtime list.
     *
     * @param blacklist the runtime blacklist to populate
     */
    public void loadInto(Blacklist blacklist) {
        blacklist.replaceAll(loadOrCreate().entries());
    }

    /**
     * Saves the provided blacklist to disk.
     *
     * @param blacklist the runtime blacklist to persist
     */
    public void saveFrom(Blacklist blacklist) {
        BlacklistConfig config = new BlacklistConfig();
        config.getEntries().addAll(blacklist.entries());
        save(config);
    }

    @Override
    protected BlacklistConfig createDefault() {
        return new BlacklistConfig();
    }
}
