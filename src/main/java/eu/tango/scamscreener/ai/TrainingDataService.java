package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.config.ScamScreenerPaths;
import eu.tango.scamscreener.detection.ChatLineParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public final class TrainingDataService {
	public static final int MAX_CAPTURED_CHAT_LINES = 100;

	private static final String LEGACY_TRAINING_HEADER = "message,label,pushes_external_platform,demands_upfront_payment,requests_sensitive_data,claims_middleman_without_proof,repeated_contact_attempts";
	private static final String TRAINING_HEADER = "message,label," + TrainingFlags.csvColumnsHeader();
	private static final Path TRAINING_DATA_PATH = ScamScreenerPaths.inModConfigDir("scam-screener-training-data.csv");
	private static final Path LEGACY_TRAINING_DATA_PATH = ScamScreenerPaths.inRootConfigDir("scam-screener-training-data.csv");

	private final TrainingTypeAiClassifier typeAiClassifier = new TrainingTypeAiClassifier();
	private final Deque<String> recentChatLines = new ArrayDeque<>();
	private String lastCapturedChatLine = "";

	public void recordChatLine(String plain) {
		if (plain == null || plain.isBlank()) {
			return;
		}
		if (ChatLineParser.parsePlayerLine(plain) == null) {
			return;
		}

		lastCapturedChatLine = plain;
		recentChatLines.addLast(plain);
		while (recentChatLines.size() > MAX_CAPTURED_CHAT_LINES) {
			recentChatLines.removeFirst();
		}
	}

	public String lastCapturedLine() {
		return lastCapturedChatLine == null ? "" : lastCapturedChatLine.trim();
	}

	public List<String> recentLines(int count) {
		if (count <= 0 || recentChatLines.isEmpty()) {
			return List.of();
		}

		List<String> snapshot = new ArrayList<>(recentChatLines);
		int take = Math.min(count, snapshot.size());
		int startIndex = snapshot.size() - take;
		return new ArrayList<>(snapshot.subList(startIndex, snapshot.size()));
	}

	public List<String> recentLinesForPlayer(String playerName, int count) {
		if (playerName == null || playerName.isBlank() || count <= 0 || recentChatLines.isEmpty()) {
			return List.of();
		}

		String target = playerName.toLowerCase(Locale.ROOT);
		List<String> matching = new ArrayList<>();
		for (String line : recentChatLines) {
			String author = extractAuthor(line);
			if (author != null && author.equalsIgnoreCase(target)) {
				matching.add(line);
			}
		}

		if (matching.isEmpty()) {
			return List.of();
		}

		int take = Math.min(count, matching.size());
		int startIndex = matching.size() - take;
		return new ArrayList<>(matching.subList(startIndex, matching.size()));
	}

	private static String extractAuthor(String line) {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine(line);
		if (parsed == null) {
			return null;
		}
		return parsed.playerName().toLowerCase(Locale.ROOT);
	}

	public Path trainingDataPath() {
		return TRAINING_DATA_PATH;
	}

	public void appendRows(List<String> messages, int label) throws IOException {
		if (messages == null || messages.isEmpty()) {
			return;
		}

		ensureFileInitialized();
		ensureLatestHeader();
		StringBuilder rows = new StringBuilder();
		for (String message : messages) {
			rows.append(buildTrainingCsvRow(message, label)).append(System.lineSeparator());
		}

		Files.writeString(
			TRAINING_DATA_PATH,
			rows.toString(),
			StandardCharsets.UTF_8,
			StandardOpenOption.APPEND
		);
	}

	private static void ensureFileInitialized() throws IOException {
		Files.createDirectories(TRAINING_DATA_PATH.getParent());
		if (Files.exists(TRAINING_DATA_PATH)) {
			return;
		}
		if (Files.exists(LEGACY_TRAINING_DATA_PATH)) {
			Files.move(LEGACY_TRAINING_DATA_PATH, TRAINING_DATA_PATH, StandardCopyOption.REPLACE_EXISTING);
			return;
		}

		Files.writeString(
			TRAINING_DATA_PATH,
			TRAINING_HEADER + System.lineSeparator(),
			StandardCharsets.UTF_8,
			StandardOpenOption.CREATE_NEW
		);
	}

	private static void ensureLatestHeader() throws IOException {
		if (!Files.exists(TRAINING_DATA_PATH)) {
			return;
		}

		List<String> lines = Files.readAllLines(TRAINING_DATA_PATH, StandardCharsets.UTF_8);
		if (lines.isEmpty()) {
			Files.writeString(TRAINING_DATA_PATH, TRAINING_HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
			return;
		}

		String first = lines.get(0).trim();
		if (TRAINING_HEADER.equals(first)) {
			return;
		}
		if (!LEGACY_TRAINING_HEADER.equals(first)) {
			return;
		}

		lines.set(0, TRAINING_HEADER);
		Files.write(TRAINING_DATA_PATH, lines, StandardCharsets.UTF_8);
	}

	private String buildTrainingCsvRow(String message, int label) {
		String normalizedMessage = normalizeTrainingMessage(message);
		TrainingFlags.Values flags = typeAiClassifier.predict(normalizedMessage);

		return escapeCsv(normalizedMessage) + ","
			+ label + ","
			+ flags.toCsvColumns();
	}

	private static String normalizeTrainingMessage(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine(raw);
		if (parsed != null && parsed.message() != null && !parsed.message().isBlank()) {
			return parsed.message().trim();
		}
		return raw.trim();
	}

	private static String escapeCsv(String value) {
		String escaped = value
			.replace("\r", " ")
			.replace("\n", " ")
			.replace("\"", "\"\"");
		return "\"" + escaped + "\"";
	}
}
