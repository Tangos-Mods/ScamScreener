package eu.tango.scamscreener.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcPriceCatalogTest {
	@Test
	void seedsAndLoadsNpcPriceFromResource(@TempDir Path tempDir) {
		MarketDatabase database = new MarketDatabase(tempDir.resolve("market.db"));
		NpcPriceCatalog catalog = new NpcPriceCatalog(database);

		catalog.seedIfNeeded(System.currentTimeMillis());
		OptionalLong price = catalog.findPriceCoins("rookie_hoe");

		assertTrue(price.isPresent());
		assertTrue(price.getAsLong() > 0L);
	}
}

