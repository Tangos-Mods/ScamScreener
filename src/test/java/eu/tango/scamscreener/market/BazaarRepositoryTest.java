package eu.tango.scamscreener.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BazaarRepositoryTest {
	@Test
	void computes30dMedianFromSnapshots(@TempDir Path tempDir) throws Exception {
		MarketDatabase database = new MarketDatabase(tempDir.resolve("market.db"));
		BazaarRepository repository = new BazaarRepository(database);
		long now = System.currentTimeMillis();

		repository.saveSnapshots(List.of(
			new BazaarRepository.BazaarSnapshot("enchanted_diamond", now - 1_000L, 100.0, 90.0, 1_000.0, 1_000.0),
			new BazaarRepository.BazaarSnapshot("enchanted_diamond", now - 2_000L, 110.0, 100.0, 1_000.0, 1_000.0),
			new BazaarRepository.BazaarSnapshot("enchanted_diamond", now - 3_000L, 120.0, 110.0, 1_000.0, 1_000.0)
		));

		Optional<BazaarRepository.PriceStats> stats = repository.stats30d("enchanted_diamond", now);
		assertTrue(stats.isPresent());
		assertEquals(3, stats.get().sampleCount());
		assertEquals(105.0, stats.get().medianCoins(), 0.01);
	}
}

