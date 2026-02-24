package eu.tango.scamscreener.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.tango.scamscreener.ai.AiFeatureSpace;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LocalAiModelConfig {
	public static final int MODEL_SCHEMA_VERSION = 10;
	public static final int DEFAULT_MAX_TOKEN_WEIGHTS = 5_000;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE_PATH = ScamScreenerPaths.inModConfigDir("scam-screener-local-ai-model.json");

	public int version = MODEL_SCHEMA_VERSION;
	public double intercept = -2.25;
	public Map<String, Double> denseFeatureWeights = new LinkedHashMap<>(AiFeatureSpace.defaultDenseWeights());
	public Map<String, Double> tokenWeights = new LinkedHashMap<>();
	public int maxTokenWeights = DEFAULT_MAX_TOKEN_WEIGHTS;
	public DenseHeadConfig funnelHead = DenseHeadConfig.defaultFunnelHead();

	public static LocalAiModelConfig loadOrCreate() {
		if (!Files.exists(FILE_PATH)) {
			LocalAiModelConfig defaults = new LocalAiModelConfig();
			save(defaults);
			return defaults;
		}

		LocalAiModelConfig loaded = loadFromPath(FILE_PATH);
		if (loaded == null) {
			return new LocalAiModelConfig();
		}
		if (loaded.version < MODEL_SCHEMA_VERSION) {
			loaded.version = MODEL_SCHEMA_VERSION;
		}
		if (loaded.denseFeatureWeights == null || loaded.denseFeatureWeights.isEmpty()) {
			loaded.denseFeatureWeights = new LinkedHashMap<>(AiFeatureSpace.defaultDenseWeights());
		} else {
			for (Map.Entry<String, Double> entry : AiFeatureSpace.defaultDenseWeights().entrySet()) {
				loaded.denseFeatureWeights.putIfAbsent(entry.getKey(), entry.getValue());
			}
		}
		loaded.maxTokenWeights = normalizeMaxTokenWeights(loaded.maxTokenWeights);
		if (loaded.tokenWeights == null) {
			loaded.tokenWeights = new LinkedHashMap<>();
		}
		loaded.tokenWeights = pruneTokenWeights(loaded.tokenWeights, loaded.maxTokenWeights);
		if (loaded.funnelHead == null) {
			loaded.funnelHead = DenseHeadConfig.fromMainHead(loaded.intercept, loaded.denseFeatureWeights);
		} else {
			loaded.funnelHead.normalizeFromMain(loaded.intercept, loaded.denseFeatureWeights);
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
		if (config == null) {
			return;
		}
		config.version = Math.max(MODEL_SCHEMA_VERSION, config.version);
		config.maxTokenWeights = normalizeMaxTokenWeights(config.maxTokenWeights);
		config.tokenWeights = pruneTokenWeights(config.tokenWeights, config.maxTokenWeights);
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

	public static int normalizeMaxTokenWeights(int rawValue) {
		if (rawValue < 500 || rawValue > 50_000) {
			return DEFAULT_MAX_TOKEN_WEIGHTS;
		}
		return rawValue;
	}

	public static Map<String, Double> pruneTokenWeights(Map<String, Double> tokenWeights, int maxTokenWeights) {
		if (tokenWeights == null || tokenWeights.isEmpty()) {
			return new LinkedHashMap<>();
		}
		int normalizedLimit = normalizeMaxTokenWeights(maxTokenWeights);
		List<Map.Entry<String, Double>> entries = new ArrayList<>();
		for (Map.Entry<String, Double> entry : tokenWeights.entrySet()) {
			if (entry == null || entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
				continue;
			}
			if (!Double.isFinite(entry.getValue())) {
				continue;
			}
			entries.add(Map.entry(entry.getKey(), entry.getValue()));
		}
		if (entries.isEmpty()) {
			return new LinkedHashMap<>();
		}
		entries.sort(
			Comparator.<Map.Entry<String, Double>>comparingDouble(entry -> Math.abs(entry.getValue())).reversed()
				.thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER)
		);
		int keep = Math.min(normalizedLimit, entries.size());
		Map<String, Double> pruned = new LinkedHashMap<>(keep);
		for (int i = 0; i < keep; i++) {
			Map.Entry<String, Double> entry = entries.get(i);
			pruned.put(entry.getKey(), entry.getValue());
		}
		return pruned;
	}

	public static final class DenseHeadConfig {
		public double intercept = -2.25;
		public Map<String, Double> denseFeatureWeights = new LinkedHashMap<>(AiFeatureSpace.defaultFunnelDenseWeights());

		public DenseHeadConfig() {
		}

		public static DenseHeadConfig defaultFunnelHead() {
			DenseHeadConfig head = new DenseHeadConfig();
			head.intercept = -2.25;
			head.denseFeatureWeights = new LinkedHashMap<>(AiFeatureSpace.defaultFunnelDenseWeights());
			return head;
		}

		public static DenseHeadConfig fromMainHead(double mainIntercept, Map<String, Double> mainDense) {
			DenseHeadConfig head = defaultFunnelHead();
			head.intercept = mainIntercept;
			head.normalizeFromMain(mainIntercept, mainDense);
			return head;
		}

		public void normalizeFromMain(double fallbackIntercept, Map<String, Double> mainDense) {
			if (denseFeatureWeights == null) {
				denseFeatureWeights = new LinkedHashMap<>();
			}
			Map<String, Double> defaults = AiFeatureSpace.defaultFunnelDenseWeights();
			for (Map.Entry<String, Double> entry : defaults.entrySet()) {
				String key = entry.getKey();
				if (denseFeatureWeights.containsKey(key)) {
					continue;
				}
				double fallbackWeight = 0.0;
				if (mainDense != null) {
					Double fromMain = mainDense.get(key);
					if (fromMain != null) {
						fallbackWeight = fromMain;
					}
				}
				if (fallbackWeight == 0.0) {
					fallbackWeight = entry.getValue();
				}
				denseFeatureWeights.put(key, fallbackWeight);
			}
			denseFeatureWeights.entrySet().removeIf(entry -> !AiFeatureSpace.isFunnelDenseFeature(entry.getKey()));
			if (!Double.isFinite(intercept)) {
				intercept = fallbackIntercept;
			}
		}
	}
}
