package eu.tango.scamscreener.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        assertEquals(ChatLineClassifier.ChatLineType.SYSTEM, ChatLineClassifier.classify(npcLine));
        assertFalse(ChatLineClassifier.isPlayerMessage(npcLine));

        assertEquals(ChatLineClassifier.ChatLineType.SYSTEM, ChatLineClassifier.classify(bossLine));
        assertFalse(ChatLineClassifier.isPlayerMessage(bossLine));

        assertEquals(ChatLineClassifier.ChatLineType.SYSTEM, ChatLineClassifier.classify(bazaarLine));
        assertFalse(ChatLineClassifier.isPlayerMessage(bazaarLine));
    }
}
