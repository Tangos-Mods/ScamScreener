package eu.tango.scamscreener.message;

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
        String title = decision != null && decision.getOutcome() == PipelineDecision.Outcome.BLOCK
            ? "BLOCK WARNING"
            : "RISK WARNING";
        String player = safePlayer(chatEvent);
        String score = decision == null ? "0" : String.valueOf(Math.max(0, decision.getTotalScore()));
        String reason = primaryReason(decision, "Suspicious message detected.");

        MutableText message = Text.empty()
            .append(Text.literal(WARNING_BORDER).formatted(Formatting.DARK_RED))
            .append(Text.literal("\n" + title).formatted(Formatting.DARK_RED, Formatting.BOLD))
            .append(Text.literal("\n" + player).formatted(Formatting.AQUA))
            .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
            .append(Text.literal(score).formatted(Formatting.GOLD, Formatting.BOLD))
            .append(Text.literal("\n" + reason).formatted(Formatting.YELLOW));

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
        String score = decision == null ? "0" : String.valueOf(Math.max(0, decision.getTotalScore()));
        String reason = primaryReason(decision, "Matched a blacklist entry.");

        MutableText message = Text.empty()
            .append(Text.literal(WARNING_BORDER).formatted(Formatting.DARK_RED))
            .append(Text.literal("\nBLACKLIST WARNING").formatted(Formatting.DARK_RED, Formatting.BOLD))
            .append(Text.literal("\n" + player).formatted(Formatting.AQUA))
            .append(Text.literal(" | ").formatted(Formatting.DARK_GRAY))
            .append(Text.literal(score).formatted(Formatting.GOLD, Formatting.BOLD))
            .append(Text.literal("\n" + reason).formatted(Formatting.YELLOW));

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
}
