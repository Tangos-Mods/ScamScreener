package eu.tango.scamscreener.message;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageDispatcherTest {
    @AfterEach
    void tearDown() {
        MessageDispatcher.clearLocalEchoes();
    }

    @Test
    void suppressesExactLocalEchoMessage() {
        MessageDispatcher.rememberLocalEchoText("[ScamScreener] test");

        assertTrue(MessageDispatcher.consumeLocalEcho("[ScamScreener] test"));
        assertFalse(MessageDispatcher.consumeLocalEcho("[ScamScreener] test"));
    }

    @Test
    void suppressesIndividualLinesFromMultilineMessages() {
        MessageDispatcher.rememberLocalEchoText("header\nreason line\nfooter");

        assertTrue(MessageDispatcher.consumeLocalEcho("reason line"));
        assertTrue(MessageDispatcher.consumeLocalEcho("footer"));
        assertFalse(MessageDispatcher.consumeLocalEcho("reason line"));
    }
}
