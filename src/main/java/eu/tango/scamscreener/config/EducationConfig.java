package eu.tango.scamscreener.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class EducationConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public Set<String> disabledMessages = new LinkedHashSet<>();

	public static EducationConfig loadOrCreate() {
		Path configPath = filePath();
		if (!Files.exists(configPath)) {
			EducationConfig defaults = new EducationConfig();
			save(defaults);
			return defaults;
		}

		EducationConfig loaded = loadFromPath(configPath);
		if (loaded == null) {
			return new EducationConfig();
		}
		return loaded.normalize();
	}

	private static EducationConfig loadFromPath(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, EducationConfig.class);
		} catch (IOException ignored) {
			return null;
		}
	}

	public static void save(EducationConfig config) {
		Path configPath = filePath();
		try {
			Files.createDirectories(configPath.getParent());
			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				GSON.toJson(config == null ? new EducationConfig() : config.normalize(), writer);
			}
		} catch (IOException ignored) {
		}
	}

	public boolean isDisabled(String messageId) {
		if (messageId == null || messageId.isBlank()) {
			return false;
		}
		return disabledMessages.contains(normalizeId(messageId));
	}

	public boolean disable(String messageId) {
		if (messageId == null || messageId.isBlank()) {
			return false;
		}
		return disabledMessages.add(normalizeId(messageId));
	}

	private EducationConfig normalize() {
		if (disabledMessages == null) {
			disabledMessages = new LinkedHashSet<>();
			return this;
		}
		Set<String> normalized = new LinkedHashSet<>();
		for (String id : disabledMessages) {
			String value = normalizeId(id);
			if (!value.isBlank()) {
				normalized.add(value);
			}
		}
		disabledMessages = normalized;
		return this;
	}

	private static String normalizeId(String id) {
		return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
	}

	private static Path filePath() {
		return ScamScreenerPaths.inModConfigDir("scamscreener-edu.json");
	}
}
