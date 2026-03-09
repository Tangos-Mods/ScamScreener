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
        String modLine = "[Skyblocker] BetterMap ready";
        String systemColonLine = "Auction expires in 5m: use /ah to bid";

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

        assertEquals(ChatLineClassifier.ChatLineType.SYSTEM, ChatLineClassifier.classify(modLine));
        assertFalse(ChatLineClassifier.isPlayerMessage(modLine));
        assertTrue(ChatLineClassifier.parsePlayerMessage(modLine).isEmpty());

        assertEquals(ChatLineClassifier.ChatLineType.SYSTEM, ChatLineClassifier.classify(systemColonLine));
        assertFalse(ChatLineClassifier.isPlayerMessage(systemColonLine));
        assertTrue(ChatLineClassifier.parsePlayerMessage(systemColonLine).isEmpty());
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
        String emblemPublicLine = "[241] ? [MVP+] Pankraz01: add me on discord";

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

        ChatLineClassifier.ParsedPlayerLine emblemPublicMessage = ChatLineClassifier.parsePlayerMessage(emblemPublicLine).orElse(null);
        assertNotNull(emblemPublicMessage);
        assertEquals("Pankraz01", emblemPublicMessage.senderName());
        assertEquals("add me on discord", emblemPublicMessage.message());
    }

    @Test
    void ignoresInvalidPlayerLikeFormats() {
        String invalidPublicLine = "[123] Auction Bot: deal now";
        String invalidDirectMessageLine = "From: Server Team: maintenance";
        String unsupportedChannelLine = "Co-op > Sam: hi";
        String invalidBracketedRankLine = "[VIP] Sam: hi";

        assertEquals(ChatLineClassifier.ChatLineType.IGNORED, ChatLineClassifier.classify(invalidPublicLine));
        assertTrue(ChatLineClassifier.parsePlayerMessage(invalidPublicLine).isEmpty());

        assertEquals(ChatLineClassifier.ChatLineType.IGNORED, ChatLineClassifier.classify(invalidDirectMessageLine));
        assertTrue(ChatLineClassifier.parsePlayerMessage(invalidDirectMessageLine).isEmpty());

        assertEquals(ChatLineClassifier.ChatLineType.IGNORED, ChatLineClassifier.classify(unsupportedChannelLine));
        assertTrue(ChatLineClassifier.parsePlayerMessage(unsupportedChannelLine).isEmpty());

        assertEquals(ChatLineClassifier.ChatLineType.IGNORED, ChatLineClassifier.classify(invalidBracketedRankLine));
        assertTrue(ChatLineClassifier.parsePlayerMessage(invalidBracketedRankLine).isEmpty());
    }

    @Test
    void stripsVisiblePrefixesForUiDisplay() {
        assertEquals("legit middleman", ChatLineClassifier.displayMessageOnly("From: [MVP+] Sam: legit middleman"));
        assertEquals("invite me to discord", ChatLineClassifier.displayMessageOnly("Guild > [VIP] Sam: invite me to discord"));
        assertEquals("send coins first", ChatLineClassifier.displayMessageOnly("Party > Sam: send coins first"));
        assertEquals("Your Ocelot is ready to pick up!", ChatLineClassifier.displayMessageOnly("[NPC] Kat: Your Ocelot is ready to pick up!"));
        assertEquals("use /ah to bid", ChatLineClassifier.displayMessageOnly("Auction expires in 5m: use /ah to bid"));
        assertEquals("plain message", ChatLineClassifier.displayMessageOnly("plain message"));
    }
}
