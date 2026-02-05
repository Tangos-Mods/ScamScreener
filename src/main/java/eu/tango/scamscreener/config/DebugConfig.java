package eu.tango.scamscreener.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DebugConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE_PATH = ScamScreenerPaths.inModConfigDir("scam-screener-debug.json");

	public boolean updater;
	public boolean party;
	public boolean trade;
	public boolean mute;

	public static DebugConfig loadOrCreate() {
		if (!Files.exists(FILE_PATH)) {
			DebugConfig defaults = new DebugConfig();
			save(defaults);
			return defaults;
		}

		DebugConfig loaded = loadFromPath(FILE_PATH);
		if (loaded == null) {
			return new DebugConfig();
		}
		return loaded;
	}

	private static DebugConfig loadFromPath(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, DebugConfig.class);
		} catch (IOException ignored) {
			return null;
		}
	}

	public static void save(DebugConfig config) {
		try {
			Files.createDirectories(FILE_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException ignored) {
		}
	}
}
