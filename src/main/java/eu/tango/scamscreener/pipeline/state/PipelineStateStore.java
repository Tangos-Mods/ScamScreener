package eu.tango.scamscreener.pipeline.state;

import eu.tango.scamscreener.pipeline.data.ChatEvent;

/**
 * Shared contract for stateful pipeline stores.
 *
 * @param <T> the immutable snapshot type exposed by the store
 */
public interface PipelineStateStore<T> {
    /**
     * Returns the current state snapshot for the given event without mutating history.
     *
     * @param chatEvent the event to inspect
     * @return the current snapshot for this store
     */
    T snapshotFor(ChatEvent chatEvent);

    /**
     * Records one event into the store history.
     *
     * @param chatEvent the event to append
     */
    void record(ChatEvent chatEvent);

    /**
     * Clears the in-memory state maintained by the store.
     */
    void reset();
}
