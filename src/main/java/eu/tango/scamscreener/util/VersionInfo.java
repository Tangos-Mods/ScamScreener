package eu.tango.scamscreener.util;

import eu.tango.scamscreener.config.LocalAiModelConfig;
import net.fabricmc.loader.api.FabricLoader;

public final class VersionInfo {
	private VersionInfo() {
	}

	public static String modVersion() {
		try {
			return FabricLoader.getInstance()
				.getModContainer("scam-screener")
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("unknown");
		} catch (Throwable ignored) {
			return "unknown";
		}
	}

	public static int aiModelVersion() {
		try {
			return LocalAiModelConfig.loadOrCreate().version;
		} catch (Throwable ignored) {
			return -1;
		}
	}
}
