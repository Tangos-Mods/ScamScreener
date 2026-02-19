package eu.tango.scamscreener.config;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Paths;
import java.nio.file.Path;

public final class ScamScreenerPaths {
	private ScamScreenerPaths() {
	}

	public static Path inModConfigDir(String filename) {
		return configRoot().resolve("scamscreener").resolve(filename);
	}

	public static Path inRootConfigDir(String filename) {
		return configRoot().resolve(filename);
	}

	private static Path configRoot() {
		try {
			Path configDir = FabricLoader.getInstance().getConfigDir();
			if (configDir != null) {
				return configDir;
			}
		} catch (Exception ignored) {
		}
		return Paths.get("config");
	}
}
