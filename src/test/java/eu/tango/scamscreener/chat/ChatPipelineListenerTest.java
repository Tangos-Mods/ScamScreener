package eu.tango.scamscreener.chat;

import com.mojang.authlib.GameProfile;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import net.minecraft.network.chat.Component;
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
            Component.literal("[134] [MVP+] Pankraz01: add me on discord"),
            32767
        );
        assertEquals(ChatSourceType.PLAYER, playerEvent.getSourceType());
        assertEquals("Pankraz01", playerEvent.getSenderName());
        assertEquals("add me on discord", playerEvent.getRawMessage());

        ChatEvent systemEvent = ChatPipelineListener.classifyGameMessage(
            Component.literal("[NPC] Kat: Your Ocelot is ready to pick up!"),
            32767
        );
        assertEquals(ChatSourceType.SYSTEM, systemEvent.getSourceType());
        assertEquals("", systemEvent.getSenderName());
        assertEquals("[NPC] Kat: Your Ocelot is ready to pick up!", systemEvent.getRawMessage());

        ChatEvent modEvent = ChatPipelineListener.classifyGameMessage(
            Component.literal("[Skyblocker] BetterMap ready"),
            32767
        );
        assertEquals(ChatSourceType.SYSTEM, modEvent.getSourceType());
        assertEquals("", modEvent.getSenderName());
        assertEquals("[Skyblocker] BetterMap ready", modEvent.getRawMessage());

        ChatEvent emblemPlayerEvent = ChatPipelineListener.classifyGameMessage(
            Component.literal("[241] ? [MVP+] Pankraz01: add me on discord"),
            32767
        );
        assertEquals(ChatSourceType.PLAYER, emblemPlayerEvent.getSourceType());
        assertEquals("Pankraz01", emblemPlayerEvent.getSenderName());
        assertEquals("add me on discord", emblemPlayerEvent.getRawMessage());

        ChatEvent unknownEvent = ChatPipelineListener.classifyGameMessage(
            Component.literal("You earned 10 SkyBlock XP."),
            32767
        );
        assertEquals(ChatSourceType.UNKNOWN, unknownEvent.getSourceType());
        assertEquals("", unknownEvent.getSenderName());
        assertEquals("You earned 10 SkyBlock XP.", unknownEvent.getRawMessage());

        ChatEvent ignoredEvent = ChatPipelineListener.classifyGameMessage(
            Component.literal("[123] Auction Bot: deal now"),
            32767
        );
        assertNull(ignoredEvent);

        ChatEvent ignoredBracketedRankEvent = ChatPipelineListener.classifyGameMessage(
            Component.literal("[VIP] Sam: hi"),
            32767
        );
        assertNull(ignoredBracketedRankEvent);
    }

    @Test
    void reclassifiesWrappedDungeonModMessagesAsSystemMessages() {
        ChatEvent scoreEvent = ChatPipelineListener.classifyGameMessage(
            Component.literal("[130] [MVP+] Pankraz01: [Skyblocker] 300 Score Reached!"),
            32767
        );
        assertEquals(ChatSourceType.SYSTEM_PLAYER, scoreEvent.getSourceType());
        assertEquals("Pankraz01", scoreEvent.getSenderName());
        assertEquals("[Skyblocker] 300 Score Reached!", scoreEvent.getRawMessage());
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(scoreEvent));

        ChatEvent cryptEvent = ChatPipelineListener.classifyGameMessage(
            Component.literal("[130] [MVP+] Pankraz01: [Skyblocker] We only have 4 crypts out of 5, we need more!"),
            32767
        );
        assertEquals(ChatSourceType.SYSTEM_PLAYER, cryptEvent.getSourceType());
        assertEquals("Pankraz01", cryptEvent.getSenderName());
        assertEquals("[Skyblocker] We only have 4 crypts out of 5, we need more!", cryptEvent.getRawMessage());
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(cryptEvent));

        ChatEvent prefixedLividEvent = ChatPipelineListener.classifyGameMessage(
            Component.literal("[130] [MVP+] Pankraz01: [Skyblocker] The livid color is LIME"),
            32767
        );
        assertEquals(ChatSourceType.SYSTEM_PLAYER, prefixedLividEvent.getSourceType());
        assertEquals("Pankraz01", prefixedLividEvent.getSenderName());
        assertEquals("[Skyblocker] The livid color is LIME", prefixedLividEvent.getRawMessage());
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(prefixedLividEvent));
    }

    @Test
    void fallbackFilterStillCatchesForcedPlayerDungeonModMessages() {
        ChatEvent scoreEvent = ChatEvent.messageOnly("[Skyblocker] 300 Score Reached!", ChatSourceType.PLAYER);
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(scoreEvent));

        ChatEvent mimicEvent = ChatEvent.messageOnly("Mimic dead!", ChatSourceType.PLAYER);
        assertEquals("Mimic dead!", mimicEvent.getRawMessage());
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(mimicEvent));

        ChatEvent lividEvent = ChatEvent.messageOnly("The Livid color is RED", ChatSourceType.PLAYER);
        assertEquals("The Livid color is RED", lividEvent.getRawMessage());
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(lividEvent));

        ChatEvent percentageEvent = ChatEvent.messageOnly("0 (0.00%)", ChatSourceType.PLAYER);
        assertEquals("0 (0.00%)", percentageEvent.getRawMessage());
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(percentageEvent));
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
            ChatEvent.messageOnly("[Skyblocker] BetterMap ready", ChatSourceType.PLAYER)
        ));
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(
            ChatEvent.messageOnly("The livid color is PURPLE", ChatSourceType.PLAYER)
        ));
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(
            ChatEvent.messageOnly("300 Score Reached!", ChatSourceType.PLAYER)
        ));
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(
            ChatEvent.messageOnly("We only have 4 crypts out of 5, we need more!", ChatSourceType.PLAYER)
        ));
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(
            ChatEvent.messageOnly("0 (0.00%)", ChatSourceType.PLAYER)
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
            Component.literal("[SkyHanni] Visitor reward ready"),
            null,
            null,
            Instant.ofEpochMilli(1_000L),
            32767
        );
        assertEquals(ChatSourceType.SYSTEM, modEvent.getSourceType());
        assertEquals("", modEvent.getSenderName());
        assertEquals("[SkyHanni] Visitor reward ready", modEvent.getRawMessage());

        ChatEvent plainUnknownEvent = ChatPipelineListener.classifyChatMessage(
            Component.literal("Coins: +42"),
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
            Component.literal("add me on discord"),
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
            Component.literal("[Skyblocker] BetterMap ready"),
            new GameProfile(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"), "Skyblocker"),
            null,
            Instant.ofEpochMilli(3_500L),
            32767
        );

        assertEquals(ChatSourceType.SYSTEM_PLAYER, systemEvent.getSourceType());
        assertEquals("Skyblocker", systemEvent.getSenderName());
        assertEquals("[Skyblocker] BetterMap ready", systemEvent.getRawMessage());
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(systemEvent));
    }

    @Test
    void paramBackedChatMessagesStillEnterAsPlayerMessages() {
        ChatEvent playerEvent = ChatPipelineListener.classifyChatMessage(
            Component.literal("add me on discord"),
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
            Component.literal("[Skyblocker] BetterMap ready"),
            null,
            new SenderNameParams(),
            Instant.ofEpochMilli(4_500L),
            32767
        );

        assertEquals(ChatSourceType.SYSTEM_PLAYER, systemEvent.getSourceType());
        assertEquals("Pankraz01", systemEvent.getSenderName());
        assertEquals("[Skyblocker] BetterMap ready", systemEvent.getRawMessage());
        assertEquals(false, ChatPipelineListener.shouldEnterPipeline(systemEvent));
    }
}
