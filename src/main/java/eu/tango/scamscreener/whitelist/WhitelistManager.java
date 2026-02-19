package eu.tango.scamscreener.whitelist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.tango.scamscreener.config.ScamScreenerPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class WhitelistManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String DEFAULT_NAME = "unknown";

	private final Path filePath;
	private final Path legacyFilePath;
	private final Map<UUID, WhitelistEntry> entries = new HashMap<>();

	public WhitelistManager() {
		this(
			ScamScreenerPaths.inModConfigDir("scam-screener-whitelist.json"),
			ScamScreenerPaths.inRootConfigDir("scam-screener-whitelist.json")
		);
	}

	WhitelistManager(Path filePath, Path legacyFilePath) {
		this.filePath = filePath;
		this.legacyFilePath = legacyFilePath;
	}

	public synchronized void load() {
		entries.clear();
		if (Files.exists(filePath)) {
			loadJson(filePath);
			return;
		}
		if (Files.exists(legacyFilePath)) {
			loadJson(legacyFilePath);
			save();
		}
	}

	public synchronized AddOrUpdateResult addOrUpdate(UUID uuid, String displayName) {
		if (uuid == null) {
			return AddOrUpdateResult.INVALID;
		}

		String safeName = safeName(displayName);
		WhitelistEntry existing = entries.get(uuid);
		if (existing == null) {
			entries.put(uuid, new WhitelistEntry(uuid, safeName, Instant.now().toString()));
			save();
			return AddOrUpdateResult.ADDED;
		}

		if (existing.name.equals(safeName)) {
			return AddOrUpdateResult.UNCHANGED;
		}
		existing.name = safeName;
		save();
		return AddOrUpdateResult.UPDATED;
	}

	public synchronized boolean remove(UUID uuid) {
		if (uuid == null) {
			return false;
		}
		boolean changed = entries.remove(uuid) != null;
		if (changed) {
			save();
		}
		return changed;
	}

	public synchronized WhitelistEntry removeByName(String name) {
		if (name == null || name.isBlank()) {
			return null;
		}
		String normalized = name.trim().toLowerCase(Locale.ROOT);
		UUID matchingUuid = null;
		WhitelistEntry removedEntry = null;
		for (Map.Entry<UUID, WhitelistEntry> entry : entries.entrySet()) {
			WhitelistEntry value = entry.getValue();
			if (value == null || value.name == null || value.name.isBlank()) {
				continue;
			}
			if (!value.name.trim().toLowerCase(Locale.ROOT).equals(normalized)) {
				continue;
			}
			matchingUuid = entry.getKey();
			removedEntry = value;
			break;
		}
		if (matchingUuid == null) {
			return null;
		}
		entries.remove(matchingUuid);
		save();
		return removedEntry;
	}

	public synchronized boolean contains(UUID uuid) {
		return uuid != null && entries.containsKey(uuid);
	}

	public synchronized WhitelistEntry get(UUID uuid) {
		return uuid == null ? null : entries.get(uuid);
	}

	public synchronized WhitelistEntry findByName(String name) {
		if (name == null || name.isBlank()) {
			return null;
		}
		String normalized = name.trim().toLowerCase(Locale.ROOT);
		for (WhitelistEntry entry : entries.values()) {
			if (entry == null || entry.name == null || entry.name.isBlank()) {
				continue;
			}
			if (entry.name.trim().toLowerCase(Locale.ROOT).equals(normalized)) {
				return entry;
			}
		}
		return null;
	}

	public synchronized Collection<WhitelistEntry> allEntries() {
		List<WhitelistEntry> sorted = new ArrayList<>(entries.values());
		sorted.sort(Comparator
			.comparing((WhitelistEntry entry) -> safeName(entry == null ? null : entry.name).toLowerCase(Locale.ROOT))
			.thenComparing(entry -> entry == null || entry.uuid == null ? new UUID(0L, 0L) : entry.uuid));
		return sorted;
	}

	public synchronized boolean isEmpty() {
		return entries.isEmpty();
	}

	public synchronized boolean isWhitelisted(String playerName, Function<String, UUID> uuidResolver) {
		if (playerName == null || playerName.isBlank()) {
			return false;
		}
		String normalizedName = playerName.trim();
		UUID resolvedUuid = uuidResolver == null ? null : uuidResolver.apply(normalizedName);
		if (resolvedUuid != null && contains(resolvedUuid)) {
			return true;
		}
		return findByName(normalizedName) != null;
	}

	public synchronized boolean updateDisplayName(UUID uuid, String canonicalName) {
		if (uuid == null || canonicalName == null || canonicalName.isBlank()) {
			return false;
		}
		WhitelistEntry existing = entries.get(uuid);
		if (existing == null) {
			return false;
		}
		String safeName = safeName(canonicalName);
		if (existing.name.equals(safeName)) {
			return false;
		}
		existing.name = safeName;
		save();
		return true;
	}

	private void loadJson(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			WhitelistFile data = GSON.fromJson(reader, WhitelistFile.class);
			if (data == null || data.entries == null) {
				return;
			}
			for (WhitelistEntry entry : data.entries) {
				WhitelistEntry normalized = normalizeEntry(entry);
				if (normalized != null) {
					entries.put(normalized.uuid, normalized);
				}
			}
		} catch (IOException ignored) {
		}
	}

	private void save() {
		try {
			Files.createDirectories(filePath.getParent());
			WhitelistFile data = new WhitelistFile();
			data.version = 1;
			data.entries = new ArrayList<>(allEntries());
			try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
				GSON.toJson(data, writer);
			}
		} catch (IOException ignored) {
		}
	}

	private static WhitelistEntry normalizeEntry(WhitelistEntry entry) {
		if (entry == null || entry.uuid == null) {
			return null;
		}
		String addedAt = entry.addedAt == null || entry.addedAt.isBlank() ? Instant.now().toString() : entry.addedAt;
		return new WhitelistEntry(entry.uuid, safeName(entry.name), addedAt);
	}

	private static String safeName(String input) {
		if (input == null || input.isBlank()) {
			return DEFAULT_NAME;
		}
		return input.trim();
	}

	private static final class WhitelistFile {
		int version;
		List<WhitelistEntry> entries;
	}

	public enum AddOrUpdateResult {
		ADDED,
		UPDATED,
		UNCHANGED,
		INVALID
	}

	public static final class WhitelistEntry {
		UUID uuid;
		String name;
		String addedAt;

		WhitelistEntry(UUID uuid, String name, String addedAt) {
			this.uuid = uuid;
			this.name = name;
			this.addedAt = addedAt;
		}

		public UUID uuid() {
			return uuid;
		}

		public String name() {
			return name;
		}

		public String addedAt() {
			return addedAt;
		}
	}
}
