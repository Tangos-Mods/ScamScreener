package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.BlacklistConfig;
import eu.tango.scamscreener.config.data.ReviewConfig;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import eu.tango.scamscreener.config.data.WhitelistConfig;
import eu.tango.scamscreener.config.migration.ConfigSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionedConfigStoreMigrationTest {
    @TempDir
    Path tempDir;

    @Test
    void loadOrCreateOverwritesOlderRuntimeConfigWithCurrentDefaults() throws IOException {
        Path runtimeFile = tempDir.resolve("runtime.json");
        Files.writeString(runtimeFile, """
            {
              "version": 1,
              "enabled": false,
              "alerts": {
                "minimumRiskLevel": "HIGH"
              }
            }
            """, StandardCharsets.UTF_8);

        RuntimeConfig config = new RuntimeConfigStore(runtimeFile).loadOrCreate();

        assertEquals(ConfigSchema.RUNTIME.currentVersion(), config.version());
        assertEquals(true, config.isEnabled());
        assertEquals("MEDIUM", config.alerts().minimumRiskLevel().name());
        assertEquals("MEDIUM", config.alerts().autoCaptureLevel().name());
        String storedJson = Files.readString(runtimeFile, StandardCharsets.UTF_8);
        assertTrue(storedJson.contains("\"version\": " + ConfigSchema.RUNTIME.currentVersion()));
        assertTrue(storedJson.contains("\"autoCaptureLevel\": \"MEDIUM\""));
    }

    @Test
    void loadOrCreateKeepsCurrentVersionRuntimeConfigUntouched() throws IOException {
        Path runtimeFile = tempDir.resolve("runtime-current.json");
        Files.writeString(
            runtimeFile,
            """
            {
              "version": %d,
              "enabled": false,
              "alerts": {
                "minimumRiskLevel": "HIGH",
                "autoCaptureLevel": "LOW"
              }
            }
            """.formatted(ConfigSchema.RUNTIME.currentVersion()),
            StandardCharsets.UTF_8
        );

        RuntimeConfig config = new RuntimeConfigStore(runtimeFile).loadOrCreate();

        assertEquals(ConfigSchema.RUNTIME.currentVersion(), config.version());
        assertEquals(false, config.isEnabled());
        assertEquals("HIGH", config.alerts().minimumRiskLevel().name());
        assertEquals("LOW", config.alerts().autoCaptureLevel().name());
        String storedJson = Files.readString(runtimeFile, StandardCharsets.UTF_8);
        assertTrue(storedJson.contains("\"version\": " + ConfigSchema.RUNTIME.currentVersion()));
        assertTrue(storedJson.contains("\"minimumRiskLevel\": \"HIGH\""));
        assertTrue(storedJson.contains("\"autoCaptureLevel\": \"LOW\""));
    }

    @Test
    void loadOrCreateOverwritesUnversionedWhitelistConfigWithCurrentDefaults() throws IOException {
        Path whitelistFile = tempDir.resolve("whitelist.json");
        Files.writeString(whitelistFile, """
            {
              "playerUuids": ["123e4567-e89b-12d3-a456-426614174000"],
              "playerNames": ["Alpha"]
            }
            """, StandardCharsets.UTF_8);

        WhitelistConfig config = new WhitelistConfigStore(whitelistFile).loadOrCreate();

        assertEquals(ConfigSchema.WHITELIST.currentVersion(), config.version());
        assertEquals(0, config.playerUuids().size());
        assertEquals(0, config.playerNames().size());
        assertTrue(Files.readString(whitelistFile, StandardCharsets.UTF_8).contains("\"version\": " + ConfigSchema.WHITELIST.currentVersion()));
    }

    @Test
    void loadOrCreateOverwritesUnversionedBlacklistConfigWithCurrentDefaults() throws IOException {
        Path blacklistFile = tempDir.resolve("blacklist.json");
        Files.writeString(blacklistFile, """
            {
              "entries": [
                {
                  "playerUuid": "123e4567-e89b-12d3-a456-426614174000",
                  "playerName": "Alpha",
                  "score": 75,
                  "reason": "manual",
                  "source": "PLAYER"
                }
              ]
            }
            """, StandardCharsets.UTF_8);

        BlacklistConfig config = new BlacklistConfigStore(blacklistFile).loadOrCreate();

        assertEquals(ConfigSchema.BLACKLIST.currentVersion(), config.version());
        assertEquals(0, config.entries().size());
        assertTrue(Files.readString(blacklistFile, StandardCharsets.UTF_8).contains("\"version\": " + ConfigSchema.BLACKLIST.currentVersion()));
    }

    @Test
    void loadOrCreateOverwritesUnversionedReviewConfigWithCurrentDefaults() throws IOException {
        Path reviewFile = tempDir.resolve("review.json");
        Files.writeString(reviewFile, """
            {
              "entries": [
                {
                  "id": "review-1",
                  "message": "hello",
                  "score": 5
                }
              ]
            }
            """, StandardCharsets.UTF_8);

        ReviewConfig config = new ReviewConfigStore(reviewFile).loadOrCreate();

        assertEquals(ConfigSchema.REVIEW.currentVersion(), config.version());
        assertEquals(0, config.entries().size());
        assertTrue(Files.readString(reviewFile, StandardCharsets.UTF_8).contains("\"version\": " + ConfigSchema.REVIEW.currentVersion()));
    }
}
