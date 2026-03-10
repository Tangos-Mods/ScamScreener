package eu.tango.scamscreener.chat;

import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.lists.Blacklist;
import eu.tango.scamscreener.lists.Whitelist;
import eu.tango.scamscreener.pipeline.core.PipelineEngine;
import eu.tango.scamscreener.pipeline.core.ScamScreenerPipelineFactory;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.pipeline.state.BehaviorStore;
import eu.tango.scamscreener.pipeline.state.FunnelStore;
import eu.tango.scamscreener.pipeline.state.TrendStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Manual inspection harness for arbitrary chat lines.
 *
     * <p>Edit {@code src/test/resources/manual-chat-inspection-input.txt}, then run:
     * {@code .\gradlew.bat manualChatInspection}
     *
     * <p>The test prints the result for each line to the terminal and also writes
     * the same output to a version-specific report in {@code build/reports/manual-chat-inspection/}.
 */
class ManualChatInspectionTest {
    private static final String INPUT_RESOURCE = "/manual-chat-inspection-input.txt";

    @Test
    void writesInspectionReportForConfiguredMessages() throws IOException {
        List<String> configuredMessages = loadConfiguredMessages();
        RecentChatCache recentChatCache = new RecentChatCache();
        PipelineEngine engine = ScamScreenerPipelineFactory.createDefaultEngine(
            new Whitelist(),
            new Blacklist(),
            new RulesConfig(),
            new BehaviorStore(),
            new TrendStore(),
            new FunnelStore(),
            recentChatCache,
            1
        );

        List<String> reportLines = new ArrayList<>();
        reportLines.add("Manual Chat Inspection Report");
        reportLines.add("Input file: src/test/resources/manual-chat-inspection-input.txt");
        reportLines.add("Messages are evaluated top-to-bottom through one shared pipeline instance.");
        reportLines.add("");

        if (configuredMessages.isEmpty()) {
            reportLines.add("No configured chat lines found.");
        }

        for (int index = 0; index < configuredMessages.size(); index++) {
            String rawLine = configuredMessages.get(index);
            InspectionResult result = inspect(rawLine, recentChatCache, engine);
            appendResult(reportLines, index + 1, rawLine, result);
        }

        Path reportPath = reportPath();
        Files.createDirectories(reportPath.getParent());
        Files.write(reportPath, reportLines, StandardCharsets.UTF_8);
        System.out.println(String.join(System.lineSeparator(), reportLines));
        System.out.println("Report written to: " + reportPath.toAbsolutePath());

        assertTrue(Files.exists(reportPath));
        assertTrue(Files.size(reportPath) > 0L);
    }

    private static InspectionResult inspect(String rawLine, RecentChatCache recentChatCache, PipelineEngine engine) {
        ChatLineClassifier.ChatLineType lineType = ChatLineClassifier.classify(rawLine);
        ChatEvent chatEvent = toChatEvent(rawLine, lineType);
        if (!ChatPipelineListener.shouldEnterPipeline(chatEvent)) {
            return new InspectionResult(lineType, null, null);
        }

        recentChatCache.record(chatEvent);
        PipelineDecision decision = engine.evaluate(chatEvent);
        return new InspectionResult(lineType, chatEvent, decision);
    }

    private static ChatEvent toChatEvent(String rawLine, ChatLineClassifier.ChatLineType lineType) {
        return switch (lineType == null ? ChatLineClassifier.ChatLineType.UNKNOWN : lineType) {
            case PLAYER -> {
                ChatLineClassifier.ParsedPlayerLine parsed = ChatLineClassifier.parsePlayerMessage(rawLine).orElse(null);
                if (parsed == null) {
                    yield null;
                }

                yield new ChatEvent(
                    parsed.message(),
                    null,
                    parsed.senderName(),
                    System.currentTimeMillis(),
                    ChatSourceType.PLAYER
                );
            }
            case SYSTEM -> ChatEvent.messageOnly(rawLine, ChatSourceType.SYSTEM);
            case UNKNOWN -> ChatEvent.messageOnly(rawLine, ChatSourceType.UNKNOWN);
            case IGNORED -> null;
        };
    }

    private static void appendResult(List<String> reportLines, int index, String rawLine, InspectionResult result) {
        reportLines.add("[" + index + "] " + rawLine);
        reportLines.add("  lineType: " + safeLineType(result));

        if (result == null || result.chatEvent() == null) {
            reportLines.add("  pipeline: SKIPPED");
            reportLines.add("");
            return;
        }

        ChatEvent chatEvent = result.chatEvent();
        PipelineDecision decision = result.decision();
        reportLines.add("  sourceType: " + chatEvent.getSourceType());
        reportLines.add("  sender: " + (chatEvent.getSenderName().isBlank() ? "-" : chatEvent.getSenderName()));
        reportLines.add("  effectiveMessage: " + chatEvent.getRawMessage());
        reportLines.add("  outcome: " + (decision == null ? "-" : decision.getOutcome()));
        reportLines.add("  score: " + (decision == null ? 0 : decision.getTotalScore()));
        reportLines.add("  reasons: " + formatReasons(decision));
        reportLines.add("");
    }

    private static String safeLineType(InspectionResult result) {
        if (result == null || result.lineType() == null) {
            return ChatLineClassifier.ChatLineType.UNKNOWN.name();
        }

        return result.lineType().name();
    }

    private static String formatReasons(PipelineDecision decision) {
        if (decision == null || decision.getReasons().isEmpty()) {
            return "-";
        }

        return String.join(" | ", decision.getReasons());
    }

    private static Path reportPath() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))) {
                return current.resolve("build")
                    .resolve("reports")
                    .resolve("manual-chat-inspection")
                    .resolve("report-" + ScamScreenerMod.MINECRAFT + ".txt");
            }

            current = current.getParent();
        }

        return Path.of("build", "reports", "manual-chat-inspection", "report-" + ScamScreenerMod.MINECRAFT + ".txt");
    }

    private static List<String> loadConfiguredMessages() throws IOException {
        try (InputStream stream = ManualChatInspectionTest.class.getResourceAsStream(INPUT_RESOURCE)) {
            if (stream == null) {
                return List.of();
            }

            List<String> configuredMessages = new ArrayList<>();
            for (String rawLine : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                configuredMessages.add(line);
            }

            return List.copyOf(configuredMessages);
        }
    }

    private record InspectionResult(
        ChatLineClassifier.ChatLineType lineType,
        ChatEvent chatEvent,
        PipelineDecision decision
    ) {
    }
}
