package eu.tango.scamscreener.pipeline.state;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunnelStoreTest {
    @Test
    void recordsGenericAndExplicitStepsPerSender() {
        FunnelStore store = new FunnelStore();
        UUID senderUuid = UUID.randomUUID();
        ChatEvent event = new ChatEvent("add me on discord", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER);

        store.record(event);
        store.recordStep(
            new ChatEvent("pay first", senderUuid, "Alpha", 2_000L, ChatSourceType.PLAYER),
            FunnelStore.FunnelStep.PAYMENT,
            "pay first"
        );

        FunnelStore.FunnelSnapshot snapshot = store.snapshotFor(
            new ChatEvent("later", senderUuid, "Alpha", 3_000L, ChatSourceType.PLAYER)
        );

        assertTrue(snapshot.hasSender());
        assertEquals(2, snapshot.recentSteps().size());
        assertEquals(FunnelStore.FunnelStep.MESSAGE, snapshot.recentSteps().get(0));
        assertEquals(FunnelStore.FunnelStep.PAYMENT, snapshot.recentSteps().get(1));
        assertEquals(2, snapshot.evidences().size());
    }

    @Test
    void resetClearsFunnelHistory() {
        FunnelStore store = new FunnelStore();
        UUID senderUuid = UUID.randomUUID();
        store.record(new ChatEvent("hello", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));

        store.reset();
        FunnelStore.FunnelSnapshot snapshot = store.snapshotFor(
            new ChatEvent("later", senderUuid, "Alpha", 2_000L, ChatSourceType.PLAYER)
        );

        assertTrue(snapshot.hasSender());
        assertEquals(0, snapshot.recentSteps().size());
        assertEquals(0, snapshot.evidences().size());
    }

    @Test
    void configureTrimsBufferedHistory() {
        FunnelStore store = new FunnelStore();
        UUID senderUuid = UUID.randomUUID();
        store.record(new ChatEvent("one", senderUuid, "Alpha", 1_000L, ChatSourceType.PLAYER));
        store.record(new ChatEvent("two", senderUuid, "Alpha", 2_000L, ChatSourceType.PLAYER));
        store.record(new ChatEvent("three", senderUuid, "Alpha", 3_000L, ChatSourceType.PLAYER));

        store.configure(300_000L, 2);
        FunnelStore.FunnelSnapshot snapshot = store.snapshotFor(
            new ChatEvent("later", senderUuid, "Alpha", 4_000L, ChatSourceType.PLAYER)
        );

        assertEquals(2, snapshot.recentSteps().size());
        assertEquals(2, snapshot.evidences().size());
    }
}
