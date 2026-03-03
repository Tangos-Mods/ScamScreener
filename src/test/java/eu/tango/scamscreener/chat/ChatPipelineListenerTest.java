package eu.tango.scamscreener.chat;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        ChatEvent unknownEvent = ChatPipelineListener.classifyGameMessage(
            Text.literal("You earned 10 SkyBlock XP."),
            32767
        );
        assertEquals(ChatSourceType.UNKNOWN, unknownEvent.getSourceType());
        assertEquals("", unknownEvent.getSenderName());
        assertEquals("You earned 10 SkyBlock XP.", unknownEvent.getRawMessage());
    }
}
