package eu.tango.scamscreener.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatLineClassifierTest {
    @Test
    void classifiesPlayerAndHypixelSystemLinesAsExpected() {
        String playerLine = "[241] [MVP+] Pankraz01: Ich spiele Hypixel Skyblock";
        String npcLine = "[NPC] Kat: Your Ocelot is ready to pick up!";
        String bossLine = "[BOSS] Necron: You never defeat me.";
        String bazaarLine = "[Bazaar] Cancelled buy order for Decombobulator.";
        String serverColonLine = "Auction expires in 5m: use /ah to bid";

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

        assertEquals(ChatLineClassifier.ChatLineType.UNKNOWN, ChatLineClassifier.classify(serverColonLine));
        assertFalse(ChatLineClassifier.isPlayerMessage(serverColonLine));
        assertTrue(ChatLineClassifier.parsePlayerMessage(serverColonLine).isEmpty());
    }

    @Test
    void doesNotBlowUpOnLongNonMatchingLines() {
        String longUnknownLine = "[".repeat(5000) + "this is not chat";

        assertEquals(ChatLineClassifier.ChatLineType.UNKNOWN, ChatLineClassifier.classify(longUnknownLine));
        assertTrue(ChatLineClassifier.parsePlayerMessage(longUnknownLine).isEmpty());
    }

    @Test
    void parsesDirectGuildAndPartyLines() {
        String directMessageLine = "From: [MVP+] Sam: legit middleman";
        String guildLine = "Guild > [VIP] Sam: invite me to discord";
        String partyLine = "Party > Sam: send coins first";

        ChatLineClassifier.ParsedPlayerLine directMessage = ChatLineClassifier.parsePlayerMessage(directMessageLine).orElse(null);
        assertNotNull(directMessage);
        assertEquals("Sam", directMessage.senderName());
        assertEquals("legit middleman", directMessage.message());

        ChatLineClassifier.ParsedPlayerLine guildMessage = ChatLineClassifier.parsePlayerMessage(guildLine).orElse(null);
        assertNotNull(guildMessage);
        assertEquals("Sam", guildMessage.senderName());
        assertEquals("invite me to discord", guildMessage.message());

        ChatLineClassifier.ParsedPlayerLine partyMessage = ChatLineClassifier.parsePlayerMessage(partyLine).orElse(null);
        assertNotNull(partyMessage);
        assertEquals("Sam", partyMessage.senderName());
        assertEquals("send coins first", partyMessage.message());
    }

    @Test
    void stripsVisiblePrefixesForUiDisplay() {
        assertEquals("legit middleman", ChatLineClassifier.displayMessageOnly("From: [MVP+] Sam: legit middleman"));
        assertEquals("invite me to discord", ChatLineClassifier.displayMessageOnly("Guild > [VIP] Sam: invite me to discord"));
        assertEquals("send coins first", ChatLineClassifier.displayMessageOnly("Party > Sam: send coins first"));
        assertEquals("Your Ocelot is ready to pick up!", ChatLineClassifier.displayMessageOnly("[NPC] Kat: Your Ocelot is ready to pick up!"));
        assertEquals("plain message", ChatLineClassifier.displayMessageOnly("plain message"));
    }
}
