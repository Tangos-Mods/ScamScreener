package eu.tango.scamscreener.market;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketItemKeyTest {
	@Test
	void normalizeRemovesFormattingAndSpecialChars() {
		String normalized = MarketItemKey.normalize("\u00A76Hyperion!!!");
		assertEquals("hyperion", normalized);
	}

	@Test
	void fromDisplayNameAndLoreAppendsRelevantLoreMarkers() {
		String key = MarketItemKey.fromDisplayNameAndLore(
			"Necron's Chestplate",
			List.of("Recombobulated", "Some other line", "Dungeon Item")
		);

		assertTrue(key.startsWith("necron_s_chestplate"));
		assertTrue(key.contains("recombobulated"));
		assertTrue(key.contains("dungeon_item"));
	}
}

