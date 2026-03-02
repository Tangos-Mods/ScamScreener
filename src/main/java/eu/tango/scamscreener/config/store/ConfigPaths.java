package eu.tango.scamscreener.config.store;

import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

/**
 * Centralizes the on-disk config locations used by ScamScreener.
 */
@UtilityClass
public class ConfigPaths {
    private static final String CONFIG_DIRECTORY_NAME = "scamscreener";
    private static final String RUNTIME_FILE_NAME = "runtime.json";
    private static final String RULES_FILE_NAME = "rules.json";
    private static final String WHITELIST_FILE_NAME = "whitelist.json";
    private static final String BLACKLIST_FILE_NAME = "blacklist.json";
    private static final String REVIEW_FILE_NAME = "review.json";

    /**
     * Returns the base config directory for ScamScreener.
     *
     * @return the mod-specific config directory
     */
    public Path baseDirectory() {
        return FabricLoader.getInstance()
            .getConfigDir()
            .resolve(CONFIG_DIRECTORY_NAME);
    }

    /**
     * Returns the path to the runtime config file.
     *
     * @return the runtime config JSON path
     */
    public Path runtimeFile() {
        return baseDirectory().resolve(RUNTIME_FILE_NAME);
    }

    /**
     * Returns the path to the rules config file.
     *
     * @return the rules config JSON path
     */
    public Path rulesFile() {
        return baseDirectory().resolve(RULES_FILE_NAME);
    }

    /**
     * Returns the path to the whitelist file.
     *
     * @return the whitelist JSON path
     */
    public Path whitelistFile() {
        return baseDirectory().resolve(WHITELIST_FILE_NAME);
    }

    /**
     * Returns the path to the blacklist file.
     *
     * @return the blacklist JSON path
     */
    public Path blacklistFile() {
        return baseDirectory().resolve(BLACKLIST_FILE_NAME);
    }

    /**
     * Returns the path to the review queue file.
     *
     * @return the review queue JSON path
     */
    public Path reviewFile() {
        return baseDirectory().resolve(REVIEW_FILE_NAME);
    }
}
