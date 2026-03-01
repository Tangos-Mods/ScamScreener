package eu.tango.scamscreener.api.event;

import eu.tango.scamscreener.lists.WhitelistEntry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Public event fired after the shared whitelist changes.
 */
public final class WhitelistEvent {
    /**
     * Global whitelist change event.
     */
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(
        Listener.class,
        listeners -> (changeType, entry) -> {
            for (Listener listener : listeners) {
                listener.onWhitelistChanged(changeType, entry);
            }
        }
    );

    private WhitelistEvent() {
    }

    /**
     * Listener contract for whitelist change notifications.
     */
    @FunctionalInterface
    public interface Listener {
        /**
         * Called when the whitelist changes.
         *
         * <p>The entry is {@code null} for {@link PlayerListChangeType#CLEARED}
         * and {@link PlayerListChangeType#RELOADED}.
         *
         * @param changeType the type of list change
         * @param entry the changed entry, or {@code null} when the list was cleared
         */
        void onWhitelistChanged(PlayerListChangeType changeType, WhitelistEntry entry);
    }
}
