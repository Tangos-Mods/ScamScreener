package eu.tango.scamscreener.chat;

import com.mojang.authlib.GameProfile;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ChatPipelineListenerTest {
    public static final class SenderNameParams {
        public String getName() {
            return "Pankraz01";
        }
    }

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

    @Test
    void disabledRuntimeSkipsPipelineProcessing() {
        assertEquals(false, ChatPipelineListener.shouldProcessChatEvent(
            ChatEvent.messageOnly("hello", ChatSourceType.PLAYER),
            false
        ));
    }

    @Test
    void senderlessChatMessagesAreClassifiedBeforePipelineEntry() {
        ChatEvent modEvent = ChatPipelineListener.classifyChatMessage(
            Text.literal("[SkyHanni] Visitor reward ready"),
            null,
            null,
            Instant.ofEpochMilli(1_000L),
            32767
        );
        assertEquals(ChatSourceType.SYSTEM, modEvent.getSourceType());
        assertEquals("", modEvent.getSenderName());
        assertEquals("[SkyHanni] Visitor reward ready", modEvent.getRawMessage());

        ChatEvent plainUnknownEvent = ChatPipelineListener.classifyChatMessage(
            Text.literal("Coins: +42"),
            null,
            null,
            Instant.ofEpochMilli(2_000L),
            32767
        );
        assertEquals(ChatSourceType.UNKNOWN, plainUnknownEvent.getSourceType());
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(plainUnknownEvent));
    }

    @Test
    void senderBackedChatMessagesStillEnterAsPlayerMessages() {
        ChatEvent playerEvent = ChatPipelineListener.classifyChatMessage(
            Text.literal("add me on discord"),
            new GameProfile(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), "Pankraz01"),
            null,
            Instant.ofEpochMilli(3_000L),
            32767
        );

        assertEquals(ChatSourceType.PLAYER, playerEvent.getSourceType());
        assertEquals("Pankraz01", playerEvent.getSenderName());
        assertEquals("add me on discord", playerEvent.getRawMessage());
    }

    @Test
    void senderBackedSystemPrefixDoesNotEnterAsPlayerMessage() {
        ChatEvent systemEvent = ChatPipelineListener.classifyChatMessage(
            Text.literal("[Skyblocker] BetterMap ready"),
            new GameProfile(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"), "Skyblocker"),
            null,
            Instant.ofEpochMilli(3_500L),
            32767
        );

        assertEquals(ChatSourceType.SYSTEM, systemEvent.getSourceType());
        assertEquals("", systemEvent.getSenderName());
        assertEquals("[Skyblocker] BetterMap ready", systemEvent.getRawMessage());
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(systemEvent));
    }

    @Test
    void paramBackedChatMessagesStillEnterAsPlayerMessages() {
        ChatEvent playerEvent = ChatPipelineListener.classifyChatMessage(
            Text.literal("add me on discord"),
            null,
            new SenderNameParams(),
            Instant.ofEpochMilli(4_000L),
            32767
        );

        assertEquals(ChatSourceType.PLAYER, playerEvent.getSourceType());
        assertEquals("Pankraz01", playerEvent.getSenderName());
        assertEquals("add me on discord", playerEvent.getRawMessage());
    }

    @Test
    void paramBackedSystemPrefixDoesNotEnterAsPlayerMessage() {
        ChatEvent systemEvent = ChatPipelineListener.classifyChatMessage(
            Text.literal("[Skyblocker] BetterMap ready"),
            null,
            new SenderNameParams(),
            Instant.ofEpochMilli(4_500L),
            32767
        );

        assertEquals(ChatSourceType.SYSTEM, systemEvent.getSourceType());
        assertEquals("", systemEvent.getSenderName());
        assertEquals("[Skyblocker] BetterMap ready", systemEvent.getRawMessage());
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(systemEvent));
    }
}
