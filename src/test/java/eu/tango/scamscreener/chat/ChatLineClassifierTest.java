package eu.tango.scamscreener.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatLineClassifierTest {
    @Test
    void classifiesPlayerAndHypixelSystemLinesAsExpected() {
        String playerLine = "[134] [MVP+] Pankraz01: Ich spiele Hypixel Skyblock";
        String npcLine = "[NPC] Kat: Your Ocelot is ready to pick up!";
        String bossLine = "[BOSS] Necron: You never defeat me.";
        String bazaarLine = "[Bazaar] Cancelled buy order for Decombobulator.";

        assertEquals(ChatLineClassifier.ChatLineType.PLAYER, ChatLineClassifier.classify(playerLine));
        assertTrue(ChatLineClassifier.isPlayerMessage(playerLine));
        ChatLineClassifier.ParsedPlayerLine parsedPlayerLine = ChatLineClassifier.parsePlayerMessage(playerLine).orElse(null);
        assertNotNull(parsedPlayerLine);
        assertEquals("Pankraz01", parsedPlayerLine.senderName());
        assertEquals("Ich spiele Hypixel Skyblock", parsedPlayerLine.message());

        assertEquals(ChatLineClassifier.ChatLineType.SYSTEM, ChatLineClassifier.classify(npcLine));
        assertFalse(ChatLineClassifier.isPlayerMessage(npcLine));
        assertTrue(ChatLineClassifier.parsePlayerMessage(npcLine).isEmpty());

        assertEquals(ChatLineClassifier.ChatLineType.SYSTEM, ChatLineClassifier.classify(bossLine));
        assertFalse(ChatLineClassifier.isPlayerMessage(bossLine));
        assertTrue(ChatLineClassifier.parsePlayerMessage(bossLine).isEmpty());

        assertEquals(ChatLineClassifier.ChatLineType.SYSTEM, ChatLineClassifier.classify(bazaarLine));
        assertFalse(ChatLineClassifier.isPlayerMessage(bazaarLine));
        assertTrue(ChatLineClassifier.parsePlayerMessage(bazaarLine).isEmpty());
    }

    @Test
    void doesNotBlowUpOnLongNonMatchingLines() {
        String longUnknownLine = "[".repeat(5000) + "this is not chat";

        assertEquals(ChatLineClassifier.ChatLineType.UNKNOWN, ChatLineClassifier.classify(longUnknownLine));
        assertTrue(ChatLineClassifier.parsePlayerMessage(longUnknownLine).isEmpty());
    }
}
