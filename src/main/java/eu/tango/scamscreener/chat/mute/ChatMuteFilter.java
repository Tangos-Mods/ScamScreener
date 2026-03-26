package eu.tango.scamscreener.chat.mute;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.profiler.ScamScreenerProfiler;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.network.chat.Component;

/**
 * Early inbound chat filter that restores the old manual mute-pattern flow.
 */
public final class ChatMuteFilter {
    private static boolean initialized;

    private ChatMuteFilter() {
    }

    /**
     * Registers the mute filter once.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) ->
            allowGameMessage(message, overlay)
        );
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, timestamp) ->
            allowChatMessage(message)
        );
    }

    private static boolean allowGameMessage(Component message, boolean overlay) {
        if (overlay) {
            return true;
        }

        return allowMessage(message);
    }

    private static boolean allowChatMessage(Component message) {
        return allowMessage(message);
    }

    private static boolean allowMessage(Component message) {
        ScamScreenerRuntime runtime = ScamScreenerRuntime.getInstance();
        MutePatternManager mutePatternManager = runtime.mutePatternManager();
        if (!runtime.isEnabled() || !mutePatternManager.isEnabled()) {
            return true;
        }

        String rawMessage = message == null ? "" : message.getString();
        boolean blocked;
        try (ScamScreenerProfiler.Scope ignored = ScamScreenerProfiler.getInstance().scope("mute.filter", "Mute Filter")) {
            blocked = mutePatternManager.shouldBlock(rawMessage);
        }
        if (blocked) {
            ScamScreenerProfiler.getInstance().recordSummary("Mute filter blocked inbound chat");
        }

        return !blocked;
    }
}
