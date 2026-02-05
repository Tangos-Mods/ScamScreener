package eu.tango.scamscreener.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LocalAiModelConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE_PATH = ScamScreenerPaths.inModConfigDir("scam-screener-local-ai-model.json");
	private static final Path LEGACY_FILE_PATH = ScamScreenerPaths.inRootConfigDir("scam-screener-local-ai-model.json");

	public int version = 1;
	public double intercept = -2.10;
	public double hasPaymentWords = 1.05;
	public double hasAccountWords = 1.30;
	public double hasUrgencyWords = 0.65;
	public double hasTrustWords = 0.45;
	public double hasTooGoodWords = 0.60;
	public double hasPlatformWords = 0.50;
	public double hasLink = 0.95;
	public double hasSuspiciousPunctuation = 0.25;
	public double ctxPushesExternalPlatform = 0.55;
	public double ctxDemandsUpfrontPayment = 0.70;
	public double ctxRequestsSensitiveData = 1.10;
	public double ctxClaimsMiddlemanWithoutProof = 0.45;
	public double ctxRepeatedContact3Plus = 0.35;
	public double ctxIsSpam = 0.30;
	public double ctxAsksForStuff = 0.25;
	public double ctxAdvertising = 0.35;
	public Map<String, Double> tokenWeights = new LinkedHashMap<>();

	public static LocalAiModelConfig loadOrCreate() {
		if (!Files.exists(FILE_PATH)) {
			LocalAiModelConfig migrated = loadFromPath(LEGACY_FILE_PATH);
			if (migrated != null) {
				save(migrated);
				return migrated;
			}

			LocalAiModelConfig defaults = new LocalAiModelConfig();
			save(defaults);
			return defaults;
		}

		LocalAiModelConfig loaded = loadFromPath(FILE_PATH);
		if (loaded == null) {
			return new LocalAiModelConfig();
		}
		if (loaded.tokenWeights == null) {
			loaded.tokenWeights = new LinkedHashMap<>();
		}
		return loaded;
	}

	private static LocalAiModelConfig loadFromPath(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, LocalAiModelConfig.class);
		} catch (IOException ignored) {
			return null;
		}
	}

	public static void save(LocalAiModelConfig config) {
		try {
			Files.createDirectories(FILE_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public static Path filePath() {
		return FILE_PATH;
	}
}
