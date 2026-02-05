package eu.tango.scamscreener.config;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class ScamScreenerPaths {
	private static final Path CONFIG_ROOT = FabricLoader.getInstance().getConfigDir();
	private static final Path MOD_CONFIG_DIR = CONFIG_ROOT.resolve("scamscreener");

	private ScamScreenerPaths() {
	}

	public static Path inModConfigDir(String filename) {
		return MOD_CONFIG_DIR.resolve(filename);
	}

	public static Path inRootConfigDir(String filename) {
		return CONFIG_ROOT.resolve(filename);
	}
}
