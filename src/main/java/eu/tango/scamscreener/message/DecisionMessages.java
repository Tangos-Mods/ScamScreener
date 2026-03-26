package eu.tango.scamscreener.message;

import eu.tango.scamscreener.chat.ChatLineClassifier;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import eu.tango.scamscreener.pipeline.data.ChatEvent;

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
    public static MutableComponent riskWarning(ChatEvent chatEvent, PipelineDecision decision) {
        String player = safePlayer(chatEvent);
        int scoreValue = decision == null ? 0 : Math.max(0, decision.getTotalScore());
        AlertSeverity severity = AlertSeverity.fromDecision(decision);
        String title = severity.name() + " RISK MESSAGE";
        String score = String.valueOf(scoreValue);
        String playerScoreLine = player + " | " + score;
        String alertContextId = AlertContextRegistry.register(chatEvent, decision);

        MutableComponent message = Component.empty()
            .append(Component.literal(WARNING_BORDER).withStyle(ChatFormatting.DARK_RED))
            .append(Component.literal("\n" + centeredBold(title)).withStyle(severityColor(severity), ChatFormatting.BOLD))
            .append(Component.literal("\n" + leadingPadding(playerScoreLine)))
            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(score).withStyle(scoreColor(scoreValue), ChatFormatting.BOLD))
            .append(Component.literal("\n"))
            .append(actionLine(alertContextId));

        return message.append(Component.literal("\n" + WARNING_BORDER).withStyle(ChatFormatting.DARK_RED));
    }

    /**
     * Creates the warning shown for explicit blacklist matches.
     *
     * @param chatEvent the evaluated chat event
     * @param decision the final pipeline decision
     * @return the formatted blacklist warning
     */
    public static MutableComponent blacklistWarning(ChatEvent chatEvent, PipelineDecision decision) {
        String player = safePlayer(chatEvent);
        int scoreValue = decision == null ? 0 : Math.max(0, decision.getTotalScore());
        String score = String.valueOf(scoreValue);
        String reason = primaryReason(decision, "Matched a blacklist entry.");
        String playerScoreLine = player + " | " + score;

        MutableComponent message = Component.empty()
            .append(Component.literal(WARNING_BORDER).withStyle(ChatFormatting.DARK_RED))
            .append(Component.literal("\n" + centeredBold("BLACKLIST WARNING")).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
            .append(Component.literal("\n" + leadingPadding(playerScoreLine)))
            .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(score).withStyle(scoreColor(scoreValue), ChatFormatting.BOLD))
            .append(Component.literal("\n" + centered(reason)).withStyle(ChatFormatting.YELLOW));

        return message.append(Component.literal("\n" + WARNING_BORDER).withStyle(ChatFormatting.DARK_RED));
    }

    private static String safePlayer(ChatEvent chatEvent) {
        if (chatEvent == null || chatEvent.getSenderName() == null || chatEvent.getSenderName().isBlank()) {
            if (chatEvent != null) {
                ChatLineClassifier.ParsedPlayerLine parsedPlayerLine = ChatLineClassifier.parsePlayerMessage(chatEvent.getRawMessage()).orElse(null);
                if (parsedPlayerLine != null && !parsedPlayerLine.senderName().isBlank()) {
                    return parsedPlayerLine.senderName();
                }
            }
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

    private static ChatFormatting severityColor(AlertSeverity severity) {
        return switch (severity == null ? AlertSeverity.LOW : severity) {
            case LOW -> ChatFormatting.RED;
            case MEDIUM -> ChatFormatting.GOLD;
            case HIGH -> ChatFormatting.RED;
            default -> ChatFormatting.DARK_RED;
        };
    }

    private static ChatFormatting scoreColor(int score) {
        if (score >= 75) {
            return ChatFormatting.DARK_RED;
        }
        if (score >= 50) {
            return ChatFormatting.RED;
        }
        if (score >= 25) {
            return ChatFormatting.GOLD;
        }
        return ChatFormatting.YELLOW;
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

        Minecraft client = Minecraft.getInstance();
        if (client != null && client.font != null) {
            int targetWidth = client.font.width(WARNING_BORDER);
            int textWidth = client.font.width(text);
            if (bold) {
                textWidth += estimateBoldExtraPixels(text);
            }
            if (textWidth >= targetWidth) {
                return "";
            }

            int spaceWidth = Math.max(1, client.font.width(" "));
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

    private static MutableComponent actionLine(String alertContextId) {
        MutableComponent line = Component.literal(leadingPadding("[review] [info]"));
        boolean hasContext = alertContextId != null && !alertContextId.isBlank();

        line.append(actionTag(
            "review",
            ChatFormatting.GOLD,
            "Open the case review window to group context messages and assign signal tags.",
            hasContext ? "/scamscreener review manage " + alertContextId : null
        ));
        line.append(Component.literal(" "));
        line.append(actionTag(
            "info",
            ChatFormatting.YELLOW,
            "Open rule detail window for this alert.",
            hasContext ? "/scamscreener review info " + alertContextId : null
        ));

        return line;
    }

    private static MutableComponent actionTag(String label, ChatFormatting color, String hover, String command) {
        Style style = Style.EMPTY.withColor(color);
        if (hover != null && !hover.isBlank()) {
            style = style.withHoverEvent(new HoverEvent.ShowText(Component.literal(hover)));
        }
        if (command != null && !command.isBlank()) {
            style = style.withClickEvent(new ClickEvent.RunCommand(command));
        } else {
            style = style.withStrikethrough(true);
        }

        return Component.literal("[" + label + "]").setStyle(style);
    }
}
