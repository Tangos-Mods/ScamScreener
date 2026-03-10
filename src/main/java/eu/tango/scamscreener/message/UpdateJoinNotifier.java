package eu.tango.scamscreener.message;

import eu.tango.scamscreener.update.ModrinthUpdateChecker;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * Shows a local update notification when a newer Modrinth release is available.
 */
public final class UpdateJoinNotifier {
    private static boolean initialized;
    private static String lastNotifiedVersion = "";

    private UpdateJoinNotifier() {
    }

    /**
     * Registers the join-time update notification once.
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
            ModrinthUpdateChecker.checkOnJoin(updateInfo -> client.execute(() -> notifyOnce(updateInfo)))
        );
    }

    private static synchronized void notifyOnce(ModrinthUpdateChecker.UpdateInfo updateInfo) {
        if (updateInfo == null || updateInfo.latestVersion() == null || updateInfo.latestVersion().isBlank()) {
            return;
        }
        if (updateInfo.latestVersion().equalsIgnoreCase(lastNotifiedVersion)) {
            return;
        }

        lastNotifiedVersion = updateInfo.latestVersion();
        MessageDispatcher.reply(ClientMessages.updateAvailable(
            updateInfo.currentVersion(),
            updateInfo.latestVersion(),
            updateInfo.modrinthUrl(),
            updateInfo.changelog()
        ));
    }
}
