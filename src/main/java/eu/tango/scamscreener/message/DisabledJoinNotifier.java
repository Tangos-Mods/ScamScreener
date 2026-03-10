package eu.tango.scamscreener.message;

import eu.tango.scamscreener.ScamScreenerRuntime;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * Shows a one-time reminder on each server join when ScamScreener is disabled.
 */
public final class DisabledJoinNotifier {
    private static boolean initialized;

    private DisabledJoinNotifier() {
    }

    /**
     * Registers the join reminder once.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!ScamScreenerRuntime.getInstance().isEnabled()) {
                MessageDispatcher.reply(ClientMessages.scamScreenerDisabledJoinNotice());
            }
        });
    }
}
