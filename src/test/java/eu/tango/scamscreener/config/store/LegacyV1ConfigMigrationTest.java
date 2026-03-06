package eu.tango.scamscreener.config.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.tango.scamscreener.config.data.BlacklistConfig;
import eu.tango.scamscreener.config.data.WhitelistConfig;
import eu.tango.scamscreener.lists.BlacklistEntry;
import eu.tango.scamscreener.lists.BlacklistSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyV1ConfigMigrationTest {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @TempDir
    Path tempDir;

    @Test
    void migratesLegacyListsAndDeletesLegacyDirectory() throws IOException {
        Path configRoot = tempDir.resolve("config");
        Path v2Directory = configRoot.resolve("scamscreener");
        Path whitelistTarget = v2Directory.resolve("whitelist.json");
        Path blacklistTarget = v2Directory.resolve("blacklist.json");
        Path markerFile = v2Directory.resolve(".v1-to-v2-migration.done");

        Path legacyDirectory = configRoot.resolve("scam-screener");
        Files.createDirectories(legacyDirectory);

        UUID whitelistUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID blacklistUuid = UUID.fromString("223e4567-e89b-12d3-a456-426614174000");
        Files.writeString(
            legacyDirectory.resolve("scamscreener-whitelist.json"),
            """
            {
              "version": 1,
              "entries": [
                { "uuid": "123e4567-e89b-12d3-a456-426614174000", "name": "Alpha" }
              ]
            }
            """.trim(),
            StandardCharsets.UTF_8
        );
        Files.writeString(
            legacyDirectory.resolve("scamscreener-blacklist.json"),
            """
            {
              "version": 1,
              "entries": [
                {
                  "uuid": "223e4567-e89b-12d3-a456-426614174000",
                  "name": "Scammer",
                  "score": 80,
                  "reason": "manual",
                  "source": "PLAYER"
                }
              ]
            }
            """.trim(),
            StandardCharsets.UTF_8
        );

        new LegacyV1ConfigMigration(configRoot, v2Directory, whitelistTarget, blacklistTarget, markerFile).runOnce();

        assertTrue(Files.exists(whitelistTarget));
        assertTrue(Files.exists(blacklistTarget));
        assertTrue(Files.exists(markerFile));
        assertFalse(Files.exists(legacyDirectory));

        WhitelistConfig migratedWhitelist = readJson(whitelistTarget, WhitelistConfig.class);
        assertTrue(migratedWhitelist.playerUuids().contains(whitelistUuid.toString()));
        assertTrue(migratedWhitelist.playerNames().contains("alpha"));

        BlacklistConfig migratedBlacklist = readJson(blacklistTarget, BlacklistConfig.class);
        assertEquals(1, migratedBlacklist.entries().size());
        BlacklistEntry entry = migratedBlacklist.entries().get(0);
        assertEquals(blacklistUuid, entry.playerUuid());
        assertEquals("Scammer", entry.playerName());
        assertEquals(80, entry.score());
        assertEquals(BlacklistSource.PLAYER, entry.source());
    }

    @Test
    void keepsExistingV2WhitelistWhenPresent() throws IOException {
        Path configRoot = tempDir.resolve("config");
        Path v2Directory = configRoot.resolve("scamscreener");
        Path whitelistTarget = v2Directory.resolve("whitelist.json");
        Path blacklistTarget = v2Directory.resolve("blacklist.json");
        Path markerFile = v2Directory.resolve(".v1-to-v2-migration.done");
        Files.createDirectories(v2Directory);

        UUID existingUuid = UUID.fromString("323e4567-e89b-12d3-a456-426614174000");
        WhitelistConfig existingWhitelist = new WhitelistConfig();
        existingWhitelist.getPlayerUuids().add(existingUuid.toString());
        existingWhitelist.getPlayerNames().add("existing");
        writeJson(whitelistTarget, existingWhitelist);

        Path legacyDirectory = configRoot.resolve("scam-screener");
        Files.createDirectories(legacyDirectory);
        Files.writeString(
            legacyDirectory.resolve("scamscreener-whitelist.json"),
            """
            {
              "entries": [
                { "uuid": "423e4567-e89b-12d3-a456-426614174000", "name": "Legacy" }
              ]
            }
            """.trim(),
            StandardCharsets.UTF_8
        );

        new LegacyV1ConfigMigration(configRoot, v2Directory, whitelistTarget, blacklistTarget, markerFile).runOnce();

        WhitelistConfig migratedWhitelist = readJson(whitelistTarget, WhitelistConfig.class);
        assertNotNull(migratedWhitelist);
        assertEquals(1, migratedWhitelist.playerUuids().size());
        assertTrue(migratedWhitelist.playerUuids().contains(existingUuid.toString()));
    }

    private static <T> T readJson(Path path, Class<T> type) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
        }
    }

    private static void writeJson(Path path, Object value) throws IOException {
        Files.createDirectories(path.getParent());
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(value, writer);
        }
    }
}
