package eu.tango.scamscreener.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AutoLeaveConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE_PATH = ScamScreenerPaths.inModConfigDir("scam-screener-auto-leave.json");

	public Boolean enabled = false;

	public static AutoLeaveConfig loadOrCreate() {
		if (!Files.exists(FILE_PATH)) {
			AutoLeaveConfig defaults = new AutoLeaveConfig();
			save(defaults);
			return defaults;
		}

		try (Reader reader = Files.newBufferedReader(FILE_PATH, StandardCharsets.UTF_8)) {
			AutoLeaveConfig loaded = GSON.fromJson(reader, AutoLeaveConfig.class);
			if (loaded == null) {
				return new AutoLeaveConfig();
			}
			if (loaded.enabled == null) {
				loaded.enabled = false;
			}
			return loaded;
		} catch (IOException ignored) {
			return new AutoLeaveConfig();
		}
	}

	public static void save(AutoLeaveConfig config) {
		try {
			Files.createDirectories(FILE_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException ignored) {
		}
	}
}
