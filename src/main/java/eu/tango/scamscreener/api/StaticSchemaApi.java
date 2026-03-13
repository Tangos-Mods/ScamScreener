package eu.tango.scamscreener.api;

import eu.tango.scamscreener.config.migration.ConfigSchema;

/**
 * Static implementation of the public config-schema API.
 */
final class StaticSchemaApi implements ScamScreenerSchemaApi {
    @Override
    public int runtimeConfigVersion() {
        return ConfigSchema.RUNTIME.currentVersion();
    }

    @Override
    public int rulesConfigVersion() {
        return ConfigSchema.RULES.currentVersion();
    }

    @Override
    public int whitelistConfigVersion() {
        return ConfigSchema.WHITELIST.currentVersion();
    }

    @Override
    public int blacklistConfigVersion() {
        return ConfigSchema.BLACKLIST.currentVersion();
    }

    @Override
    public int reviewConfigVersion() {
        return ConfigSchema.REVIEW.currentVersion();
    }
}
