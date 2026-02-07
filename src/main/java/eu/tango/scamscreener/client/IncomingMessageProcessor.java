package eu.tango.scamscreener.client;

import eu.tango.scamscreener.ai.TrainingDataService;
import eu.tango.scamscreener.blacklist.BlacklistAlertService;
import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.chat.parser.ChatLineParser;
import eu.tango.scamscreener.chat.trigger.TriggerContext;
import eu.tango.scamscreener.pipeline.core.DetectionPipeline;
import eu.tango.scamscreener.pipeline.model.MessageContext;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.ScreeningResult;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

@RequiredArgsConstructor
public final class IncomingMessageProcessor {
	private final TrainingDataService trainingDataService;
	private final DetectionPipeline detectionPipeline;
	private final BlacklistManager blacklist;
	private final BlacklistAlertService blacklistAlertService;
	private final Consumer<Component> reply;
	private final Runnable warningSound;
	private final Consumer<ScreeningResult> onWarned;

	public ScreeningResult process(Component message) {
		String plain = message == null ? "" : message.getString().trim();
		trainingDataService.recordChatLine(plain);

		Minecraft client = Minecraft.getInstance();
		if (client.player == null || client.getConnection() == null) {
			return null;
		}

		ScreeningResult screening = runDetection(plain);
		runBlacklistChecks(plain);
		return screening;
	}

	private ScreeningResult runDetection(String plain) {
		MessageEvent event = parseMessageEvent(plain, System.currentTimeMillis());
		if (event == null) {
			return null;
		}

		ScreeningResult screening = detectionPipeline.processWithResult(event, reply, warningSound);
		if (screening != null && screening.shouldWarn()) {
			onWarned.accept(screening);
		}
		return screening;
	}

	private void runBlacklistChecks(String plain) {
		if (blacklist.isEmpty()) {
			return;
		}
		for (TriggerContext context : TriggerContext.values()) {
			blacklistAlertService.checkTriggerAndWarn(plain, context);
		}
	}

	private static MessageEvent parseMessageEvent(String rawLine, long timestampMs) {
		ChatLineParser.ParsedPlayerLine parsed = ChatLineParser.parsePlayerLine(rawLine);
		if (parsed == null) {
			return null;
		}
		return MessageEvent.from(
			parsed.playerName(),
			parsed.message(),
			timestampMs,
			MessageContext.UNKNOWN,
			null
		);
	}
}
