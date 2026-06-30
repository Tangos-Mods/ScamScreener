package eu.tango.scamscreener.chat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InboundMessageBypassRegistryTest {
    @AfterEach
    void tearDown() {
        InboundMessageBypassRegistry.clear();
    }

    @Test
    void consumesRememberedMessageOnce() {
        InboundMessageBypassRegistry.remember("[Companion] Visitor reward ready");

        assertEquals(true, InboundMessageBypassRegistry.consume("[Companion] Visitor reward ready"));
        assertEquals(false, InboundMessageBypassRegistry.consume("[Companion] Visitor reward ready"));
    }

    @Test
    void ignoresBlankMessages() {
        InboundMessageBypassRegistry.remember("   ");

        assertEquals(false, InboundMessageBypassRegistry.consume("   "));
        assertEquals(false, InboundMessageBypassRegistry.consume("real message"));
    }

    @Test
    void trimsMessagesForMatching() {
        InboundMessageBypassRegistry.remember("  [Companion] done  ");

        assertEquals(true, InboundMessageBypassRegistry.consume("[Companion] done"));
    }
}
