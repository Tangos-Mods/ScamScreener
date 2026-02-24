package eu.tango.scamscreener.config;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class ScamScreenerPaths {
	private static final Path CONFIG_ROOT = resolveConfigRoot();
	private static final Path MOD_CONFIG_DIR = CONFIG_ROOT.resolve("scamscreener");

	private ScamScreenerPaths() {
	}

	public static Path inModConfigDir(String filename) {
		return MOD_CONFIG_DIR.resolve(filename);
	}

	public static Path inRootConfigDir(String filename) {
		return CONFIG_ROOT.resolve(filename);
	}

	private static Path resolveConfigRoot() {
		try {
			Path configDir = FabricLoader.getInstance().getConfigDir();
			if (configDir != null) {
				return configDir;
			}
		} catch (Throwable ignored) {
		}
		String override = System.getProperty("scamscreener.configDir");
		if (override != null && !override.isBlank()) {
			return Path.of(override.trim());
		}
		String tmpDir = System.getProperty("java.io.tmpdir", ".");
		String runId = Long.toUnsignedString(System.nanoTime(), 36);
		return Path.of(tmpDir, "scamscreener-test-" + runId);
	}
}
