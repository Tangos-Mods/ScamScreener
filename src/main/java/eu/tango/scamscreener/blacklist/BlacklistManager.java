package eu.tango.scamscreener.blacklist;

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
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class BlacklistManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String DEFAULT_NAME = "unknown";
	private static final String DEFAULT_REASON = "manual-entry";
	private static final int DEFAULT_SCORE = 50;

	private final Path filePath;
	private final Path legacyJsonFilePath;
	private final Path legacyTxtFilePath;
	private final Map<UUID, ScamEntry> entries = new HashMap<>();

	public BlacklistManager() {
		this.filePath = ScamScreenerPaths.inModConfigDir("scam-screener-blacklist.json");
		this.legacyJsonFilePath = ScamScreenerPaths.inRootConfigDir("scam-screener-blacklist.json");
		this.legacyTxtFilePath = ScamScreenerPaths.inRootConfigDir("scam-screener-blacklist.txt");
	}

	public void load() {
		entries.clear();
		if (Files.exists(filePath)) {
			loadJson(filePath);
			return;
		}

		if (Files.exists(legacyJsonFilePath)) {
			loadJson(legacyJsonFilePath);
			save();
			return;
		}

		if (Files.exists(legacyTxtFilePath)) {
			loadLegacyTxt(legacyTxtFilePath);
			save();
		}
	}

	public boolean add(UUID uuid) {
		return add(uuid, DEFAULT_NAME, DEFAULT_SCORE, DEFAULT_REASON);
	}

	public boolean add(UUID uuid, String name, int score, String reason) {
		if (uuid == null || entries.containsKey(uuid)) {
			return false;
		}

		entries.put(uuid, new ScamEntry(uuid, safeName(name), clampScore(score), safeReason(reason), Instant.now().toString()));
		save();
		return true;
	}

	public boolean update(UUID uuid, String name, int score, String reason) {
		if (uuid == null) {
			return false;
		}
		ScamEntry entry = entries.get(uuid);
		if (entry == null) {
			return false;
		}
		entry.name = safeName(name);
		entry.score = clampScore(score);
		entry.reason = safeReason(reason);
		save();
		return true;
	}

	public boolean remove(UUID uuid) {
		if (uuid == null) {
			return false;
		}

		boolean changed = entries.remove(uuid) != null;
		if (changed) {
			save();
		}
		return changed;
	}

	public boolean contains(UUID uuid) {
		return uuid != null && entries.containsKey(uuid);
	}

	public ScamEntry get(UUID uuid) {
		return entries.get(uuid);
	}

	public ScamEntry findByName(String name) {
		if (name == null || name.isBlank()) {
			return null;
		}
		for (ScamEntry entry : entries.values()) {
			if (entry.name != null && entry.name.equalsIgnoreCase(name.trim())) {
				return entry;
			}
		}
		return null;
	}

	public boolean isBlacklisted(String playerName, Function<String, UUID> uuidResolver) {
		if (playerName == null || playerName.isBlank()) {
			return false;
		}

		String normalizedName = playerName.trim();
		UUID uuid = uuidResolver == null ? null : uuidResolver.apply(normalizedName);
		if (uuid != null) {
			return contains(uuid);
		}
		return findByName(normalizedName) != null;
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public Collection<ScamEntry> allEntries() {
		List<ScamEntry> sorted = new ArrayList<>(entries.values());
		sorted.sort(Comparator.comparing((ScamEntry entry) -> entry.name.toLowerCase()).thenComparing(entry -> entry.uuid.toString()));
		return sorted;
	}

	private void loadJson(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			BlacklistFile data = GSON.fromJson(reader, BlacklistFile.class);
			if (data == null || data.entries == null) {
				return;
			}
			for (ScamEntry entry : data.entries) {
				ScamEntry normalized = normalizeEntry(entry);
				if (normalized != null) {
					entries.put(normalized.uuid, normalized);
				}
			}
		} catch (IOException ignored) {
		}
	}

	private void loadLegacyTxt(Path path) {
		try {
			List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
			for (String line : lines) {
				UUID parsed = parseUuid(line);
				if (parsed != null) {
					entries.put(parsed, new ScamEntry(parsed, DEFAULT_NAME, DEFAULT_SCORE, "migrated-from-legacy", Instant.now().toString()));
				}
			}
		} catch (IOException ignored) {
		}
	}

	private void save() {
		try {
			Files.createDirectories(filePath.getParent());
			BlacklistFile data = new BlacklistFile();
			data.version = 1;
			data.entries = new ArrayList<>(allEntries());
			try (Writer writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
				GSON.toJson(data, writer);
			}
		} catch (IOException ignored) {
		}
	}

	private static ScamEntry normalizeEntry(ScamEntry entry) {
		if (entry == null) {
			return null;
		}
		UUID uuid = entry.uuid;
		if (uuid == null) {
			return null;
		}
		String addedAt = entry.addedAt == null || entry.addedAt.isBlank() ? Instant.now().toString() : entry.addedAt;
		return new ScamEntry(uuid, safeName(entry.name), clampScore(entry.score), safeReason(entry.reason), addedAt);
	}

	private static String safeName(String name) {
		if (name == null || name.isBlank()) {
			return DEFAULT_NAME;
		}
		return name.trim();
	}

	private static String safeReason(String reason) {
		if (reason == null || reason.isBlank()) {
			return DEFAULT_REASON;
		}
		return reason.trim();
	}

	private static int clampScore(int score) {
		return Math.max(0, Math.min(100, score));
	}

	private static UUID parseUuid(String input) {
		if (input == null || input.trim().isEmpty()) {
			return null;
		}

		try {
			return UUID.fromString(input.trim());
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private static final class BlacklistFile {
		int version;
		List<ScamEntry> entries;
	}

	public static final class ScamEntry {
		UUID uuid;
		String name;
		int score;
		String reason;
		String addedAt;

		ScamEntry(UUID uuid, String name, int score, String reason, String addedAt) {
			this.uuid = uuid;
			this.name = name;
			this.score = score;
			this.reason = reason;
			this.addedAt = addedAt;
		}

		public UUID uuid() {
			return uuid;
		}

		public String name() {
			return name;
		}

		public int score() {
			return score;
		}

		public String reason() {
			return reason;
		}

		public String addedAt() {
			return addedAt;
		}
	}
}
