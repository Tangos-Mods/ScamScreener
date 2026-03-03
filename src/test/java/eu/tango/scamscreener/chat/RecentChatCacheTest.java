package eu.tango.scamscreener.chat;

import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecentChatCacheTest {
    @Test
    void keepsNewestMessagesFirstWithinCapacity() {
        RecentChatCache cache = new RecentChatCache(3);

        cache.record(new ChatEvent("first", null, "Alice", 1L, ChatSourceType.PLAYER));
        cache.record(new ChatEvent("second", null, "Bob", 2L, ChatSourceType.PLAYER));
        cache.record(new ChatEvent("third", null, "", 3L, ChatSourceType.SYSTEM));
        cache.record(new ChatEvent("fourth", null, "Cara", 4L, ChatSourceType.PLAYER));

        List<RecentChatCache.CachedChatMessage> entries = cache.entries();
        assertEquals(3, entries.size());
        assertEquals("fourth", entries.get(0).cleanText());
        assertEquals("third", entries.get(1).cleanText());
        assertEquals("second", entries.get(2).cleanText());
        assertEquals("System", entries.get(1).displaySender());
    }
}
