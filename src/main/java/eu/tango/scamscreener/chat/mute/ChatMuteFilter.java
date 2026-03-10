package eu.tango.scamscreener.chat.mute;

import eu.tango.scamscreener.ScamScreenerRuntime;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

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
            allowMessage(message == null ? "" : message.getString())
        );
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, timestamp) ->
            allowMessage(message == null ? "" : message.getString())
        );
    }

    private static boolean allowMessage(String rawMessage) {
        if (!ScamScreenerRuntime.getInstance().isEnabled()) {
            return true;
        }

        return !ScamScreenerRuntime.getInstance().mutePatternManager().shouldBlock(rawMessage);
    }
}
