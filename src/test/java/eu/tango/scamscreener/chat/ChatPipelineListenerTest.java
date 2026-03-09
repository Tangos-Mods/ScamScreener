package eu.tango.scamscreener.chat;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatPipelineListenerTest {
    @Test
    void classifiesGameMessagesIntoPlayerSystemAndUnknown() {
        ChatEvent playerEvent = ChatPipelineListener.classifyGameMessage(
            Text.literal("[134] [MVP+] Pankraz01: add me on discord"),
            32767
        );
        assertEquals(ChatSourceType.PLAYER, playerEvent.getSourceType());
        assertEquals("Pankraz01", playerEvent.getSenderName());
        assertEquals("add me on discord", playerEvent.getRawMessage());

        ChatEvent systemEvent = ChatPipelineListener.classifyGameMessage(
            Text.literal("[NPC] Kat: Your Ocelot is ready to pick up!"),
            32767
        );
        assertEquals(ChatSourceType.SYSTEM, systemEvent.getSourceType());
        assertEquals("", systemEvent.getSenderName());
        assertEquals("[NPC] Kat: Your Ocelot is ready to pick up!", systemEvent.getRawMessage());

        ChatEvent modEvent = ChatPipelineListener.classifyGameMessage(
            Text.literal("[Skyblocker] BetterMap ready"),
            32767
        );
        assertEquals(ChatSourceType.SYSTEM, modEvent.getSourceType());
        assertEquals("", modEvent.getSenderName());
        assertEquals("[Skyblocker] BetterMap ready", modEvent.getRawMessage());

        ChatEvent emblemPlayerEvent = ChatPipelineListener.classifyGameMessage(
            Text.literal("[241] ? [MVP+] Pankraz01: add me on discord"),
            32767
        );
        assertEquals(ChatSourceType.PLAYER, emblemPlayerEvent.getSourceType());
        assertEquals("Pankraz01", emblemPlayerEvent.getSenderName());
        assertEquals("add me on discord", emblemPlayerEvent.getRawMessage());

        ChatEvent unknownEvent = ChatPipelineListener.classifyGameMessage(
            Text.literal("You earned 10 SkyBlock XP."),
            32767
        );
        assertEquals(ChatSourceType.UNKNOWN, unknownEvent.getSourceType());
        assertEquals("", unknownEvent.getSenderName());
        assertEquals("You earned 10 SkyBlock XP.", unknownEvent.getRawMessage());

        ChatEvent ignoredEvent = ChatPipelineListener.classifyGameMessage(
            Text.literal("[123] Auction Bot: deal now"),
            32767
        );
        assertNull(ignoredEvent);

        ChatEvent ignoredBracketedRankEvent = ChatPipelineListener.classifyGameMessage(
            Text.literal("[VIP] Sam: hi"),
            32767
        );
        assertNull(ignoredBracketedRankEvent);
    }

    @Test
    void onlyPlayerMessagesEnterPipeline() {
        assertEquals(true, ChatPipelineListener.shouldEnterPipeline(
            ChatEvent.messageOnly("hello", ChatSourceType.PLAYER)
        ));
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(
            ChatEvent.messageOnly("[NPC] hi", ChatSourceType.SYSTEM)
        ));
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(
            ChatEvent.messageOnly("You earned 10 SkyBlock XP.", ChatSourceType.UNKNOWN)
        ));
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(null));
    }
}
