package eu.tango.scamscreener.pipeline.state;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrendStoreTest {
    @Test
    void tracksMatchingMessagesAcrossDifferentSenders() {
        TrendStore store = new TrendStore();

        store.record(new ChatEvent("add me on discord", UUID.randomUUID(), "Alpha", 1_000L, ChatSourceType.PLAYER));
        store.record(new ChatEvent("add me on discord", UUID.randomUUID(), "Beta", 2_000L, ChatSourceType.PLAYER));

        TrendStore.TrendSnapshot snapshot = store.snapshotFor(
            new ChatEvent("add me on discord", UUID.randomUUID(), "Gamma", 3_000L, ChatSourceType.PLAYER)
        );

        assertTrue(snapshot.hasTrend());
        assertEquals(2, snapshot.matchingMessageCount());
        assertEquals(2, snapshot.distinctSenderCount());
        assertEquals(2, snapshot.matchingSenderKeys().size());
    }

    @Test
    void ignoresSameSenderForTrendMatching() {
        TrendStore store = new TrendStore();
        UUID senderUuid = UUID.randomUUID();
        store.record(new ChatEvent("add me on discord", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));

        TrendStore.TrendSnapshot snapshot = store.snapshotFor(
            new ChatEvent("add me on discord", senderUuid, "Alpha", 2_000L, ChatSourceType.PLAYER)
        );

        assertFalse(snapshot.hasTrend());
        assertEquals(0, snapshot.matchingMessageCount());
        assertEquals(0, snapshot.distinctSenderCount());
    }

    @Test
    void configureTrimsBufferedHistory() {
        TrendStore store = new TrendStore();

        store.record(new ChatEvent("same", UUID.randomUUID(), "Alpha", 1_000L, ChatSourceType.PLAYER));
        store.record(new ChatEvent("same", UUID.randomUUID(), "Beta", 2_000L, ChatSourceType.PLAYER));
        store.record(new ChatEvent("same", UUID.randomUUID(), "Gamma", 3_000L, ChatSourceType.PLAYER));

        store.configure(120_000L, 2);
        TrendStore.TrendSnapshot snapshot = store.snapshotFor(
            new ChatEvent("same", UUID.randomUUID(), "Delta", 4_000L, ChatSourceType.PLAYER)
        );

        assertEquals(2, snapshot.matchingMessageCount());
        assertEquals(2, snapshot.distinctSenderCount());
    }
}
