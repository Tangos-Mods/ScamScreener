package eu.tango.scamscreener.pipeline.state;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorStoreTest {
    @Test
    void tracksRecentMessagesPerSender() {
        BehaviorStore store = new BehaviorStore();
        UUID senderUuid = UUID.randomUUID();

        store.record(new ChatEvent("hello", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));
        store.record(new ChatEvent("hello", senderUuid, "Alpha", 2_000L, ChatSourceType.PLAYER));

        BehaviorStore.BehaviorSnapshot snapshot = store.snapshotFor(
            new ChatEvent("hello", senderUuid, "Alpha", 3_000L, ChatSourceType.PLAYER)
        );

        assertTrue(snapshot.hasSender());
        assertEquals(2, snapshot.recentMessageCount());
        assertEquals(2, snapshot.sameMessageCount());
        assertEquals(2, snapshot.recentMessages().size());
    }

    @Test
    void resetClearsBehaviorHistory() {
        BehaviorStore store = new BehaviorStore();
        UUID senderUuid = UUID.randomUUID();
        store.record(new ChatEvent("hello", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));

        store.reset();
        BehaviorStore.BehaviorSnapshot snapshot = store.snapshotFor(
            new ChatEvent("hello", senderUuid, "Alpha", 2_000L, ChatSourceType.PLAYER)
        );

        assertTrue(snapshot.hasSender());
        assertEquals(0, snapshot.recentMessageCount());
        assertEquals(0, snapshot.sameMessageCount());
    }

    @Test
    void configureTrimsBufferedHistory() {
        BehaviorStore store = new BehaviorStore();
        UUID senderUuid = UUID.randomUUID();
        store.record(new ChatEvent("one", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));
        store.record(new ChatEvent("two", senderUuid, "Alpha", 2_000L, ChatSourceType.PLAYER));
        store.record(new ChatEvent("three", senderUuid, "Alpha", 3_000L, ChatSourceType.PLAYER));

        store.configure(90_000L, 2);
        BehaviorStore.BehaviorSnapshot snapshot = store.snapshotFor(
            new ChatEvent("three", senderUuid, "Alpha", 4_000L, ChatSourceType.PLAYER)
        );

        assertEquals(2, snapshot.recentMessageCount());
        assertEquals(2, snapshot.recentMessages().size());
    }
}
