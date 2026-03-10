package eu.tango.scamscreener.config.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.config.data.BlacklistConfig;
import eu.tango.scamscreener.config.data.WhitelistConfig;
import eu.tango.scamscreener.config.store.BlacklistConfigStore;
import eu.tango.scamscreener.config.store.ConfigPaths;
import eu.tango.scamscreener.config.store.WhitelistConfigStore;
import eu.tango.scamscreener.lists.Blacklist;
import eu.tango.scamscreener.lists.BlacklistEntry;
import eu.tango.scamscreener.lists.BlacklistSource;
import eu.tango.scamscreener.lists.Whitelist;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * One-time migration for legacy v1 config artifacts.
 *
 * <p>The migration imports legacy blacklist/whitelist files and removes old
 * v1-only config folders afterwards. A marker file guarantees this runs only
 * once per installation.
 */
public final class LegacyV1ConfigMigration {
    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create();
    private static final List<String> LEGACY_WHITELIST_FILE_NAMES = List.of(
        "scamscreener-whitelist.json",
        "scam-screener-whitelist.json"
    );
    private static final List<String> LEGACY_BLACKLIST_FILE_NAMES = List.of(
        "scamscreener-blacklist.json",
        "scam-screener-blacklist.json"
    );
    private static final List<String> LEGACY_BLACKLIST_TEXT_FILE_NAMES = List.of(
        "scam-screener-blacklist.txt"
    );
    private static final List<String> LEGACY_DIRECTORY_NAMES = List.of(
        "scam-screener",
        "scamscreener-v1"
    );

    private final Path configRootDirectory;
    private final Path v2BaseDirectory;
    private final Path whitelistTargetFile;
    private final Path blacklistTargetFile;
    private final Path migrationMarkerFile;

    public static void runDefaultOnce() {
        new LegacyV1ConfigMigration(
            ConfigPaths.configRootDirectory(),
            ConfigPaths.baseDirectory(),
            ConfigPaths.whitelistFile(),
            ConfigPaths.blacklistFile(),
            ConfigPaths.v1MigrationMarkerFile()
        ).runOnce();
    }

    public LegacyV1ConfigMigration(
        Path configRootDirectory,
        Path v2BaseDirectory,
        Path whitelistTargetFile,
        Path blacklistTargetFile,
        Path migrationMarkerFile
    ) {
        this.configRootDirectory = normalize(configRootDirectory);
        this.v2BaseDirectory = normalize(v2BaseDirectory);
        this.whitelistTargetFile = normalize(whitelistTargetFile);
        this.blacklistTargetFile = normalize(blacklistTargetFile);
        this.migrationMarkerFile = normalize(migrationMarkerFile);
    }

    public void runOnce() {
        if (Files.exists(migrationMarkerFile)) {
            return;
        }

        try {
            Files.createDirectories(v2BaseDirectory);
            migrateWhitelist();
            migrateBlacklist();
            cleanupLegacyArtifacts();
            writeMigrationMarker();
        } catch (Exception exception) {
            ScamScreenerMod.LOGGER.warn("Legacy v1 config migration failed.", exception);
        }
    }

    private void migrateWhitelist() throws IOException {
        if (Files.exists(whitelistTargetFile)) {
            return;
        }

        Set<UUID> playerUuids = new LinkedHashSet<>();
        Set<String> playerNames = new LinkedHashSet<>();

        for (Path sourceFile : findLegacyFiles(LEGACY_WHITELIST_FILE_NAMES)) {
            collectWhitelistEntries(sourceFile, playerUuids, playerNames);
        }

        if (playerUuids.isEmpty() && playerNames.isEmpty()) {
            return;
        }

        Whitelist whitelist = new Whitelist();
        for (UUID playerUuid : playerUuids) {
            whitelist.add(playerUuid, null);
        }
        for (String playerName : playerNames) {
            whitelist.add(null, playerName);
        }

        WhitelistConfig migratedConfig = new SimpleVersionedConfigMigration<>(ConfigSchema.WHITELIST, WhitelistConfig::new)
            .prepareForSave(WhitelistConfigStore.fromWhitelist(whitelist))
            .config();
        writeJson(whitelistTargetFile, migratedConfig);
        int totalEntries = migratedConfig.playerUuids().size() + migratedConfig.playerNames().size();
        ScamScreenerMod.LOGGER.info("Migrated {} whitelist entries from legacy v1 config files.", totalEntries);
    }

    private void migrateBlacklist() throws IOException {
        if (Files.exists(blacklistTargetFile)) {
            return;
        }

        Blacklist blacklist = new Blacklist();
        for (Path sourceFile : findLegacyFiles(LEGACY_BLACKLIST_FILE_NAMES)) {
            for (BlacklistEntry entry : readBlacklistEntries(sourceFile)) {
                blacklist.add(entry.playerUuid(), entry.playerName(), entry.score(), entry.reason(), entry.source());
            }
        }

        for (Path sourceFile : findLegacyFiles(LEGACY_BLACKLIST_TEXT_FILE_NAMES)) {
            for (UUID playerUuid : readLegacyBlacklistText(sourceFile)) {
                blacklist.add(playerUuid, "unknown", 50, "migrated-from-v1", BlacklistSource.PLAYER);
            }
        }

        if (blacklist.isEmpty()) {
            return;
        }

        BlacklistConfig migratedConfig = new SimpleVersionedConfigMigration<>(ConfigSchema.BLACKLIST, BlacklistConfig::new)
            .prepareForSave(BlacklistConfigStore.fromBlacklist(blacklist))
            .config();
        writeJson(blacklistTargetFile, migratedConfig);
        ScamScreenerMod.LOGGER.info("Migrated {} blacklist entries from legacy v1 config files.", migratedConfig.entries().size());
    }

    private void cleanupLegacyArtifacts() {
        Set<Path> legacyFiles = new LinkedHashSet<>();
        legacyFiles.addAll(findLegacyFiles(LEGACY_WHITELIST_FILE_NAMES));
        legacyFiles.addAll(findLegacyFiles(LEGACY_BLACKLIST_FILE_NAMES));
        legacyFiles.addAll(findLegacyFiles(LEGACY_BLACKLIST_TEXT_FILE_NAMES));

        for (Path legacyFile : legacyFiles) {
            if (legacyFile.equals(whitelistTargetFile) || legacyFile.equals(blacklistTargetFile)) {
                continue;
            }

            try {
                Files.deleteIfExists(legacyFile);
            } catch (IOException exception) {
                ScamScreenerMod.LOGGER.warn("Could not delete legacy config file {}.", legacyFile, exception);
            }
        }

        for (Path legacyDirectory : legacyDirectories()) {
            if (!Files.isDirectory(legacyDirectory)) {
                continue;
            }

            try {
                deleteDirectoryRecursively(legacyDirectory);
                ScamScreenerMod.LOGGER.info("Deleted legacy v1 config directory {}.", legacyDirectory);
            } catch (IOException exception) {
                ScamScreenerMod.LOGGER.warn("Could not delete legacy config directory {}.", legacyDirectory, exception);
            }
        }
    }

    private void writeMigrationMarker() throws IOException {
        Files.createDirectories(migrationMarkerFile.getParent());
        Files.writeString(
            migrationMarkerFile,
            "completedAt=" + Instant.now() + System.lineSeparator(),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private List<Path> findLegacyFiles(List<String> fileNames) {
        Set<Path> files = new LinkedHashSet<>();
        for (Path directory : legacySearchDirectories()) {
            for (String fileName : fileNames) {
                Path file = normalize(directory.resolve(fileName));
                if (Files.isRegularFile(file)) {
                    files.add(file);
                }
            }
        }
        return new ArrayList<>(files);
    }

    private List<Path> legacySearchDirectories() {
        Set<Path> directories = new LinkedHashSet<>();
        directories.add(configRootDirectory);
        directories.add(v2BaseDirectory);
        directories.addAll(legacyDirectories());
        return new ArrayList<>(directories);
    }

    private List<Path> legacyDirectories() {
        List<Path> directories = new ArrayList<>();
        for (String directoryName : LEGACY_DIRECTORY_NAMES) {
            Path directory = normalize(configRootDirectory.resolve(directoryName));
            if (!directory.equals(v2BaseDirectory)) {
                directories.add(directory);
            }
        }
        return directories;
    }

    private void collectWhitelistEntries(Path sourceFile, Set<UUID> playerUuids, Set<String> playerNames) {
        JsonElement root = readJson(sourceFile);
        if (root == null) {
            return;
        }

        if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            collectWhitelistUuidArray(object.get("playerUuids"), playerUuids);
            collectWhitelistNameArray(object.get("playerNames"), playerNames);
            collectWhitelistEntriesArray(object.get("entries"), playerUuids, playerNames);
            return;
        }

        if (root.isJsonArray()) {
            collectWhitelistEntriesArray(root.getAsJsonArray(), playerUuids, playerNames);
        }
    }

    private void collectWhitelistEntriesArray(JsonElement entriesElement, Set<UUID> playerUuids, Set<String> playerNames) {
        if (entriesElement == null || !entriesElement.isJsonArray()) {
            return;
        }

        collectWhitelistEntriesArray(entriesElement.getAsJsonArray(), playerUuids, playerNames);
    }

    private void collectWhitelistEntriesArray(JsonArray entries, Set<UUID> playerUuids, Set<String> playerNames) {
        for (JsonElement entryElement : entries) {
            if (entryElement == null || entryElement.isJsonNull()) {
                continue;
            }

            if (entryElement.isJsonPrimitive()) {
                String rawValue = asString(entryElement);
                if (rawValue == null || rawValue.isBlank()) {
                    continue;
                }

                UUID parsedUuid = parseUuid(rawValue);
                if (parsedUuid != null) {
                    playerUuids.add(parsedUuid);
                } else {
                    String normalizedName = normalizeName(rawValue);
                    if (!normalizedName.isEmpty()) {
                        playerNames.add(normalizedName);
                    }
                }
                continue;
            }

            if (!entryElement.isJsonObject()) {
                continue;
            }

            JsonObject object = entryElement.getAsJsonObject();
            UUID parsedUuid = parseUuid(firstString(object, "uuid", "playerUuid", "player_uuid"));
            if (parsedUuid != null) {
                playerUuids.add(parsedUuid);
            }

            String name = normalizeName(firstString(object, "name", "playerName", "player_name"));
            if (!name.isEmpty()) {
                playerNames.add(name);
            }
        }
    }

    private void collectWhitelistUuidArray(JsonElement uuidArrayElement, Set<UUID> playerUuids) {
        if (uuidArrayElement == null || !uuidArrayElement.isJsonArray()) {
            return;
        }

        for (JsonElement element : uuidArrayElement.getAsJsonArray()) {
            String rawUuid = asString(element);
            UUID parsedUuid = parseUuid(rawUuid);
            if (parsedUuid != null) {
                playerUuids.add(parsedUuid);
            }
        }
    }

    private void collectWhitelistNameArray(JsonElement nameArrayElement, Set<String> playerNames) {
        if (nameArrayElement == null || !nameArrayElement.isJsonArray()) {
            return;
        }

        for (JsonElement element : nameArrayElement.getAsJsonArray()) {
            String normalizedName = normalizeName(asString(element));
            if (!normalizedName.isEmpty()) {
                playerNames.add(normalizedName);
            }
        }
    }

    private List<BlacklistEntry> readBlacklistEntries(Path sourceFile) {
        JsonElement root = readJson(sourceFile);
        if (root == null) {
            return List.of();
        }

        List<BlacklistEntry> entries = new ArrayList<>();
        if (root.isJsonObject()) {
            JsonObject object = root.getAsJsonObject();
            JsonElement entriesElement = object.get("entries");
            if (entriesElement != null && entriesElement.isJsonArray()) {
                collectBlacklistEntries(entriesElement.getAsJsonArray(), entries);
            } else {
                BlacklistEntry parsedEntry = parseBlacklistEntry(object);
                if (parsedEntry != null) {
                    entries.add(parsedEntry);
                }
            }
            return entries;
        }

        if (root.isJsonArray()) {
            collectBlacklistEntries(root.getAsJsonArray(), entries);
        }

        return entries;
    }

    private void collectBlacklistEntries(JsonArray sourceEntries, List<BlacklistEntry> targetEntries) {
        for (JsonElement element : sourceEntries) {
            if (element == null || !element.isJsonObject()) {
                continue;
            }

            BlacklistEntry parsedEntry = parseBlacklistEntry(element.getAsJsonObject());
            if (parsedEntry != null) {
                targetEntries.add(parsedEntry);
            }
        }
    }

    private BlacklistEntry parseBlacklistEntry(JsonObject source) {
        if (source == null) {
            return null;
        }

        UUID playerUuid = parseUuid(firstString(source, "playerUuid", "player_uuid", "uuid"));
        String playerName = normalizeName(firstString(source, "playerName", "player_name", "name"));

        if (playerUuid == null && playerName.isEmpty()) {
            return null;
        }

        int score = clampScore(parseInt(firstString(source, "score"), 50));
        String reason = normalizeReason(firstString(source, "reason"));
        BlacklistSource sourceType = parseBlacklistSource(firstString(source, "source"));
        return new BlacklistEntry(playerUuid, playerName, score, reason, sourceType);
    }

    private List<UUID> readLegacyBlacklistText(Path sourceFile) {
        if (!Files.isRegularFile(sourceFile)) {
            return List.of();
        }

        List<UUID> uuids = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(sourceFile, StandardCharsets.UTF_8)) {
                UUID parsedUuid = parseUuid(line);
                if (parsedUuid != null) {
                    uuids.add(parsedUuid);
                }
            }
        } catch (IOException exception) {
            ScamScreenerMod.LOGGER.warn("Could not read legacy blacklist text file {}.", sourceFile, exception);
        }

        return uuids;
    }

    private JsonElement readJson(Path sourceFile) {
        try (Reader reader = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (IOException | JsonParseException exception) {
            ScamScreenerMod.LOGGER.warn("Could not parse legacy config file {}.", sourceFile, exception);
            return null;
        }
    }

    private void writeJson(Path targetFile, Object value) throws IOException {
        Files.createDirectories(targetFile.getParent());
        try (Writer writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8)) {
            GSON.toJson(value, writer);
        }
    }

    private static void deleteDirectoryRecursively(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static Path normalize(Path path) {
        return path == null ? Path.of(".") : path.toAbsolutePath().normalize();
    }

    private static String firstString(JsonObject object, String... keys) {
        if (object == null || keys == null) {
            return null;
        }

        for (String key : keys) {
            if (key == null || !object.has(key)) {
                continue;
            }

            String value = asString(object.get(key));
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return null;
    }

    private static String asString(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return null;
        }

        try {
            return value.getAsString();
        } catch (UnsupportedOperationException | ClassCastException | IllegalStateException ignored) {
            return null;
        }
    }

    private static UUID parseUuid(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return UUID.fromString(rawValue.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static int parseInt(String rawValue, int defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private static String normalizeName(String rawValue) {
        if (rawValue == null) {
            return "";
        }

        return rawValue.trim();
    }

    private static String normalizeReason(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "migrated-from-v1";
        }

        return rawValue.trim();
    }

    private static BlacklistSource parseBlacklistSource(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return BlacklistSource.PLAYER;
        }

        try {
            return BlacklistSource.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BlacklistSource.PLAYER;
        }
    }
}
