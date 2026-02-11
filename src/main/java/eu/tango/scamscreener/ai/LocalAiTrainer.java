package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.config.LocalAiModelConfig;
import eu.tango.scamscreener.rules.ScamRules;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class LocalAiTrainer {
	private static final int MAX_VOCAB_SIZE = 3200;
	private static final int MIN_TOKEN_COUNT = 2;
	private static final int ITERATIONS = 1400;
	private static final double LEARNING_RATE = 0.18;
	private static final double L2 = 0.008;
	private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("\\u00A7.");
	private static final String OLD_DIR_NAME = "old";
	private static final String OLD_TRAINING_DIR_NAME = "training-data";
	private static final String OLD_MODELS_DIR_NAME = "models";

	public TrainingResult trainAndSave(Path csvPath) throws IOException {
		int[] ignoredRows = new int[] {0};
		List<Sample> samples = loadAllSamples(csvPath, ignoredRows);
		validateSamples(samples);
		int preservedModelVersion = currentModelVersion();

		List<String> vocab = TokenFeatureExtractor.buildVocab(samples, MAX_VOCAB_SIZE, MIN_TOKEN_COUNT);
		Map<String, Integer> vocabIndex = new HashMap<>();
		for (int i = 0; i < vocab.size(); i++) {
			vocabIndex.put(vocab.get(i), i);
		}

		Map<String, Integer> messageFrequency = new HashMap<>();
		Map<String, Integer> windowFrequency = new HashMap<>();
		double mainPositives = 0.0;
		double mainNegatives = 0.0;
		double funnelPositives = 0.0;
		double funnelNegatives = 0.0;
		for (Sample sample : samples) {
			messageFrequency.merge(sample.message(), 1, Integer::sum);
			windowFrequency.merge(sample.windowId(), 1, Integer::sum);
			if (sample.label() == 1) {
				mainPositives += 1.0;
			} else {
				mainNegatives += 1.0;
			}
			if (sample.funnelLabel()) {
				funnelPositives += 1.0;
			} else {
				funnelNegatives += 1.0;
			}
		}
		double mainPosClassWeight = mainPositives <= 0.0 ? 1.0 : (mainPositives + mainNegatives) / (2.0 * mainPositives);
		double mainNegClassWeight = mainNegatives <= 0.0 ? 1.0 : (mainPositives + mainNegatives) / (2.0 * mainNegatives);
		double funnelPosClassWeight = funnelPositives <= 0.0 ? 1.0 : (funnelPositives + funnelNegatives) / (2.0 * funnelPositives);
		double funnelNegClassWeight = funnelNegatives <= 0.0 ? 1.0 : (funnelPositives + funnelNegatives) / (2.0 * funnelNegatives);

		int mainDenseCount = AiFeatureSpace.DENSE_FEATURE_NAMES.size();
		int mainFeatureCount = mainDenseCount + vocab.size();
		List<SampleVector> mainVectors = new ArrayList<>(samples.size());
		for (Sample sample : samples) {
			double[] features = vectorizeMain(sample, vocabIndex, mainDenseCount, mainFeatureCount);
			double effectiveWeight = effectiveWeight(
				sample,
				messageFrequency,
				windowFrequency,
				mainPosClassWeight,
				mainNegClassWeight,
				sample.label()
			);
			mainVectors.add(new SampleVector(features, sample.label(), effectiveWeight));
		}
		LinearModel mainModel = trainLinear(mainVectors, mainFeatureCount);

		int funnelDenseCount = AiFeatureSpace.FUNNEL_DENSE_FEATURE_NAMES.size();
		List<SampleVector> funnelVectors = new ArrayList<>(samples.size());
		boolean hasFunnelPositive = false;
		boolean hasFunnelNegative = false;
		for (Sample sample : samples) {
			double[] features = vectorizeFunnel(sample, funnelDenseCount);
			int funnelLabel = sample.funnelLabel() ? 1 : 0;
			if (funnelLabel == 1) {
				hasFunnelPositive = true;
			} else {
				hasFunnelNegative = true;
			}
			double effectiveWeight = effectiveWeight(
				sample,
				messageFrequency,
				windowFrequency,
				funnelPosClassWeight,
				funnelNegClassWeight,
				funnelLabel
			);
			funnelVectors.add(new SampleVector(features, funnelLabel, effectiveWeight));
		}
		if (!hasFunnelPositive || !hasFunnelNegative) {
			funnelVectors.clear();
			for (Sample sample : samples) {
				double[] features = vectorizeFunnel(sample, funnelDenseCount);
				double effectiveWeight = effectiveWeight(
					sample,
					messageFrequency,
					windowFrequency,
					mainPosClassWeight,
					mainNegClassWeight,
					sample.label()
				);
				funnelVectors.add(new SampleVector(features, sample.label(), effectiveWeight));
			}
		}
		LinearModel funnelModel = trainLinear(funnelVectors, funnelDenseCount);

		LocalAiModelConfig model = new LocalAiModelConfig();
		model.version = preservedModelVersion;
		model.intercept = mainModel.intercept();
		model.denseFeatureWeights = new LinkedHashMap<>();
		for (int i = 0; i < mainDenseCount; i++) {
			model.denseFeatureWeights.put(AiFeatureSpace.DENSE_FEATURE_NAMES.get(i), mainModel.weights()[i]);
		}
		model.tokenWeights = new LinkedHashMap<>();
		for (int i = 0; i < vocab.size(); i++) {
			model.tokenWeights.put(vocab.get(i), mainModel.weights()[mainDenseCount + i]);
		}
		model.funnelHead = new LocalAiModelConfig.DenseHeadConfig();
		model.funnelHead.intercept = funnelModel.intercept();
		model.funnelHead.denseFeatureWeights = new LinkedHashMap<>();
		for (int i = 0; i < funnelDenseCount; i++) {
			model.funnelHead.denseFeatureWeights.put(AiFeatureSpace.FUNNEL_DENSE_FEATURE_NAMES.get(i), funnelModel.weights()[i]);
		}

		archiveExistingModelFile();
		LocalAiModelConfig.save(model);
		Path archivedPath = archiveTrainingData(csvPath);

		long positiveCount = samples.stream().filter(sample -> sample.label() == 1).count();
		return new TrainingResult(samples.size(), (int) positiveCount, archivedPath, ignoredRows[0]);
	}

	private static double[] vectorizeMain(Sample sample, Map<String, Integer> vocabIndex, int denseCount, int featureCount) {
		double[] vector = new double[featureCount];
		Map<String, Double> dense = AiFeatureSpace.extractDenseFeatures(sample.context());
		for (int i = 0; i < denseCount; i++) {
			String name = AiFeatureSpace.DENSE_FEATURE_NAMES.get(i);
			vector[i] = dense.getOrDefault(name, 0.0);
		}

		Set<String> tokens = TokenFeatureExtractor.extractFeatureTokens(sample.message());
		for (String token : tokens) {
			Integer idx = vocabIndex.get(token);
			if (idx != null) {
				vector[denseCount + idx] = 1.0;
			}
		}
		return vector;
	}

	private static double[] vectorizeFunnel(Sample sample, int funnelDenseCount) {
		double[] vector = new double[funnelDenseCount];
		Map<String, Double> dense = AiFeatureSpace.extractDenseFeatures(sample.context());
		for (int i = 0; i < funnelDenseCount; i++) {
			String name = AiFeatureSpace.FUNNEL_DENSE_FEATURE_NAMES.get(i);
			vector[i] = dense.getOrDefault(name, 0.0);
		}
		return vector;
	}

	private static LinearModel trainLinear(List<SampleVector> samples, int featureCount) {
		if (featureCount <= 0) {
			return new LinearModel(0.0, new double[0]);
		}
		double[] weights = new double[featureCount];
		double intercept = 0.0;
		if (samples == null || samples.isEmpty()) {
			return new LinearModel(intercept, weights);
		}

		for (int iteration = 0; iteration < ITERATIONS; iteration++) {
			double learningRate = LEARNING_RATE / Math.sqrt(1.0 + (iteration * 0.02));
			for (SampleVector sample : samples) {
				double[] features = sample.features();
				if (features == null || features.length != featureCount) {
					continue;
				}
				double linear = intercept;
				for (int i = 0; i < featureCount; i++) {
					linear += weights[i] * features[i];
				}
				double probability = sigmoid(linear);
				double error = (probability - sample.label()) * sample.weight();

				intercept -= learningRate * error;
				for (int i = 0; i < featureCount; i++) {
					double xi = features[i];
					if (xi == 0.0 && weights[i] == 0.0) {
						continue;
					}
					double gradient = (error * xi) + (L2 * weights[i]);
					weights[i] -= learningRate * gradient;
				}
			}
		}

		return new LinearModel(intercept, weights);
	}

	private static double effectiveWeight(
		Sample sample,
		Map<String, Integer> messageFrequency,
		Map<String, Integer> windowFrequency,
		double posClassWeight,
		double negClassWeight,
		int label
	) {
		double classWeight = label == 1 ? posClassWeight : negClassWeight;
		double duplicateWeight = 1.0 / Math.sqrt(messageFrequency.getOrDefault(sample.message(), 1));
		double windowWeight = 1.0 / Math.sqrt(windowFrequency.getOrDefault(sample.windowId(), 1));
		double effectiveWeight = sample.baseWeight() * classWeight * duplicateWeight * windowWeight;
		if (sample.hardNegative()) {
			effectiveWeight *= 1.15;
		}
		return effectiveWeight;
	}

	private static Path archiveTrainingData(Path csvPath) throws IOException {
		Path archiveDir = csvPath.resolveSibling(OLD_DIR_NAME).resolve(OLD_TRAINING_DIR_NAME);
		Path target = nextArchiveTarget(csvPath, archiveDir);
		return Files.move(csvPath, target);
	}

	private static List<Sample> loadAllSamples(Path csvPath, int[] ignoredRows) throws IOException {
		List<Path> sources = new ArrayList<>();
		if (Files.exists(csvPath)) {
			sources.add(csvPath);
		}
		sources.addAll(findArchivedTrainingFiles(csvPath));
		if (sources.isEmpty()) {
			throw new IOException("Training file not found: " + csvPath);
		}

		List<Sample> all = new ArrayList<>();
		for (Path source : sources) {
			all.addAll(loadSamples(source, ignoredRows));
		}
		return all;
	}

	private static List<Path> findArchivedTrainingFiles(Path csvPath) throws IOException {
		Path dir = csvPath.getParent();
		if (dir == null || !Files.isDirectory(dir)) {
			return List.of();
		}

		String base = csvPath.getFileName().toString();
		List<Path> matches = new ArrayList<>();
		matches.addAll(listArchiveCandidates(dir, base));
		matches.addAll(listArchiveCandidates(dir.resolve(OLD_DIR_NAME).resolve(OLD_TRAINING_DIR_NAME), base));
		matches.sort(Comparator.comparingInt(LocalAiTrainer::archiveIndex));
		return matches;
	}

	private static int archiveIndex(Path archivedPath) {
		String name = archivedPath.getFileName().toString();
		int lastDot = name.lastIndexOf('.');
		if (lastDot < 0 || lastDot + 1 >= name.length()) {
			return Integer.MAX_VALUE;
		}
		return parseInt(name.substring(lastDot + 1), Integer.MAX_VALUE);
	}

	private static void archiveExistingModelFile() throws IOException {
		Path modelPath = LocalAiModelConfig.filePath();
		if (!Files.exists(modelPath)) {
			return;
		}
		Path archiveDir = modelPath.resolveSibling(OLD_DIR_NAME).resolve(OLD_MODELS_DIR_NAME);
		Path target = nextArchiveTarget(modelPath, archiveDir);
		Files.copy(modelPath, target, StandardCopyOption.COPY_ATTRIBUTES);
	}

	private static int currentModelVersion() {
		try {
			LocalAiModelConfig existing = LocalAiModelConfig.loadOrCreate();
			if (existing != null && existing.version >= 9) {
				return existing.version;
			}
		} catch (Exception ignored) {
		}
		return new LocalAiModelConfig().version;
	}

	private static List<Path> listArchiveCandidates(Path dir, String baseName) throws IOException {
		if (dir == null || !Files.isDirectory(dir)) {
			return List.of();
		}
		List<Path> matches = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, baseName + ".old.*")) {
			for (Path candidate : stream) {
				String name = candidate.getFileName().toString();
				if (!name.matches(".*\\.old\\.\\d+$")) {
					continue;
				}
				if (Files.isRegularFile(candidate)) {
					matches.add(candidate);
				}
			}
		}
		return matches;
	}

	private static Path nextArchiveTarget(Path baseFile, Path archiveDir) throws IOException {
		Files.createDirectories(archiveDir);
		int index = 1;
		Path target = archiveDir.resolve(baseFile.getFileName() + ".old." + index);
		while (Files.exists(target)) {
			index++;
			target = archiveDir.resolve(baseFile.getFileName() + ".old." + index);
		}
		return target;
	}

	private static List<Sample> loadSamples(Path csvPath, int[] ignoredRows) throws IOException {
		if (!Files.exists(csvPath)) {
			throw new IOException("Training file not found: " + csvPath);
		}

		List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
		if (lines.size() < 2) {
			throw new IOException("Training file has no samples.");
		}

		Map<String, Integer> index = headerIndex(lines.get(0));
		Integer messageCol = index.get("message");
		Integer labelCol = index.get("label");
		if (messageCol == null || labelCol == null) {
			throw new IOException("Training header must contain message,label");
		}

		List<Sample> samples = new ArrayList<>();
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.isEmpty()) {
				continue;
			}

			List<String> cols = parseCsvLine(line);
			String message = normalizeTrainingMessage(value(cols, messageCol, ""));
			if (message.isBlank()) {
				continue;
			}
			if (countTokens(message) <= 1) {
				if (ignoredRows != null && ignoredRows.length > 0) {
					ignoredRows[0]++;
				}
				continue;
			}

			int label = parseInt(value(cols, labelCol, ""), -1);
			if (label != 0 && label != 1) {
				continue;
			}

			String windowId = value(cols, index.get("window_id"), "unknown");
			double baseWeight = parseDouble(value(cols, index.get("sample_weight"), "1"), 1.0);
			boolean hardNegative = parseBinary(value(cols, index.get("hard_negative"), "0"));
			ScamRules.BehaviorContext context = parseBehaviorContext(message, cols, index);
			boolean funnelLabel = deriveFunnelLabel(context);

			samples.add(new Sample(message, label, context, baseWeight, windowId, hardNegative, funnelLabel));
		}
		return samples;
	}

	private static boolean deriveFunnelLabel(ScamRules.BehaviorContext context) {
		if (context == null) {
			return false;
		}
		if (context.funnelFullChain() || context.funnelPartialChain()) {
			return true;
		}
		if (context.funnelStepIndex() > 0 || context.funnelSequenceScore() > 0.0 || context.funnelHits() > 0) {
			return true;
		}
		return false;
	}

	private static ScamRules.BehaviorContext parseBehaviorContext(String message, List<String> cols, Map<String, Integer> index) {
		String channel = value(cols, index.get("channel"), "unknown");
		long deltaMs = parseLong(value(cols, index.get("delta_ms"), "0"), 0L);
		boolean pushesExternalPlatform = parseBinary(value(cols, index.get("pushes_external_platform"), "0"));
		boolean demandsUpfrontPayment = parseBinary(value(cols, index.get("demands_upfront_payment"), "0"));
		boolean requestsSensitiveData = parseBinary(value(cols, index.get("requests_sensitive_data"), "0"));
		boolean claimsMiddleman = parseBinary(value(cols, index.get("claims_middleman_without_proof"), "0"));
		int repeatedContactAttempts = parseInt(value(cols, index.get("repeated_contact_attempts"), "0"), 0);
		boolean tooGood = parseBinary(value(cols, index.get("too_good_to_be_true"), "0"));
		boolean isSpam = parseBinary(value(cols, index.get("is_spam"), "0"));
		boolean asksForStuff = parseBinary(value(cols, index.get("asks_for_stuff"), "0"));
		boolean advertising = parseBinary(value(cols, index.get("advertising"), "0"));

		boolean intentOffer = parseBinary(value(cols, index.get("intent_offer"), "0"));
		boolean intentRep = parseBinary(value(cols, index.get("intent_rep"), "0"));
		boolean intentRedirect = parseBinary(value(cols, index.get("intent_redirect"), "0"));
		boolean intentInstruction = parseBinary(value(cols, index.get("intent_instruction"), "0"));
		boolean intentPayment = parseBinary(value(cols, index.get("intent_payment"), "0"));
		boolean intentAnchor = parseBinary(value(cols, index.get("intent_anchor"), "0"));
		int funnelStepIndex = parseInt(value(cols, index.get("funnel_step_index"), "0"), 0);
		double funnelSequenceScore = parseDouble(value(cols, index.get("funnel_sequence_score"), "0"), 0.0);
		boolean funnelFullChain = parseBinary(value(cols, index.get("funnel_full_chain"), "0"));
		boolean funnelPartialChain = parseBinary(value(cols, index.get("funnel_partial_chain"), "0"));
		int ruleHits = parseInt(value(cols, index.get("rule_hits"), "0"), 0);
		int similarityHits = parseInt(value(cols, index.get("similarity_hits"), "0"), 0);
		int behaviorHits = parseInt(value(cols, index.get("behavior_hits"), "0"), 0);
		int trendHits = parseInt(value(cols, index.get("trend_hits"), "0"), 0);
		int funnelHits = parseInt(value(cols, index.get("funnel_hits"), "0"), 0);

		return new ScamRules.BehaviorContext(
			message,
			channel,
			deltaMs,
			pushesExternalPlatform,
			demandsUpfrontPayment,
			requestsSensitiveData,
			claimsMiddleman,
			repeatedContactAttempts,
			tooGood,
			isSpam,
			asksForStuff,
			advertising,
			intentOffer,
			intentRep,
			intentRedirect,
			intentInstruction,
			intentPayment,
			intentAnchor,
			funnelStepIndex,
			funnelSequenceScore,
			funnelFullChain,
			funnelPartialChain,
			ruleHits,
			similarityHits,
			behaviorHits,
			trendHits,
			funnelHits
		);
	}

	private static Map<String, Integer> headerIndex(String headerLine) {
		List<String> header = parseCsvLine(headerLine);
		Map<String, Integer> index = new HashMap<>();
		for (int i = 0; i < header.size(); i++) {
			String key = header.get(i);
			if (key == null || key.isBlank()) {
				continue;
			}
			index.put(key.trim().toLowerCase(Locale.ROOT), i);
		}
		return index;
	}

	private static String value(List<String> cols, Integer index, String fallback) {
		if (index == null || index < 0 || index >= cols.size()) {
			return fallback;
		}
		String value = cols.get(index);
		return value == null ? fallback : value;
	}

	private static String normalizeTrainingMessage(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		String stripped = COLOR_CODE_PATTERN.matcher(raw).replaceAll("").trim().toLowerCase(Locale.ROOT);
		String cleaned = stripped.replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ");
		return cleaned.trim();
	}

	private static int countTokens(String text) {
		int count = 0;
		for (String token : TokenFeatureExtractor.wordSequence(text)) {
			count++;
			if (count > 1) {
				return count;
			}
		}
		return count;
	}

	private static void validateSamples(List<Sample> samples) throws IOException {
		if (samples.size() < 12) {
			throw new IOException("Not enough samples. Need at least 12.");
		}
		boolean hasZero = false;
		boolean hasOne = false;
		for (Sample sample : samples) {
			if (sample.label() == 0) {
				hasZero = true;
			} else if (sample.label() == 1) {
				hasOne = true;
			}
		}
		if (!hasZero || !hasOne) {
			throw new IOException("Need both labels 0 and 1 in training data.");
		}
	}

	private static List<String> parseCsvLine(String line) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					current.append('"');
					i++;
				} else {
					inQuotes = !inQuotes;
				}
				continue;
			}
			if (c == ',' && !inQuotes) {
				values.add(current.toString());
				current.setLength(0);
				continue;
			}
			current.append(c);
		}
		values.add(current.toString());
		return values;
	}

	private static boolean parseBinary(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}
		try {
			return Integer.parseInt(value.trim()) > 0;
		} catch (NumberFormatException ignored) {
			String normalized = value.trim().toLowerCase(Locale.ROOT);
			return "true".equals(normalized) || "yes".equals(normalized);
		}
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static long parseLong(String value, long fallback) {
		try {
			return Long.parseLong(value.trim());
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static double parseDouble(String value, double fallback) {
		try {
			double parsed = Double.parseDouble(value.trim());
			if (Double.isFinite(parsed)) {
				return parsed;
			}
			return fallback;
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static double sigmoid(double x) {
		double clamped = Math.max(-30.0, Math.min(30.0, x));
		return 1.0 / (1.0 + Math.exp(-clamped));
	}

	static record Sample(
		String message,
		int label,
		ScamRules.BehaviorContext context,
		double baseWeight,
		String windowId,
		boolean hardNegative,
		boolean funnelLabel
	) {
	}

	private record SampleVector(double[] features, int label, double weight) {
	}

	private record LinearModel(double intercept, double[] weights) {
	}

	public record TrainingResult(int sampleCount, int positiveCount, Path archivedDataPath, int ignoredUnigrams) {
	}
}
