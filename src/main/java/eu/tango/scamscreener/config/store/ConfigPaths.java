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
    private static final String TRAINING_CASES_V2_FILE_NAME = "training-cases-v2.jsonl";
    private static final String V1_MIGRATION_MARKER_FILE_NAME = ".v1-to-v2-migration.done";

    /**
     * Returns the global Fabric config root directory.
     *
     * @return the config root path
     */
    public Path configRootDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    /**
     * Returns the base config directory for ScamScreener.
     *
     * @return the mod-specific config directory
     */
    public Path baseDirectory() {
        return configRootDirectory()
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

    /**
     * Returns the path to the canonical training-case export file.
     *
     * @return the training-case JSONL path
     */
    public Path trainingCasesV2File() {
        return baseDirectory().resolve(TRAINING_CASES_V2_FILE_NAME);
    }

    /**
     * Returns the marker file used to guarantee one-time legacy migration.
     *
     * @return the migration marker path
     */
    public Path v1MigrationMarkerFile() {
        return baseDirectory().resolve(V1_MIGRATION_MARKER_FILE_NAME);
    }
}
