package eu.tango.scamscreener.message;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Shared v1-style prefixed chat lines for local command feedback.
 */
public final class ClientMessages {
    private static final String PREFIX = "[ScamScreener] ";

    private ClientMessages() {
    }

    public static MutableText uiUnavailable() {
        return error("ScamScreener UI is not available right now.");
    }

    public static MutableText whitelistUpdateFailed() {
        return error("Whitelist update failed. Provide a valid player name or UUID.");
    }

    public static MutableText whitelistUpdated(String target) {
        return prefixed()
            .append(Text.literal("Whitelist updated: ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(target)).formatted(Formatting.AQUA))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText whitelistEntryMissing(String target) {
        return error("No whitelist entry found for " + displayValue(target) + ".");
    }

    public static MutableText whitelistRemoved(String target) {
        return prefixed()
            .append(Text.literal("Whitelist entry removed: ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(target)).formatted(Formatting.AQUA))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText whitelistCleared() {
        return prefixed().append(Text.literal("Whitelist cleared.").formatted(Formatting.GRAY));
    }

    public static MutableText blacklistUpdateFailed() {
        return error("Blacklist update failed. Provide a valid player name or UUID.");
    }

    public static MutableText blacklistUpdated(String target, int score) {
        return prefixed()
            .append(Text.literal("Blacklist updated: ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(target)).formatted(Formatting.AQUA))
            .append(Text.literal(" (score ").formatted(Formatting.GRAY))
            .append(Text.literal(String.valueOf(Math.max(0, score))).formatted(Formatting.DARK_RED, Formatting.BOLD))
            .append(Text.literal(").").formatted(Formatting.GRAY));
    }

    public static MutableText blacklistEntryMissing(String target) {
        return error("No blacklist entry found for " + displayValue(target) + ".");
    }

    public static MutableText blacklistRemoved(String target) {
        return prefixed()
            .append(Text.literal("Blacklist entry removed: ").formatted(Formatting.GRAY))
            .append(Text.literal(displayValue(target)).formatted(Formatting.AQUA))
            .append(Text.literal(".").formatted(Formatting.GRAY));
    }

    public static MutableText blacklistCleared() {
        return prefixed().append(Text.literal("Blacklist cleared.").formatted(Formatting.GRAY));
    }

    private static MutableText error(String message) {
        return prefixed().append(Text.literal(message == null ? "" : message).formatted(Formatting.RED));
    }

    private static MutableText prefixed() {
        return Text.literal(PREFIX).formatted(Formatting.DARK_RED);
    }

    private static String displayValue(String value) {
        if (value == null || value.isBlank()) {
            return "<unknown>";
        }

        return value.trim();
    }
}
