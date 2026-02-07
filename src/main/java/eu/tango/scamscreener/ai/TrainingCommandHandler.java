package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.ui.messages.TrainingMessages;

import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.ui.MessageFlagging;
import eu.tango.scamscreener.util.IoErrorMapper;
import eu.tango.scamscreener.config.LocalAiModelConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class TrainingCommandHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(TrainingCommandHandler.class);
	private static final int LEGIT_LABEL = 0;
	private final TrainingDataService trainingDataService;
	private final LocalAiTrainer localAiTrainer;
	private volatile boolean trainingInProgress;

	public TrainingCommandHandler(TrainingDataService trainingDataService, LocalAiTrainer localAiTrainer) {
		this.trainingDataService = trainingDataService;
		this.localAiTrainer = localAiTrainer;
	}

	public int flagHoveredMessage(int label) {
		String message = MessageFlagging.hoveredMessage();
		if (message == null || message.isBlank()) {
			MessageDispatcher.reply(TrainingMessages.noChatToCapture());
			return 0;
		}
		try {
			trainingDataService.appendRows(List.of(message), label);
			MessageDispatcher.reply(TrainingMessages.trainingSampleFlagged(label == LEGIT_LABEL ? "legit" : "scam"));
			return 1;
		} catch (IOException e) {
			LOGGER.warn("Failed to save training sample from hover", e);
			// Code: TR-SAVE-002
			MessageDispatcher.reply(TrainingMessages.trainingSamplesSaveFailed(trainingErrorDetail(e, trainingDataService.trainingDataPath())));
			return 0;
		}
	}

	public int captureChatAsTrainingData(String playerName, int label, int count) {
		List<String> lines = trainingDataService.recentLinesForPlayer(playerName, count);
		if (lines.isEmpty()) {
			MessageDispatcher.reply(TrainingMessages.noChatToCapture());
			return 0;
		}

		try {
			trainingDataService.appendRows(lines, label);
			if (lines.size() == 1) {
				MessageDispatcher.reply(TrainingMessages.trainingSampleSaved(trainingDataService.trainingDataPath().toString(), label));
			} else {
				MessageDispatcher.reply(TrainingMessages.trainingSamplesSaved(trainingDataService.trainingDataPath().toString(), label, lines.size()));
			}
			return 1;
		} catch (IOException e) {
			LOGGER.warn("Failed to save training samples", e);
			// Code: TR-SAVE-002
			MessageDispatcher.reply(TrainingMessages.trainingSamplesSaveFailed(trainingErrorDetail(e, trainingDataService.trainingDataPath())));
			return 0;
		}
	}

	public int captureMessageById(String messageId, int label) {
		String message = MessageFlagging.messageById(messageId);
		if (message == null || message.isBlank()) {
			MessageDispatcher.reply(TrainingMessages.noChatToCapture());
			return 0;
		}
		try {
			trainingDataService.appendRows(List.of(message), label);
			MessageDispatcher.reply(TrainingMessages.trainingSampleFlagged(label == LEGIT_LABEL ? "legit" : "scam"));
			return 1;
		} catch (IOException e) {
			LOGGER.warn("Failed to save training sample from message id", e);
			// Code: TR-SAVE-002
			MessageDispatcher.reply(TrainingMessages.trainingSamplesSaveFailed(trainingErrorDetail(e, trainingDataService.trainingDataPath())));
			return 0;
		}
	}

	public int captureBulkLegit(int count) {
		List<String> lines = trainingDataService.recentLines(count);
		if (lines.isEmpty()) {
			MessageDispatcher.reply(TrainingMessages.noChatToCapture());
			return 0;
		}
		try {
			trainingDataService.appendRows(lines, LEGIT_LABEL);
			MessageDispatcher.reply(TrainingMessages.trainingSamplesSaved(trainingDataService.trainingDataPath().toString(), LEGIT_LABEL, lines.size()));
			return 1;
		} catch (IOException e) {
			LOGGER.warn("Failed to save bulk legit samples", e);
			// Code: TR-SAVE-002
			MessageDispatcher.reply(TrainingMessages.trainingSamplesSaveFailed(trainingErrorDetail(e, trainingDataService.trainingDataPath())));
			return 0;
		}
	}

	public int migrateTrainingData() {
		try {
			int updated = trainingDataService.migrateTrainingData();
			if (updated <= 0) {
				MessageDispatcher.reply(TrainingMessages.trainingDataUpToDate());
			} else {
				MessageDispatcher.reply(TrainingMessages.trainingDataMigrated(updated));
			}
			return 1;
		} catch (IOException e) {
			LOGGER.warn("Failed to migrate training data", e);
			// Code: TR-SAVE-002
			MessageDispatcher.reply(TrainingMessages.trainingSamplesSaveFailed(trainingErrorDetail(e, trainingDataService.trainingDataPath())));
			return 0;
		}
	}

	public int trainLocalAiModel() {
		if (trainingInProgress) {
			MessageDispatcher.reply(TrainingMessages.trainingAlreadyRunning());
			return 0;
		}
		trainingInProgress = true;
		Thread thread = new Thread(() -> {
			try {
				LocalAiTrainer.TrainingResult result = localAiTrainer.trainAndSave(trainingDataService.trainingDataPath());
				ScamRules.reloadConfig();
				MessageDispatcher.reply(TrainingMessages.trainingCompleted(
					result.sampleCount(),
					result.positiveCount(),
					result.archivedDataPath().getFileName().toString()
				));
				if (result.ignoredUnigrams() > 0) {
					MessageDispatcher.reply(TrainingMessages.trainingUnigramsIgnored(result.ignoredUnigrams()));
				}
			} catch (IOException e) {
				LOGGER.warn("Failed to train local AI model", e);
				// Code: TR-TRAIN-001
				MessageDispatcher.reply(TrainingMessages.trainingFailed(trainingErrorDetail(e, trainingDataService.trainingDataPath())));
			} finally {
				trainingInProgress = false;
			}
		}, "scamscreener-train");
		thread.setDaemon(true);
		thread.start();
		return 1;
	}

	public int resetLocalAiModel() {
		LocalAiModelConfig.save(new LocalAiModelConfig());
		ScamRules.reloadConfig();
		MessageDispatcher.reply(TrainingMessages.localAiModelReset());
		return 1;
	}

	private static String trainingErrorDetail(IOException error, Path trainingPath) {
		return IoErrorMapper.trainingErrorDetail(error, trainingPath);
	}
}

