package eu.tango.scamscreener.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MutePatternsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE_PATH = ScamScreenerPaths.inModConfigDir("scam-screener-mute.json");

	public List<String> patterns = new ArrayList<>();
	public boolean notifyEnabled = true;
	public int notifyIntervalSeconds = 30;

	public static MutePatternsConfig loadOrCreate() {
		if (!Files.exists(FILE_PATH)) {
			MutePatternsConfig defaults = new MutePatternsConfig();
			save(defaults);
			return defaults;
		}

		try (Reader reader = Files.newBufferedReader(FILE_PATH, StandardCharsets.UTF_8)) {
			MutePatternsConfig loaded = GSON.fromJson(reader, MutePatternsConfig.class);
			if (loaded == null) {
				return new MutePatternsConfig();
			}
			if (loaded.patterns == null) {
				loaded.patterns = new ArrayList<>();
			}
			if (loaded.notifyIntervalSeconds < 5 || loaded.notifyIntervalSeconds > 600) {
				loaded.notifyIntervalSeconds = 30;
			}
			return loaded;
		} catch (IOException ignored) {
			return new MutePatternsConfig();
		}
	}

	public static void save(MutePatternsConfig config) {
		try {
			Files.createDirectories(FILE_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException ignored) {
		}
	}
}
