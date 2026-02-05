package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.config.LocalAiModelConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LocalAiTrainer {
	private static final String[] PAYMENT_WORDS = {"pay", "payment", "vorkasse", "coins", "money", "btc", "crypto"};
	private static final String[] ACCOUNT_WORDS = {"password", "passwort", "2fa", "code", "email", "login"};
	private static final String[] URGENCY_WORDS = {"now", "quick", "fast", "urgent", "sofort", "jetzt"};
	private static final String[] TRUST_WORDS = {"trust", "legit", "safe", "trusted", "middleman"};
	private static final String[] TOO_GOOD_WORDS = {"free", "100%", "guaranteed", "garantiert", "dupe", "rank"};
	private static final String[] PLATFORM_WORDS = {"discord", "telegram", "t.me", "server", "dm"};

	private static final int BASE_FEATURE_COUNT = 17;
	private static final int MAX_VOCAB_SIZE = 200;
	private static final int MIN_TOKEN_COUNT = 2;
	private static final int ITERATIONS = 1200;
	private static final double LEARNING_RATE = 0.22;
	private static final double L2 = 0.01;
	private static final Pattern CHAT_LINE_PATTERN = Pattern.compile("^.*?([A-Za-z0-9_]{3,16})\\s*:\\s*(.+)$");
	private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§.");
	private static final String OLD_DIR_NAME = "old";
	private static final String OLD_TRAINING_DIR_NAME = "training-data";
	private static final String OLD_MODELS_DIR_NAME = "models";

	public TrainingResult trainAndSave(Path csvPath) throws IOException {
		List<Sample> samples = loadAllSamples(csvPath);
		validateSamples(samples);

		List<String> vocab = TokenFeatureExtractor.buildVocab(samples, MAX_VOCAB_SIZE, MIN_TOKEN_COUNT);
		int featureCount = BASE_FEATURE_COUNT + vocab.size();
		double[] weights = new double[featureCount];
		double bias = 0.0;
		int sampleCount = samples.size();

		for (int iter = 0; iter < ITERATIONS; iter++) {
			double[] gradW = new double[featureCount];
			double gradB = 0.0;

			for (Sample sample : samples) {
				double[] features = buildFeatures(sample, vocab);
				double z = bias;
				for (int i = 0; i < featureCount; i++) {
					z += weights[i] * features[i];
				}
				double pred = sigmoid(z);
				double err = pred - sample.label();

				for (int i = 0; i < featureCount; i++) {
					gradW[i] += err * features[i];
				}
				gradB += err;
			}

			for (int i = 0; i < featureCount; i++) {
				gradW[i] = (gradW[i] / sampleCount) + (L2 * weights[i]);
				weights[i] -= LEARNING_RATE * gradW[i];
			}
			bias -= LEARNING_RATE * (gradB / sampleCount);
		}

		LocalAiModelConfig model = new LocalAiModelConfig();
		model.version = 4;
		model.intercept = bias;
		model.hasPaymentWords = weights[0];
		model.hasAccountWords = weights[1];
		model.hasUrgencyWords = weights[2];
		model.hasTrustWords = weights[3];
		model.hasTooGoodWords = weights[4];
		model.hasPlatformWords = weights[5];
		model.hasLink = weights[6];
		model.hasSuspiciousPunctuation = weights[7];
		model.ctxPushesExternalPlatform = weights[8];
		model.ctxDemandsUpfrontPayment = weights[9];
		model.ctxRequestsSensitiveData = weights[10];
		model.ctxClaimsMiddlemanWithoutProof = weights[11];
		model.ctxTooGoodToBeTrue = weights[12];
		model.ctxRepeatedContact3Plus = weights[13];
		model.ctxIsSpam = weights[14];
		model.ctxAsksForStuff = weights[15];
		model.ctxAdvertising = weights[16];
		model.tokenWeights = new LinkedHashMap<>();
		for (int i = 0; i < vocab.size(); i++) {
			model.tokenWeights.put(vocab.get(i), weights[BASE_FEATURE_COUNT + i]);
		}
		archiveExistingModelFile();
		LocalAiModelConfig.save(model);
		Path archivedPath = archiveTrainingData(csvPath);

		long positive = samples.stream().filter(s -> s.label() == 1).count();
		return new TrainingResult(samples.size(), (int) positive, archivedPath);
	}

	private static double[] buildFeatures(Sample sample, List<String> vocab) {
		String msg = sample.message() == null ? "" : sample.message().toLowerCase(Locale.ROOT);
		double[] features = new double[BASE_FEATURE_COUNT + vocab.size()];
		features[0] = bool(hasAny(msg, PAYMENT_WORDS));
		features[1] = bool(hasAny(msg, ACCOUNT_WORDS));
		features[2] = bool(hasAny(msg, URGENCY_WORDS));
		features[3] = bool(hasAny(msg, TRUST_WORDS));
		features[4] = bool(hasAny(msg, TOO_GOOD_WORDS));
		features[5] = bool(hasAny(msg, PLATFORM_WORDS));
		features[6] = bool(hasLink(msg));
		features[7] = bool(hasSuspiciousPunctuation(msg));
		features[8] = clamp01(sample.pushesExternalPlatform());
		features[9] = clamp01(sample.demandsUpfrontPayment());
		features[10] = clamp01(sample.requestsSensitiveData());
		features[11] = clamp01(sample.claimsMiddlemanWithoutProof());
		features[12] = clamp01(sample.tooGoodToBeTrue());
		features[13] = sample.repeatedContactAttempts() >= 3 ? 1.0 : 0.0;
		features[14] = clamp01(sample.isSpam());
		features[15] = clamp01(sample.asksForStuff());
		features[16] = clamp01(sample.advertising());

		if (vocab.isEmpty()) {
			return features;
		}

		Set<String> tokens = TokenFeatureExtractor.extractFeatureTokens(msg);
		for (int i = 0; i < vocab.size(); i++) {
			if (tokens.contains(vocab.get(i))) {
				features[BASE_FEATURE_COUNT + i] = 1.0;
			}
		}
		return features;
	}

	private static Path archiveTrainingData(Path csvPath) throws IOException {
		Path archiveDir = csvPath.resolveSibling(OLD_DIR_NAME).resolve(OLD_TRAINING_DIR_NAME);
		Path target = nextArchiveTarget(csvPath, archiveDir);
		return Files.move(csvPath, target);
	}

	private static List<Sample> loadAllSamples(Path csvPath) throws IOException {
		List<Path> sources = new ArrayList<>();
		if (Files.exists(csvPath)) {
			sources.add(csvPath);
		}

		sources.addAll(findArchivedTrainingFiles(csvPath));
		if (sources.isEmpty()) {
			throw new IOException("Training file not found: " + csvPath);
		}

		List<Sample> allSamples = new ArrayList<>();
		for (Path source : sources) {
			allSamples.addAll(loadSamples(source));
		}
		return allSamples;
	}

	private static List<Path> findArchivedTrainingFiles(Path csvPath) throws IOException {
		Path dir = csvPath.getParent();
		if (dir == null || !Files.isDirectory(dir)) {
			return List.of();
		}

		String base = csvPath.getFileName().toString();
		List<Path> matches = new ArrayList<>();
		matches.addAll(listArchiveCandidates(dir, base));
		// New archive location under config/scamscreener/old/training-data
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

	private static List<Sample> loadSamples(Path csvPath) throws IOException {
		if (!Files.exists(csvPath)) {
			throw new IOException("Training file not found: " + csvPath);
		}

		List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
		if (lines.size() < 2) {
			throw new IOException("Training file has no samples.");
		}

		List<Sample> samples = new ArrayList<>();
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i).trim();
			if (line.isEmpty()) {
				continue;
			}

			List<String> cols = parseCsvLine(line);
			if (cols.size() < 7) {
				continue;
			}

			String message = extractTrainingMessage(cols.get(0));
			if (message.isBlank()) {
				continue;
			}
			int label = parseInt(cols.get(1), -1);
			TrainingFlags.Values flags = TrainingFlags.Values.fromCsv(cols, 2);
			if (label != 0 && label != 1) {
				continue;
			}

			samples.add(new Sample(
				message,
				label,
				flags.get(TrainingFlags.Flag.PUSHES_EXTERNAL_PLATFORM),
				flags.get(TrainingFlags.Flag.DEMANDS_UPFRONT_PAYMENT),
				flags.get(TrainingFlags.Flag.REQUESTS_SENSITIVE_DATA),
				flags.get(TrainingFlags.Flag.CLAIMS_MIDDLEMAN_WITHOUT_PROOF),
				flags.get(TrainingFlags.Flag.TOO_GOOD_TO_BE_TRUE),
				flags.get(TrainingFlags.Flag.REPEATED_CONTACT_ATTEMPTS),
				flags.get(TrainingFlags.Flag.IS_SPAM),
				flags.get(TrainingFlags.Flag.ASKS_FOR_STUFF),
				flags.get(TrainingFlags.Flag.ADVERTISING)
			));
		}
		return samples;
	}

	private static String extractTrainingMessage(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}

		String stripped = COLOR_CODE_PATTERN.matcher(raw).replaceAll("").trim();
		if (stripped.startsWith("[ScamScreener]")) {
			return "";
		}

		Matcher chatMatcher = CHAT_LINE_PATTERN.matcher(stripped);
		if (chatMatcher.matches()) {
			return chatMatcher.group(2).trim();
		}
		return stripped;
	}

	private static void validateSamples(List<Sample> samples) throws IOException {
		if (samples.size() < 8) {
			throw new IOException("Not enough samples. Need at least 8.");
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

	private static boolean hasAny(String text, String[] words) {
		for (String word : words) {
			if (text.contains(word)) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasLink(String text) {
		return text.contains("http://") || text.contains("https://") || text.contains("www.");
	}

	private static boolean hasSuspiciousPunctuation(String text) {
		return text.contains("!!!") || text.contains("??") || text.contains("$$");
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static double clamp01(int value) {
		return value > 0 ? 1.0 : 0.0;
	}

	private static double bool(boolean value) {
		return value ? 1.0 : 0.0;
	}

	private static double sigmoid(double x) {
		double clamped = Math.max(-30.0, Math.min(30.0, x));
		return 1.0 / (1.0 + Math.exp(-clamped));
	}

	static record Sample(
		String message,
		int label,
		int pushesExternalPlatform,
		int demandsUpfrontPayment,
		int requestsSensitiveData,
		int claimsMiddlemanWithoutProof,
		int tooGoodToBeTrue,
		int repeatedContactAttempts,
		int isSpam,
		int asksForStuff,
		int advertising
	) {
	}

	public record TrainingResult(int sampleCount, int positiveCount, Path archivedDataPath) {
	}
}
