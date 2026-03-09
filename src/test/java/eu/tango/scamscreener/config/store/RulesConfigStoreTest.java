package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.RulesConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RulesConfigStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void loadOrCreateMigratesLegacyDefaultPatternAndPersistsCurrentVersion() throws IOException {
        Path rulesFile = tempDir.resolve("rules.json");
        Files.writeString(rulesFile, """
            {
              "version": 0,
              "ruleStage": {
                "externalPlatformPattern": "\\\\b(?:discord|telegram|whatsapp|instagram|snap(?:chat)?|t\\\\.me|dm me|dm me on discord|direct message me|add me on discord|join my discord|message me on discord|contact me on discord|discord server|server invite|join vc|vc|voice chat|voice channel|call)\\\\b"
              }
            }
            """, StandardCharsets.UTF_8);

        RulesConfig config = new RulesConfigStore(rulesFile).loadOrCreate();

        assertEquals(RulesConfigMigration.CURRENT_VERSION, config.version());
        assertEquals(
            RulesConfig.RuleStageSettings.DEFAULT_EXTERNAL_PLATFORM_PATTERN,
            config.ruleStage().getExternalPlatformPattern()
        );

        String storedJson = Files.readString(rulesFile, StandardCharsets.UTF_8);
        assertEquals(true, storedJson.contains("\"version\": " + RulesConfigMigration.CURRENT_VERSION));
        assertEquals(true, storedJson.contains("join call"));
    }

    @Test
    void loadOrCreatePreservesCustomPatternWhileUpgradingVersion() throws IOException {
        Path rulesFile = tempDir.resolve("rules.json");
        Files.writeString(rulesFile, """
            {
              "version": 0,
              "ruleStage": {
                "externalPlatformPattern": "\\\\b(?:discord|telegram|teamspeak)\\\\b"
              }
            }
            """, StandardCharsets.UTF_8);

        RulesConfig config = new RulesConfigStore(rulesFile).loadOrCreate();

        assertEquals(RulesConfigMigration.CURRENT_VERSION, config.version());
        assertEquals("\\b(?:discord|telegram|teamspeak)\\b", config.ruleStage().getExternalPlatformPattern());
    }
}
