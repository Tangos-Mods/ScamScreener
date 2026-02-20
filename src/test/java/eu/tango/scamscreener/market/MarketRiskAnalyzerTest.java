package eu.tango.scamscreener.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketRiskAnalyzerTest {
	@Test
	void blocksHighConfidenceOverbid(@TempDir Path tempDir) throws Exception {
		Context ctx = new Context(tempDir);
		long now = System.currentTimeMillis();
		List<HypixelMarketApiClient.AuctionEntry> bins = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			bins.add(new HypixelMarketApiClient.AuctionEntry("u" + i, "aspect_of_the_dragons", 1_000_000L + i * 20_000L, now, "AOTD", ""));
		}
		ctx.binRepository.rebuildFromAuctions(bins, now);

		MarketRiskAnalyzer.RiskDecision decision = ctx.analyzer.evaluate(new MarketRiskAnalyzer.EvaluationInput(
			MarketRiskAnalyzer.Action.AH_BUY,
			"aspect_of_the_dragons",
			5_000_000L,
			false,
			now
		));

		assertTrue(decision.block());
		assertTrue(decision.warn());
	}

	@Test
	void downgradesToWarnWhenConfidenceLow(@TempDir Path tempDir) throws Exception {
		Context ctx = new Context(tempDir);
		long now = System.currentTimeMillis();
		ctx.binRepository.rebuildFromAuctions(List.of(
			new HypixelMarketApiClient.AuctionEntry("one", "midas_staff", 10_000_000L, now, "Midas Staff", "")
		), now);

		MarketRiskAnalyzer.RiskDecision decision = ctx.analyzer.evaluate(new MarketRiskAnalyzer.EvaluationInput(
			MarketRiskAnalyzer.Action.AH_BUY,
			"midas_staff",
			60_000_000L,
			false,
			now
		));

		assertFalse(decision.block());
		assertTrue(decision.warn());
		assertTrue(decision.lowConfidence());
	}

	@Test
	void blocksRareTradeRegardlessOfPrice(@TempDir Path tempDir) {
		Context ctx = new Context(tempDir);
		long now = System.currentTimeMillis();

		MarketRiskAnalyzer.RiskDecision decision = ctx.analyzer.evaluate(new MarketRiskAnalyzer.EvaluationInput(
			MarketRiskAnalyzer.Action.TRADE,
			"crystal_helmet",
			0L,
			true,
			now
		));

		assertTrue(decision.block());
		assertTrue(decision.warn());
	}

	@Test
	void marksNpcAndInflationHighlights(@TempDir Path tempDir) throws Exception {
		Context ctx = new Context(tempDir);
		long now = System.currentTimeMillis();
		ctx.salesRepository.saveEndedAuctions(List.of(
			new HypixelMarketApiClient.EndedAuction("s1", "rookie_hoe", 10L, now - 1_000L, "Rookie Hoe"),
			new HypixelMarketApiClient.EndedAuction("s2", "rookie_hoe", 20L, now - 2_000L, "Rookie Hoe"),
			new HypixelMarketApiClient.EndedAuction("s3", "rookie_hoe", 30L, now - 3_000L, "Rookie Hoe")
		));
		try (Connection connection = ctx.database.openConnection();
				PreparedStatement statement = connection.prepareStatement(
					"INSERT OR REPLACE INTO npc_price(item_id, npc_price_coins, source, updated_ms) VALUES (?, ?, ?, ?)"
				)) {
			statement.setString(1, "rookie_hoe");
			statement.setLong(2, 1L);
			statement.setString(3, "test");
			statement.setLong(4, now);
			statement.executeUpdate();
		}

		MarketRiskAnalyzer.RiskDecision decision = ctx.analyzer.evaluate(new MarketRiskAnalyzer.EvaluationInput(
			MarketRiskAnalyzer.Action.AH_BUY,
			"rookie_hoe",
			1_000L,
			false,
			now
		));

		assertTrue(decision.hasHighlight());
		assertTrue(decision.npcWarn());
	}

	@Test
	void flagsAhListingUnderbid(@TempDir Path tempDir) throws Exception {
		Context ctx = new Context(tempDir);
		long now = System.currentTimeMillis();
		ctx.binRepository.rebuildFromAuctions(List.of(
			new HypixelMarketApiClient.AuctionEntry("one", "necron_chestplate", 100_000_000L, now, "Necron Chestplate", "")
		), now);

		MarketRiskAnalyzer.RiskDecision decision = ctx.analyzer.evaluate(new MarketRiskAnalyzer.EvaluationInput(
			MarketRiskAnalyzer.Action.AH_LIST,
			"necron_chestplate",
			30_000_000L,
			false,
			now
		));

		assertTrue(decision.warn());
		assertEquals(MarketRiskAnalyzer.Reason.UNDERBID, decision.reason());
	}

	@Test
	void flagsBazaarListingOverbid(@TempDir Path tempDir) throws Exception {
		Context ctx = new Context(tempDir);
		long now = System.currentTimeMillis();
		ctx.bazaarRepository.saveSnapshots(List.of(
			new BazaarRepository.BazaarSnapshot("enchanted_diamond_block", now - 1_000L, 100_000.0, 95_000.0, 1_000.0, 1_000.0),
			new BazaarRepository.BazaarSnapshot("enchanted_diamond_block", now - 2_000L, 102_000.0, 96_000.0, 1_000.0, 1_000.0),
			new BazaarRepository.BazaarSnapshot("enchanted_diamond_block", now - 3_000L, 101_000.0, 97_000.0, 1_000.0, 1_000.0)
		));

		MarketRiskAnalyzer.RiskDecision decision = ctx.analyzer.evaluate(new MarketRiskAnalyzer.EvaluationInput(
			MarketRiskAnalyzer.Action.BZ_LIST,
			"enchanted_diamond_block",
			350_000L,
			false,
			now
		));

		assertTrue(decision.warn());
		assertEquals(MarketRiskAnalyzer.Reason.OVERBID, decision.reason());
	}

	private static final class Context {
		private final MarketDatabase database;
		private final AuctionBinIndexRepository binRepository;
		private final AuctionSalesRepository salesRepository;
		private final BazaarRepository bazaarRepository;
		private final NpcPriceCatalog npcPriceCatalog;
		private final MarketRiskAnalyzer analyzer;

		private Context(Path tempDir) {
			this.database = new MarketDatabase(tempDir.resolve("market.db"));
			this.binRepository = new AuctionBinIndexRepository(database);
			this.salesRepository = new AuctionSalesRepository(database);
			this.bazaarRepository = new BazaarRepository(database);
			this.npcPriceCatalog = new NpcPriceCatalog(database);
			this.analyzer = new MarketRiskAnalyzer(
				binRepository,
				salesRepository,
				bazaarRepository,
				npcPriceCatalog,
				() -> new MarketRiskAnalyzer.Settings(2.5, 4.0, 3.0, 6.0, 25.0, 100.0, 0.65, 0.45, true)
			);
		}
	}
}
