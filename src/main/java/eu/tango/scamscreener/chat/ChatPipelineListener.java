package eu.tango.scamscreener.chat;

import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.api.event.PipelineDecisionEvent;
import eu.tango.scamscreener.message.MessageDispatcher;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.profiler.ScamScreenerProfiler;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.time.Instant;
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
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            try (ScamScreenerProfiler.Scope ignored = ScamScreenerProfiler.getInstance().scope("chat.player.total", "Chat Message")) {
                ChatEvent classifiedEvent = classifyChatMessage(
                    message,
                    sender,
                    params,
                    receptionTimestamp,
                    MAX_CHAT_LENGTH
                );
                if (classifiedEvent == null) {
                    return;
                }
                if (!classifiedEvent.isPlayerSource()) {
                    if (consumeLocalEcho(classifiedEvent)) {
                        return;
                    }
                    lastChatEvent = classifiedEvent;
                    lastPipelineDecision = null;
                    return;
                }
                onChatMessage(classifiedEvent);
            }
        });
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                try (ScamScreenerProfiler.Scope ignored = ScamScreenerProfiler.getInstance().scope("chat.game.total", "Game Message")) {
                    ChatEvent classifiedEvent;
                    try (ScamScreenerProfiler.Scope nested = ScamScreenerProfiler.getInstance().scope("chat.game.classify", "  Game Message Classify")) {
                        classifiedEvent = classifyGameMessage(message, MAX_CHAT_LENGTH);
                    }
                    if (classifiedEvent == null) {
                        return;
                    }
                    if (!classifiedEvent.isPlayerSource()) {
                        if (consumeLocalEcho(classifiedEvent)) {
                            return;
                        }
                        lastChatEvent = classifiedEvent;
                        lastPipelineDecision = null;
                        return;
                    }
                    onChatMessage(classifiedEvent);
                }
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
        if (chatEvent == null) {
            return;
        }

        ChatEvent safeEvent = chatEvent;
        if (consumeLocalEcho(safeEvent)) {
            return;
        }
        if (!shouldProcessChatEvent(safeEvent)) {
            // Only player-authored chat should enter the detection pipeline.
            lastChatEvent = safeEvent;
            lastPipelineDecision = null;
            return;
        }

        try (ScamScreenerProfiler.Scope ignored = ScamScreenerProfiler.getInstance().scope("chat.cache_record", "  Recent Chat Cache")) {
            ScamScreenerRuntime.getInstance().recentChatCache().record(safeEvent);
        }
        lastChatEvent = safeEvent;
        PipelineDecision pipelineDecision = ScamScreenerRuntime.getInstance().pipelineEngine().evaluate(safeEvent);
        if (pipelineDecision == null) {
            lastPipelineDecision = null;
            ScamScreenerMod.LOGGER.warn("Pipeline returned null for chat message: {}", safeEvent.getRawMessage());
            return;
        }
        lastPipelineDecision = pipelineDecision;
        try (ScamScreenerProfiler.Scope ignored = ScamScreenerProfiler.getInstance().scope("decision.dispatch", "Decision Dispatch")) {
            PipelineDecisionEvent.EVENT.invoker().onPipelineDecision(safeEvent, pipelineDecision);
        }
        ScamScreenerProfiler.getInstance().recordDecision(safeEvent, pipelineDecision);
        ScamScreenerMod.LOGGER.debug(
            "Captured {} chat message from {} ({}): {}",
            safeEvent.getSourceType(),
            safeEvent.getSenderName().isBlank() ? "unknown" : safeEvent.getSenderName(),
            safeEvent.getSenderUuid(),
            safeEvent.getRawMessage()
        );
        ScamScreenerMod.LOGGER.debug(
            "Pipeline outcome={} score={} decidedBy={}",
            pipelineDecision.getOutcome(),
            pipelineDecision.getTotalScore(),
            pipelineDecision.getDecidedByStage()
        );
    }

    static ChatEvent classifyGameMessage(net.minecraft.network.chat.Component message, int maxChatLength) {
        String rawLine = message == null ? "" : message.getString(maxChatLength);
        return classifyVisibleLine(rawLine, System.currentTimeMillis());
    }

    static ChatEvent classifyChatMessage(
        net.minecraft.network.chat.Component message,
        com.mojang.authlib.GameProfile sender,
        Object params,
        Instant receptionTimestamp,
        int maxChatLength
    ) {
        ChatEvent inboundEvent = ChatEvent.fromInboundChat(
            message,
            sender,
            params,
            receptionTimestamp,
            maxChatLength,
            ChatSourceType.UNKNOWN
        );
        if (inboundEvent.hasSender()) {
            return new ChatEvent(
                inboundEvent.getRawMessage(),
                inboundEvent.getSenderUuid(),
                inboundEvent.getSenderName(),
                inboundEvent.getTimestampMs(),
                ChatSourceType.PLAYER
            );
        }

        return classifyVisibleLine(inboundEvent.getRawMessage(), inboundEvent.getTimestampMs());
    }

    private static ChatEvent classifyVisibleLine(String rawLine, long timestampMs) {
        ChatLineClassifier.Analysis analysis = ChatLineClassifier.analyze(rawLine);
        if (analysis.type() == ChatLineClassifier.ChatLineType.PLAYER) {
            ChatLineClassifier.ParsedPlayerLine parsedPlayerLine = analysis.parsedPlayerLine();
            return new ChatEvent(
                parsedPlayerLine.message(),
                null,
                parsedPlayerLine.senderName(),
                timestampMs,
                ChatSourceType.PLAYER
            );
        }

        if (analysis.type() == ChatLineClassifier.ChatLineType.SYSTEM) {
            return new ChatEvent(analysis.cleanedLine(), null, "", timestampMs, ChatSourceType.SYSTEM);
        }
        if (analysis.type() == ChatLineClassifier.ChatLineType.IGNORED) {
            return null;
        }

        return new ChatEvent(analysis.cleanedLine(), null, "", timestampMs, ChatSourceType.UNKNOWN);
    }

    static boolean shouldEnterPipeline(ChatEvent chatEvent) {
        return chatEvent != null && chatEvent.isPlayerSource();
    }

    static boolean shouldProcessChatEvent(ChatEvent chatEvent) {
        return shouldProcessChatEvent(chatEvent, ScamScreenerRuntime.getInstance().isEnabled());
    }

    static boolean shouldProcessChatEvent(ChatEvent chatEvent, boolean scamScreenerEnabled) {
        return scamScreenerEnabled && shouldEnterPipeline(chatEvent);
    }

    private static boolean consumeLocalEcho(ChatEvent chatEvent) {
        if (chatEvent == null || chatEvent.getRawMessage().isBlank()) {
            return false;
        }

        try (ScamScreenerProfiler.Scope ignored = ScamScreenerProfiler.getInstance().scope("chat.local_echo", "  Local Echo Check")) {
            return MessageDispatcher.consumeLocalEcho(chatEvent.getRawMessage());
        }
    }
}
