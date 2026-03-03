package eu.tango.scamscreener.message;

import net.minecraft.client.MinecraftClient;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Minimal user-facing chat messages for pipeline decisions.
 *
 * <p>This intentionally keeps the classic warning-box behavior from v1 while
 * staying small enough for the current v2 pipeline state.
 */
public final class DecisionMessages {
    private static final String WARNING_BORDER = "====================================";

    private DecisionMessages() {
    }

    /**
     * Creates the warning shown for heuristic risk decisions.
     *
     * @param chatEvent the evaluated chat event
     * @param decision the final pipeline decision
     * @return the formatted warning message
     */
    public static MutableText riskWarning(ChatEvent chatEvent, PipelineDecision decision) {
        String player = safePlayer(chatEvent);
        int scoreValue = decision == null ? 0 : Math.max(0, decision.getTotalScore());
        String title = severityLabel(decision, scoreValue) + " RISK MESSAGE";
        String score = String.valueOf(scoreValue);
        String reason = primaryReason(decision, "Suspicious message detected.");
        String playerScoreLine = player + " | " + score;

        MutableText message = Text.empty()
            .append(Text.literal(WARNING_BORDER).formatted(Formatting.DARK_RED))
            .append(Text.literal("\n" + centeredBold(title)).formatted(severityColor(decision, scoreValue), Formatting.BOLD))
            .append(Text.literal("\n" + leadingPadding(playerScoreLine)))
            .append(Text.literal(player).formatted(Formatting.AQUA))
            .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
            .append(Text.literal(score).formatted(scoreColor(scoreValue), Formatting.BOLD))
            .append(Text.literal("\n" + centered(reason)).formatted(Formatting.YELLOW));

        return message.append(Text.literal("\n" + WARNING_BORDER).formatted(Formatting.DARK_RED));
    }

    /**
     * Creates the warning shown for explicit blacklist matches.
     *
     * @param chatEvent the evaluated chat event
     * @param decision the final pipeline decision
     * @return the formatted blacklist warning
     */
    public static MutableText blacklistWarning(ChatEvent chatEvent, PipelineDecision decision) {
        String player = safePlayer(chatEvent);
        int scoreValue = decision == null ? 0 : Math.max(0, decision.getTotalScore());
        String score = String.valueOf(scoreValue);
        String reason = primaryReason(decision, "Matched a blacklist entry.");
        String playerScoreLine = player + " | " + score;

        MutableText message = Text.empty()
            .append(Text.literal(WARNING_BORDER).formatted(Formatting.DARK_RED))
            .append(Text.literal("\n" + centeredBold("BLACKLIST WARNING")).formatted(Formatting.DARK_RED, Formatting.BOLD))
            .append(Text.literal("\n" + leadingPadding(playerScoreLine)))
            .append(Text.literal(player).formatted(Formatting.AQUA))
            .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
            .append(Text.literal(score).formatted(scoreColor(scoreValue), Formatting.BOLD))
            .append(Text.literal("\n" + centered(reason)).formatted(Formatting.YELLOW));

        return message.append(Text.literal("\n" + WARNING_BORDER).formatted(Formatting.DARK_RED));
    }

    private static String safePlayer(ChatEvent chatEvent) {
        if (chatEvent == null || chatEvent.getSenderName() == null || chatEvent.getSenderName().isBlank()) {
            return "unknown";
        }

        return chatEvent.getSenderName().trim();
    }

    private static String primaryReason(PipelineDecision decision, String fallback) {
        if (decision == null || decision.getReasons().isEmpty()) {
            return fallback;
        }

        String reason = decision.getReasons().get(0);
        if (reason == null || reason.isBlank()) {
            return fallback;
        }

        return reason;
    }

    private static String severityLabel(PipelineDecision decision, int score) {
        if (decision != null && decision.getOutcome() == PipelineDecision.Outcome.BLOCK) {
            return "CRITICAL";
        }
        if (score >= 75) {
            return "CRITICAL";
        }
        if (score >= 50) {
            return "HIGH";
        }
        if (score >= 25) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private static Formatting severityColor(PipelineDecision decision, int score) {
        String severity = severityLabel(decision, score);
        return switch (severity) {
            case "LOW" -> Formatting.RED;
            case "MEDIUM" -> Formatting.GOLD;
            case "HIGH" -> Formatting.RED;
            default -> Formatting.DARK_RED;
        };
    }

    private static Formatting scoreColor(int score) {
        if (score >= 75) {
            return Formatting.DARK_RED;
        }
        if (score >= 50) {
            return Formatting.RED;
        }
        if (score >= 25) {
            return Formatting.GOLD;
        }
        return Formatting.YELLOW;
    }

    private static String centered(String text) {
        if (text == null) {
            return "";
        }

        String padding = leadingPadding(text);
        return padding.isEmpty() ? text : padding + text;
    }

    private static String centeredBold(String text) {
        if (text == null) {
            return "";
        }

        String padding = leadingPadding(text, true);
        return padding.isEmpty() ? text : padding + text;
    }

    private static String leadingPadding(String text) {
        return leadingPadding(text, false);
    }

    private static String leadingPadding(String text, boolean bold) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.textRenderer != null) {
            int targetWidth = client.textRenderer.getWidth(WARNING_BORDER);
            int textWidth = client.textRenderer.getWidth(text);
            if (bold) {
                textWidth += estimateBoldExtraPixels(text);
            }
            if (textWidth >= targetWidth) {
                return "";
            }

            int spaceWidth = Math.max(1, client.textRenderer.getWidth(" "));
            int leftPixels = (targetWidth - textWidth) / 2;
            int spaceCount = Math.max(0, (leftPixels + (spaceWidth / 2)) / spaceWidth);
            return " ".repeat(spaceCount);
        }

        if (text.length() >= WARNING_BORDER.length()) {
            return "";
        }

        int leftPadding = (WARNING_BORDER.length() - text.length()) / 2;
        return " ".repeat(Math.max(0, leftPadding));
    }

    private static int estimateBoldExtraPixels(String text) {
        int extra = 0;
        for (int index = 0; index < text.length(); index++) {
            if (!Character.isWhitespace(text.charAt(index))) {
                extra++;
            }
        }

        return extra;
    }
}
