package eu.tango.scamscreener.api;

import eu.tango.scamscreener.config.migration.ConfigSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaticSchemaApiTest {
    @Test
    void exposesCurrentConfigSchemaVersions() {
        StaticSchemaApi api = new StaticSchemaApi();

        assertEquals(ConfigSchema.RUNTIME.currentVersion(), api.runtimeConfigVersion());
        assertEquals(ConfigSchema.RULES.currentVersion(), api.rulesConfigVersion());
        assertEquals(ConfigSchema.WHITELIST.currentVersion(), api.whitelistConfigVersion());
        assertEquals(ConfigSchema.BLACKLIST.currentVersion(), api.blacklistConfigVersion());
        assertEquals(ConfigSchema.REVIEW.currentVersion(), api.reviewConfigVersion());
    }
}
