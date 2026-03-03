package eu.tango.scamscreener.chat;

import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.api.event.PipelineDecisionEvent;
import eu.tango.scamscreener.message.MessageDispatcher;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.Optional;

/**
 * Bridges inbound chat callbacks into the ScamScreener v2 pipeline.
 */
public final class ChatPipelineListener {
    private static final int MAX_CHAT_LENGTH = 32767;

    private static boolean initialized;
    private static ChatEvent lastChatEvent;
    private static PipelineDecision lastPipelineDecision;

    private ChatPipelineListener() {
    }

    /**
     * Registers the inbound chat listener once.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
            onChatMessage(ChatEvent.fromInboundChat(
                message,
                sender,
                params,
                receptionTimestamp,
                MAX_CHAT_LENGTH,
                ChatSourceType.PLAYER
            ))
        );
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                onChatMessage(classifyGameMessage(message, MAX_CHAT_LENGTH));
            }
        });
        ScamScreenerMod.LOGGER.info("ChatPipelineListener is listening for inbound chat messages.");
    }

    /**
     * Returns the last raw chat message seen by the listener.
     *
     * @return the last raw chat message, when present
     */
    public static Optional<String> getLastChatMessage() {
        if (lastChatEvent == null || lastChatEvent.getRawMessage().isBlank()) {
            return Optional.empty();
        }

        return Optional.of(lastChatEvent.getRawMessage());
    }

    /**
     * Returns the last inbound chat event seen by the listener.
     *
     * @return the last chat event, when present
     */
    public static Optional<ChatEvent> getLastChatEvent() {
        if (lastChatEvent == null) {
            return Optional.empty();
        }

        return Optional.of(lastChatEvent);
    }

    /**
     * Returns the last pipeline decision produced by the listener.
     *
     * @return the last pipeline decision, when present
     */
    public static Optional<PipelineDecision> getLastPipelineDecision() {
        if (lastPipelineDecision == null) {
            return Optional.empty();
        }

        return Optional.of(lastPipelineDecision);
    }

    /**
     * Handles a single inbound chat event.
     *
     * @param chatEvent the event to forward into the pipeline
     */
    private static void onChatMessage(ChatEvent chatEvent) {
        ChatEvent safeEvent = chatEvent == null ? ChatEvent.messageOnly("") : chatEvent;
        if (MessageDispatcher.consumeLocalEcho(safeEvent.getRawMessage())) {
            return;
        }

        lastChatEvent = safeEvent;
        lastPipelineDecision = ScamScreenerRuntime.getInstance().pipelineEngine().evaluate(safeEvent);
        PipelineDecisionEvent.EVENT.invoker().onPipelineDecision(safeEvent, lastPipelineDecision);
        ScamScreenerMod.LOGGER.debug(
            "Captured {} chat message from {} ({}): {}",
            safeEvent.getSourceType(),
            safeEvent.getSenderName().isBlank() ? "unknown" : safeEvent.getSenderName(),
            safeEvent.getSenderUuid(),
            safeEvent.getRawMessage()
        );
        ScamScreenerMod.LOGGER.debug(
            "Pipeline outcome={} score={} decidedBy={}",
            lastPipelineDecision.getOutcome(),
            lastPipelineDecision.getTotalScore(),
            lastPipelineDecision.getDecidedByStage()
        );
    }

    static ChatEvent classifyGameMessage(net.minecraft.text.Text message, int maxChatLength) {
        String rawLine = message == null ? "" : message.asTruncatedString(maxChatLength);
        ChatLineClassifier.ParsedPlayerLine parsedPlayerLine = ChatLineClassifier.parsePlayerMessage(rawLine).orElse(null);
        if (parsedPlayerLine != null) {
            return new ChatEvent(
                parsedPlayerLine.message(),
                null,
                parsedPlayerLine.senderName(),
                System.currentTimeMillis(),
                ChatSourceType.PLAYER
            );
        }

        if (ChatLineClassifier.classify(rawLine) == ChatLineClassifier.ChatLineType.SYSTEM) {
            return ChatEvent.fromGameMessage(message, maxChatLength);
        }

        return ChatEvent.messageOnly(rawLine, ChatSourceType.UNKNOWN);
    }
}
