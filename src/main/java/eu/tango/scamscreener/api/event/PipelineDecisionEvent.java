package eu.tango.scamscreener.api.event;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Public event fired after ScamScreener produces a final pipeline decision.
 */
public final class PipelineDecisionEvent {
    /**
     * Global pipeline decision event.
     */
    public static final Event<Listener> EVENT = EventFactory.createArrayBacked(
        Listener.class,
        listeners -> (chatEvent, decision) -> {
            for (Listener listener : listeners) {
                listener.onPipelineDecision(chatEvent, decision);
            }
        }
    );

    private PipelineDecisionEvent() {
    }

    /**
     * Listener contract for pipeline decision notifications.
     */
    @FunctionalInterface
    public interface Listener {
        /**
         * Called when the pipeline finishes evaluating a chat event.
         *
         * @param chatEvent the evaluated inbound chat event
         * @param decision the final pipeline decision
         */
        void onPipelineDecision(ChatEvent chatEvent, PipelineDecision decision);
    }
}
