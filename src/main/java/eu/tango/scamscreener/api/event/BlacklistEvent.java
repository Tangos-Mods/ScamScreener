package eu.tango.scamscreener.api.event;

import eu.tango.scamscreener.lists.BlacklistEntry;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Public event fired after the shared blacklist changes.
 */
public final class BlacklistEvent {
    /**
     * Global blacklist change event.
     */
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(
        Listener.class,
        listeners -> (changeType, entry) -> {
            for (Listener listener : listeners) {
                listener.onBlacklistChanged(changeType, entry);
            }
        }
    );

    private BlacklistEvent() {
    }

    /**
     * Listener contract for blacklist change notifications.
     */
    @FunctionalInterface
    public interface Listener {
        /**
         * Called when the blacklist changes.
         *
         * <p>The entry is {@code null} for {@link PlayerListChangeType#CLEARED}
         * and {@link PlayerListChangeType#RELOADED}.
         *
         * @param changeType the type of list change
         * @param entry the changed entry, or {@code null} when the list was cleared
         */
        void onBlacklistChanged(PlayerListChangeType changeType, BlacklistEntry entry);
    }
}
