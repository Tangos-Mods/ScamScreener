package eu.tango.scamscreener.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionSalesRepositoryTest {
	@Test
	void computesMedianFrom30dSales(@TempDir Path tempDir) throws Exception {
		MarketDatabase database = new MarketDatabase(tempDir.resolve("market.db"));
		AuctionSalesRepository repository = new AuctionSalesRepository(database);
		long now = System.currentTimeMillis();

		repository.saveEndedAuctions(List.of(
			new HypixelMarketApiClient.EndedAuction("a1", "aspect_of_the_dragons", 900_000L, now - 1_000L, "AOTD"),
			new HypixelMarketApiClient.EndedAuction("a2", "aspect_of_the_dragons", 1_000_000L, now - 2_000L, "AOTD"),
			new HypixelMarketApiClient.EndedAuction("a3", "aspect_of_the_dragons", 1_100_000L, now - 3_000L, "AOTD")
		));

		Optional<AuctionSalesRepository.SaleStats> stats = repository.stats30d("aspect_of_the_dragons", now);
		assertTrue(stats.isPresent());
		assertEquals(3, stats.get().sampleCount());
		assertEquals(1_000_000.0, stats.get().medianCoins(), 0.01);
	}
}

