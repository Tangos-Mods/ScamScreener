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
import java.util.List;

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

}
