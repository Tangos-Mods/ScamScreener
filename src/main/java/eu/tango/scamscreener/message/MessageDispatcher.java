package eu.tango.scamscreener.message;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Small client-side message bridge for local ScamScreener output.
 */
public final class MessageDispatcher {
    private MessageDispatcher() {
    }

    /**
     * Displays a local client-side chat message.
     *
     * @param text the message to show
     */
    public static void reply(Text text) {
        if (text == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }

        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(text, false);
            }
        });
    }
}
