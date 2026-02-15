package eu.tango.scamscreener.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class LocalAiTrainer {
	private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("\\u00A7.");
	private static final String OLD_DIR_NAME = "old";
	private static final String OLD_TRAINING_DIR_NAME = "training-data";

	public Path archiveTrainingDataOnly(Path csvPath) throws IOException {
		return archiveTrainingData(csvPath);
	}

	public Path buildNormalizedUploadFile(Path csvPath) throws IOException {
		if (csvPath == null || !Files.isRegularFile(csvPath)) {
			throw new IOException("Training file not found: " + csvPath);
		}
		int[] ignoredRows = new int[] {0};
		List<Sample> samples = loadSamples(csvPath, ignoredRows);
		validateSamples(samples);

		Path uploadDir = resolveUploadDir(csvPath);
		Path uploadPath = nextUploadTarget(csvPath, uploadDir);

		List<String> lines = new ArrayList<>(samples.size() + 1);
		lines.add("message,label");
		for (Sample sample : samples) {
			lines.add(escapeCsv(sample.message()) + "," + sample.label());
		}

		Files.write(
			uploadPath,
			lines,
			StandardCharsets.UTF_8,
			StandardOpenOption.CREATE_NEW,
			StandardOpenOption.WRITE
		);
		return uploadPath;
	}

	private static Path archiveTrainingData(Path csvPath) throws IOException {
		Path archiveDir = csvPath.resolveSibling(OLD_DIR_NAME).resolve(OLD_TRAINING_DIR_NAME);
		Path target = nextArchiveTarget(csvPath, archiveDir);
		return Files.move(csvPath, target);
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

	private static Path nextUploadTarget(Path baseFile, Path uploadDir) throws IOException {
		Files.createDirectories(uploadDir);
		String baseName = (baseFile == null || baseFile.getFileName() == null)
			? "training-data.csv"
			: baseFile.getFileName().toString();
		baseName = stripArchiveSuffix(baseName);
		Path target = uploadDir.resolve(baseName + ".upload.tmp." + UUID.randomUUID().toString().replace("-", "") + ".csv");
		while (Files.exists(target)) {
			target = uploadDir.resolve(baseName + ".upload.tmp." + UUID.randomUUID().toString().replace("-", "") + ".csv");
		}
		return target;
	}

	private static Path resolveUploadDir(Path csvPath) throws IOException {
		if (isArchivedTrainingDataFile(csvPath)) {
			Path parent = csvPath.getParent();
			Files.createDirectories(parent);
			return parent;
		}
		Path uploadDir = csvPath.resolveSibling(OLD_DIR_NAME).resolve(OLD_TRAINING_DIR_NAME);
		Files.createDirectories(uploadDir);
		return uploadDir;
	}

	private static boolean isArchivedTrainingDataFile(Path csvPath) {
		if (csvPath == null) {
			return false;
		}
		Path parent = csvPath.getParent();
		if (parent == null || parent.getFileName() == null || !OLD_TRAINING_DIR_NAME.equalsIgnoreCase(parent.getFileName().toString())) {
			return false;
		}
		Path grandParent = parent.getParent();
		return grandParent != null
			&& grandParent.getFileName() != null
			&& OLD_DIR_NAME.equalsIgnoreCase(grandParent.getFileName().toString());
	}

	private static String stripArchiveSuffix(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return "training-data.csv";
		}
		return fileName.replaceFirst("\\.old\\.\\d+$", "");
	}

	private static List<Sample> loadSamples(Path csvPath, int[] ignoredRows) throws IOException {
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
			samples.add(new Sample(message, label));
		}
		return samples;
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

	private static String escapeCsv(String value) {
		String escaped = (value == null ? "" : value)
			.replace("\r", " ")
			.replace("\n", " ")
			.replace("\"", "\"\"");
		return "\"" + escaped + "\"";
	}

	private static int parseInt(String value, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception ignored) {
			return fallback;
		}
	}

	static record Sample(String message, int label) {
	}
}
