package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.discord.DiscordWebhookUploader;
import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.ui.MessageFlagging;
import eu.tango.scamscreener.util.IoErrorMapper;
import eu.tango.scamscreener.config.LocalAiModelConfig;
import eu.tango.scamscreener.gui.UploadTosScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TrainingCommandHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TrainingCommandHandler.class);
	private static final int SCAM_LABEL = 1;
	private static final int LEGIT_LABEL = 0;
	private static final String UPLOAD_TOS_RESOURCE_PATH = "/assets/scam-screener/text/upload-tos.txt";
	private static final String UPLOAD_TOS_FALLBACK = "By uploading training data you confirm you have rights to share it, "
		+ "the data may contain personal chat content, and no malware or unrelated files are included.";
	private final TrainingDataService trainingDataService;
	private final FunnelMetricsService funnelMetricsService;
	private final LocalAiTrainer localAiTrainer;
	private final DiscordWebhookUploader discordWebhookUploader;

	public TrainingCommandHandler(
		TrainingDataService trainingDataService,
		FunnelMetricsService funnelMetricsService,
		LocalAiTrainer localAiTrainer,
		DiscordWebhookUploader discordWebhookUploader
	) {
		this.trainingDataService = trainingDataService;
		this.funnelMetricsService = funnelMetricsService == null ? new FunnelMetricsService() : funnelMetricsService;
		this.localAiTrainer = localAiTrainer;
		this.discordWebhookUploader = discordWebhookUploader == null ? new DiscordWebhookUploader() : discordWebhookUploader;
	}

	public int captureChatAsTrainingData(String playerName, int label, int count) {
		List<TrainingDataService.CapturedChat> captures = trainingDataService.recentCapturedForPlayer(playerName, count);
		if (captures.isEmpty()) {
			MessageDispatcher.reply(Messages.noChatToCapture());
			return 0;
		}

		try {
			trainingDataService.appendCapturedRows(captures, label);
			if (captures.size() == 1) {
				MessageDispatcher.reply(Messages.trainingSampleSaved(trainingDataService.trainingDataPath().toString(), label));
			} else {
				MessageDispatcher.reply(Messages.trainingSamplesSaved(trainingDataService.trainingDataPath().toString(), label, captures.size()));
			}
			return 1;
		} catch (IOException e) {
			LOGGER.warn("Failed to save training samples", e);
			// Code: TR-SAVE-002
			MessageDispatcher.reply(Messages.trainingSamplesSaveFailed(trainingErrorDetail(e, trainingDataService.trainingDataPath())));
			return 0;
		}
	}

	public int captureMessageById(String messageId, int label) {
		String message = MessageFlagging.messageById(messageId);
		if (message == null || message.isBlank()) {
			MessageDispatcher.reply(Messages.noChatToCapture());
			return 0;
		}
		try {
			trainingDataService.appendRows(List.of(message), label);
			funnelMetricsService.recordUserMark(message, label);
			MessageDispatcher.reply(Messages.trainingSampleFlagged(label == LEGIT_LABEL ? "legit" : "scam"));
			return 1;
		} catch (IOException e) {
			LOGGER.warn("Failed to save training sample from message id", e);
			// Code: TR-SAVE-002
			MessageDispatcher.reply(Messages.trainingSamplesSaveFailed(trainingErrorDetail(e, trainingDataService.trainingDataPath())));
			return 0;
		}
	}

	public int captureBulkLegit(int count) {
		List<TrainingDataService.CapturedChat> captures = trainingDataService.recentCaptured(count);
		if (captures.isEmpty()) {
			MessageDispatcher.reply(Messages.noChatToCapture());
			return 0;
		}
		try {
			trainingDataService.appendCapturedRows(captures, LEGIT_LABEL);
			MessageDispatcher.reply(Messages.trainingSamplesSaved(trainingDataService.trainingDataPath().toString(), LEGIT_LABEL, captures.size()));
			return 1;
		} catch (IOException e) {
			LOGGER.warn("Failed to save bulk legit samples", e);
			// Code: TR-SAVE-002
			MessageDispatcher.reply(Messages.trainingSamplesSaveFailed(trainingErrorDetail(e, trainingDataService.trainingDataPath())));
			return 0;
		}
	}

	public int migrateTrainingData() {
		try {
			int updated = trainingDataService.migrateTrainingData();
			if (updated <= 0) {
				MessageDispatcher.reply(Messages.trainingDataUpToDate());
			} else {
				MessageDispatcher.reply(Messages.trainingDataMigrated(updated));
			}
			return 1;
		} catch (IOException e) {
			LOGGER.warn("Failed to migrate training data", e);
			// Code: TR-SAVE-002
			MessageDispatcher.reply(Messages.trainingSamplesSaveFailed(trainingErrorDetail(e, trainingDataService.trainingDataPath())));
			return 0;
		}
	}

	public int trainLocalAiModel() {
		if (!ScamRules.uploadTosAccepted()) {
			openUploadTosPrompt();
			return 1;
		}

		Path trainingPath = trainingDataService.trainingDataPath();
		if (!Files.isRegularFile(trainingPath)) {
			// Code: TR-UPLOAD-002
			MessageDispatcher.reply(Messages.trainingUploadUnavailable("Training data file not found: " + trainingPath));
			return 1;
		}
		Path uploadPath;
		try {
			uploadPath = localAiTrainer.archiveTrainingDataOnly(trainingPath);
			openArchivedTrainingFolder(uploadPath);
		} catch (IOException e) {
			LOGGER.warn("Failed to archive training data for community upload", e);
			// Code: TR-TRAIN-001
			MessageDispatcher.reply(Messages.trainingFailed(trainingErrorDetail(e, trainingDataService.trainingDataPath())));
			return 0;
		}
		if (Files.isRegularFile(uploadPath)) {
			MessageDispatcher.reply(Messages.trainingUploadWebhookStarted(uploadPath.toString()));
			uploadArchivedTrainingDataAsync(uploadPath);
		} else {
			// Code: TR-UPLOAD-002
			MessageDispatcher.reply(Messages.trainingUploadUnavailable("Training data file not found: " + uploadPath));
		}
		return 1;
	}

	public int saveReviewedMessages(List<ReviewedMessage> selections, boolean uploadAfterSave) {
		if (selections == null || selections.isEmpty()) {
			MessageDispatcher.reply(Messages.reviewSelectionRequired());
			return 0;
		}

		List<String> scamMessages = new java.util.ArrayList<>();
		List<String> legitMessages = new java.util.ArrayList<>();
		for (ReviewedMessage selection : selections) {
			if (selection == null || selection.message() == null || selection.message().isBlank()) {
				continue;
			}
			if (selection.label() == SCAM_LABEL) {
				scamMessages.add(selection.message());
				continue;
			}
			if (selection.label() == LEGIT_LABEL) {
				legitMessages.add(selection.message());
			}
		}

		if (scamMessages.isEmpty() && legitMessages.isEmpty()) {
			MessageDispatcher.reply(Messages.reviewSelectionRequired());
			return 0;
		}

		try {
			if (!legitMessages.isEmpty()) {
				trainingDataService.appendRows(legitMessages, LEGIT_LABEL);
			}
			if (!scamMessages.isEmpty()) {
				trainingDataService.appendRows(scamMessages, SCAM_LABEL);
			}
			for (String message : legitMessages) {
				funnelMetricsService.recordUserMark(message, LEGIT_LABEL);
			}
			for (String message : scamMessages) {
				funnelMetricsService.recordUserMark(message, SCAM_LABEL);
			}
			MessageDispatcher.reply(Messages.reviewMessagesSaved(scamMessages.size(), legitMessages.size()));
		} catch (IOException e) {
			LOGGER.warn("Failed to save reviewed training samples", e);
			MessageDispatcher.reply(Messages.trainingSamplesSaveFailed(trainingErrorDetail(e, trainingDataService.trainingDataPath())));
			return 0;
		}

		if (uploadAfterSave) {
			trainLocalAiModel();
		}
		return 1;
	}

	public List<TrainingCsvReviewRow> loadTrainingCsvForReview() throws IOException {
		Path trainingPath = trainingDataService.trainingDataPath();
		if (!Files.isRegularFile(trainingPath)) {
			return List.of();
		}

		trainingDataService.migrateTrainingData();
		List<String> lines = Files.readAllLines(trainingPath, StandardCharsets.UTF_8);
		return parseTrainingCsvRows(lines);
	}

	public int saveTrainingCsvReview(List<CsvLabelUpdate> updates, boolean uploadAfterSave) {
		if (updates == null) {
			return 0;
		}

		Path trainingPath = trainingDataService.trainingDataPath();
		if (!Files.isRegularFile(trainingPath)) {
			MessageDispatcher.reply(Messages.trainingCsvReviewNoData(trainingPath.toString()));
			return 0;
		}

		Map<Integer, Integer> requestedStates = requestedStatesByLine(updates);
		if (requestedStates.isEmpty()) {
			MessageDispatcher.reply(Messages.trainingCsvReviewUpdated(0));
			if (uploadAfterSave) {
				trainLocalAiModel();
			}
			return 1;
		}

		try {
			trainingDataService.migrateTrainingData();
			List<String> lines = Files.readAllLines(trainingPath, StandardCharsets.UTF_8);
			if (lines.size() <= 1) {
				MessageDispatcher.reply(Messages.trainingCsvReviewNoData(trainingPath.toString()));
				return 0;
			}

			CsvUpdateResult updateResult = applyCsvReviewUpdates(lines, requestedStates);
			if (updateResult.changedRows() > 0) {
				Files.write(trainingPath, updateResult.lines(), StandardCharsets.UTF_8);
			}
			MessageDispatcher.reply(Messages.trainingCsvReviewUpdated(updateResult.changedRows()));
		} catch (IOException e) {
			LOGGER.warn("Failed to update training csv review labels", e);
			MessageDispatcher.reply(Messages.trainingCsvReviewFailed(trainingErrorDetail(e, trainingPath)));
			return 0;
		}

		if (uploadAfterSave) {
			trainLocalAiModel();
		}
		return 1;
	}

	public int resetLocalAiModel() {
		LocalAiModelConfig.save(new LocalAiModelConfig());
		ScamRules.reloadConfig();
		MessageDispatcher.reply(Messages.localAiModelReset());
		return 1;
	}

	public int showFunnelMetrics() {
		MessageDispatcher.reply(Messages.funnelMetricsSummary(funnelMetricsService.snapshot()));
		return 1;
	}

	public int resetFunnelMetrics() {
		funnelMetricsService.reset();
		MessageDispatcher.reply(Messages.funnelMetricsReset());
		return 1;
	}

	private static String trainingErrorDetail(IOException error, Path trainingPath) {
		return IoErrorMapper.trainingErrorDetail(error, trainingPath);
	}

	static List<TrainingCsvReviewRow> parseTrainingCsvRows(List<String> lines) throws IOException {
		if (lines == null || lines.size() <= 1) {
			return List.of();
		}

		List<String> header = parseCsvLine(lines.get(0));
		int messageIndex = columnIndex(header, "message");
		int labelIndex = columnIndex(header, "label");
		if (messageIndex < 0 || labelIndex < 0) {
			throw new IOException("Training data header must contain message,label");
		}

		List<TrainingCsvReviewRow> rows = new ArrayList<>();
		for (int i = 1; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line == null || line.isBlank()) {
				continue;
			}
			List<String> columns = parseCsvLine(line);
			String message = valueAt(columns, messageIndex);
			if (message == null || message.isBlank()) {
				continue;
			}
			int currentLabel = normalizeReviewLabel(parseInt(valueAt(columns, labelIndex), LEGIT_LABEL));
			rows.add(new TrainingCsvReviewRow(String.valueOf(i + 1), message, currentLabel));
		}
		return rows;
	}

	static CsvUpdateResult applyCsvReviewUpdates(List<String> lines, Map<Integer, Integer> requestedStates) throws IOException {
		if (lines == null || lines.isEmpty()) {
			return new CsvUpdateResult(List.of(), 0);
		}

		List<String> header = parseCsvLine(lines.get(0));
		int labelIndex = columnIndex(header, "label");
		if (labelIndex < 0) {
			throw new IOException("Training data header is missing label column");
		}

		List<String> updatedLines = new ArrayList<>(lines.size());
		updatedLines.add(lines.get(0));
		int changedRows = 0;
		for (int index = 1; index < lines.size(); index++) {
			String rawLine = lines.get(index);
			Integer requestedState = requestedStates.get(index + 1);
			if (requestedState == null) {
				updatedLines.add(rawLine);
				continue;
			}
			if (rawLine == null || rawLine.isBlank()) {
				updatedLines.add(rawLine);
				continue;
			}
			if (requestedState == -1) {
				changedRows++;
				continue;
			}

			List<String> columns = parseCsvLine(rawLine);
			ensureColumnIndex(columns, labelIndex);
			int currentLabel = normalizeReviewLabel(parseInt(columns.get(labelIndex), LEGIT_LABEL));
			int requestedLabel = normalizeReviewLabel(requestedState);
			if (requestedLabel < 0 || currentLabel == requestedLabel) {
				updatedLines.add(rawLine);
				continue;
			}

			String updatedLine = replaceCsvColumn(rawLine, labelIndex, String.valueOf(requestedLabel));
			if (updatedLine == null) {
				// Fallback for malformed rows: normalize the row structure and apply new label.
				columns.set(labelIndex, String.valueOf(requestedLabel));
				updatedLine = joinCsvLine(columns);
			}
			updatedLines.add(updatedLine);
			changedRows++;
		}
		return new CsvUpdateResult(updatedLines, changedRows);
	}

	private static Map<Integer, Integer> requestedStatesByLine(List<CsvLabelUpdate> updates) {
		Map<Integer, Integer> requested = new LinkedHashMap<>();
		if (updates == null || updates.isEmpty()) {
			return requested;
		}
		for (CsvLabelUpdate update : updates) {
			if (update == null) {
				continue;
			}
			int lineNumber = parseInt(update.rowId(), -1);
			int state = update.label();
			if (lineNumber <= 1 || (state != LEGIT_LABEL && state != SCAM_LABEL && state != -1)) {
				continue;
			}
			requested.put(lineNumber, state);
		}
		return requested;
	}

	private static int columnIndex(List<String> header, String column) {
		if (header == null || header.isEmpty() || column == null || column.isBlank()) {
			return -1;
		}
		for (int i = 0; i < header.size(); i++) {
			String value = header.get(i);
			if (value == null) {
				continue;
			}
			if (column.equalsIgnoreCase(value.trim())) {
				return i;
			}
		}
		return -1;
	}

	private static String valueAt(List<String> columns, int index) {
		if (columns == null || index < 0 || index >= columns.size()) {
			return "";
		}
		String value = columns.get(index);
		return value == null ? "" : value;
	}

	private static int parseInt(String value, int fallback) {
		if (value == null) {
			return fallback;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static int normalizeReviewLabel(int label) {
		return (label == LEGIT_LABEL || label == SCAM_LABEL) ? label : -1;
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

	private static void ensureColumnIndex(List<String> columns, int index) {
		if (columns == null || index < 0) {
			return;
		}
		while (columns.size() <= index) {
			columns.add("0");
		}
	}

	private static String joinCsvLine(List<String> columns) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < columns.size(); i++) {
			if (i > 0) {
				out.append(',');
			}
			out.append(csvValue(columns.get(i)));
		}
		return out.toString();
	}

	private static String csvValue(String value) {
		String safe = value == null ? "" : value;
		boolean needsQuotes = safe.contains(",")
			|| safe.contains("\"")
			|| safe.contains("\n")
			|| safe.contains("\r")
			|| safe.startsWith(" ")
			|| safe.endsWith(" ");
		String normalized = safe.replace("\r", " ").replace("\n", " ");
		if (!needsQuotes) {
			return normalized;
		}
		return "\"" + normalized.replace("\"", "\"\"") + "\"";
	}

	private static String replaceCsvColumn(String line, int columnIndex, String replacement) {
		CsvColumnRange range = findCsvColumnRange(line, columnIndex);
		if (range == null) {
			return null;
		}
		String safeReplacement = replacement == null ? "" : replacement;
		return line.substring(0, range.start()) + safeReplacement + line.substring(range.end());
	}

	private static CsvColumnRange findCsvColumnRange(String line, int columnIndex) {
		if (line == null || columnIndex < 0) {
			return null;
		}
		int currentColumn = 0;
		int start = 0;
		boolean inQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					i++;
					continue;
				}
				inQuotes = !inQuotes;
				continue;
			}
			if (c == ',' && !inQuotes) {
				if (currentColumn == columnIndex) {
					return new CsvColumnRange(start, i);
				}
				currentColumn++;
				start = i + 1;
			}
		}
		if (currentColumn == columnIndex) {
			return new CsvColumnRange(start, line.length());
		}
		return null;
	}

	private void openUploadTosPrompt() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			MessageDispatcher.reply(Messages.trainingUploadUnavailable("ToS screen unavailable: Minecraft client not ready."));
			return;
		}

		String tosText = loadUploadTosText();
		// Open on a queued client task so command-chat close cannot immediately overwrite the ToS screen.
		CompletableFuture.runAsync(() -> client.execute(() -> {
			Screen parent = client.screen instanceof ChatScreen ? null : client.screen;
			UploadTosScreen tosScreen = new UploadTosScreen(
				parent,
				tosText,
				() -> {
					if (!ScamRules.setUploadTosAccepted(true)) {
						MessageDispatcher.reply(Messages.trainingUploadUnavailable("Failed to persist upload ToS consent."));
						return;
					}
					trainLocalAiModel();
				}
			);
			client.setScreen(tosScreen);
		}));
	}

	private static void openArchivedTrainingFolder(Path archivedPath) {
		if (archivedPath == null || !Desktop.isDesktopSupported()) {
			return;
		}
		Path targetFolder = resolveOldFolder(archivedPath);
		if (targetFolder == null || !Files.isDirectory(targetFolder)) {
			return;
		}
		try {
			Desktop desktop = Desktop.getDesktop();
			if (desktop.isSupported(Desktop.Action.OPEN)) {
				desktop.open(targetFolder.toFile());
			}
		} catch (Exception ignored) {
		}
	}

	private static String loadUploadTosText() {
		try (InputStream input = TrainingCommandHandler.class.getResourceAsStream(UPLOAD_TOS_RESOURCE_PATH)) {
			if (input == null) {
				return UPLOAD_TOS_FALLBACK;
			}
			String text = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
			return text.isEmpty() ? UPLOAD_TOS_FALLBACK : text;
		} catch (IOException ignored) {
			return UPLOAD_TOS_FALLBACK;
		}
	}

	private void uploadArchivedTrainingDataAsync(Path uploadPath) {
		DiscordWebhookUploader.UploaderContext uploaderContext = DiscordWebhookUploader.captureCurrentUploader();
		Thread thread = new Thread(() -> {
			DiscordWebhookUploader.UploadResult result = discordWebhookUploader.uploadTrainingFile(uploadPath, uploaderContext);
			if (result.success()) {
				MessageDispatcher.reply(Messages.trainingUploadWebhookSucceeded(uploadPath.toString(), result.detail()));
				return;
			}
			LOGGER.warn("Failed to upload training data to Discord webhook: {}", result.detail());
			// Code: TR-UPLOAD-001
			MessageDispatcher.reply(Messages.trainingUploadWebhookFailed(result.detail()));
		}, "scamscreener-discord-upload");
		thread.setDaemon(true);
		thread.start();
	}

	private static Path resolveOldFolder(Path archivedPath) {
		Path cursor = archivedPath;
		while (cursor != null) {
			Path fileName = cursor.getFileName();
			if (fileName != null && "old".equalsIgnoreCase(fileName.toString())) {
				return cursor;
			}
			cursor = cursor.getParent();
		}
		return archivedPath.getParent();
	}

	public record ReviewedMessage(String message, int label) {
		public ReviewedMessage {
			message = message == null ? "" : message.trim();
		}
	}

	public record TrainingCsvReviewRow(String rowId, String message, int currentLabel) {
		public TrainingCsvReviewRow {
			rowId = rowId == null ? "" : rowId.trim();
			message = message == null ? "" : message.trim();
			currentLabel = normalizeReviewLabel(currentLabel);
		}
	}

	public record CsvLabelUpdate(String rowId, int label) {
		public CsvLabelUpdate {
			rowId = rowId == null ? "" : rowId.trim();
			label = normalizeReviewLabel(label);
		}
	}

	record CsvUpdateResult(List<String> lines, int changedRows) {
	}

	private record CsvColumnRange(int start, int end) {
	}

}
