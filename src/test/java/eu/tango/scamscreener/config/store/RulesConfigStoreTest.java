package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.config.migration.ConfigSchema;
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
    void loadOrCreateOverwritesOlderRulesConfigWithCurrentDefaults() throws IOException {
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

        assertEquals(ConfigSchema.RULES.currentVersion(), config.version());
        assertEquals(
            RulesConfig.RuleStageSettings.DEFAULT_EXTERNAL_PLATFORM_PATTERN,
            config.ruleStage().getExternalPlatformPattern()
        );

        String storedJson = Files.readString(rulesFile, StandardCharsets.UTF_8);
        assertEquals(true, storedJson.contains("\"version\": " + ConfigSchema.RULES.currentVersion()));
        assertEquals(true, storedJson.contains("join call"));
    }

    @Test
    void loadOrCreateKeepsCurrentVersionRulesConfigUntouched() throws IOException {
        Path rulesFile = tempDir.resolve("rules.json");
        Files.writeString(
            rulesFile,
            """
            {
              "version": %d,
              "ruleStage": {
                "externalPlatformPattern": "\\\\b(?:discord|telegram|teamspeak)\\\\b"
              }
            }
            """.formatted(ConfigSchema.RULES.currentVersion()),
            StandardCharsets.UTF_8
        );

        RulesConfig config = new RulesConfigStore(rulesFile).loadOrCreate();

        assertEquals(ConfigSchema.RULES.currentVersion(), config.version());
        assertEquals("\\b(?:discord|telegram|teamspeak)\\b", config.ruleStage().getExternalPlatformPattern());
    }
}
