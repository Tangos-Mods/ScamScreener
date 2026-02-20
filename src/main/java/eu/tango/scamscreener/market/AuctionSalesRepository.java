package eu.tango.scamscreener.market;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class AuctionSalesRepository {
	private static final String UPSERT_SQL = """
		INSERT OR REPLACE INTO ah_sale (auction_uuid, item_key, price_coins, end_ms)
		VALUES (?, ?, ?, ?)
		""";
	private static final String SELECT_30D_SQL = """
		SELECT price_coins, end_ms
		FROM ah_sale
		WHERE item_key = ? AND end_ms >= ?
		ORDER BY end_ms DESC
		""";
	private static final String PRUNE_SQL = "DELETE FROM ah_sale WHERE end_ms < ?";

	private final MarketDatabase database;

	public AuctionSalesRepository(MarketDatabase database) {
		this.database = database;
	}

	public void saveEndedAuctions(List<HypixelMarketApiClient.EndedAuction> auctions) throws SQLException {
		if (auctions == null || auctions.isEmpty()) {
			return;
		}
		try (Connection connection = database.openConnection();
				PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
			connection.setAutoCommit(false);
			for (HypixelMarketApiClient.EndedAuction auction : auctions) {
				if (auction == null || auction.auctionUuid().isBlank() || auction.itemKey().isBlank() || auction.priceCoins() <= 0L || auction.endMs() <= 0L) {
					continue;
				}
				statement.setString(1, auction.auctionUuid());
				statement.setString(2, MarketItemKey.normalize(auction.itemKey()));
				statement.setLong(3, auction.priceCoins());
				statement.setLong(4, auction.endMs());
				statement.addBatch();
			}
			statement.executeBatch();
			connection.commit();
		}
	}

	public Optional<SaleStats> stats30d(String itemKey, long nowMs) throws SQLException {
		String normalizedKey = MarketItemKey.normalize(itemKey);
		if (normalizedKey.isBlank()) {
			return Optional.empty();
		}
		long cutoff = nowMs - 30L * 24L * 60L * 60L * 1000L;
		List<Long> prices = new ArrayList<>();
		long newest = 0L;

		try (Connection connection = database.openConnection();
				PreparedStatement statement = connection.prepareStatement(SELECT_30D_SQL)) {
			statement.setString(1, normalizedKey);
			statement.setLong(2, cutoff);
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					long price = resultSet.getLong("price_coins");
					if (price <= 0L) {
						continue;
					}
					prices.add(price);
					newest = Math.max(newest, resultSet.getLong("end_ms"));
				}
			}
		}

		if (prices.isEmpty()) {
			return Optional.empty();
		}
		prices.sort(Comparator.naturalOrder());
		double average = prices.stream().mapToLong(Long::longValue).average().orElse(0.0);
		double median = median(prices);
		return Optional.of(new SaleStats(normalizedKey, average, median, prices.size(), newest));
	}

	public void pruneOlderThan(long cutoffMs) throws SQLException {
		try (Connection connection = database.openConnection();
				PreparedStatement statement = connection.prepareStatement(PRUNE_SQL)) {
			statement.setLong(1, Math.max(0L, cutoffMs));
			statement.executeUpdate();
		}
	}

	private static double median(List<Long> sortedValues) {
		if (sortedValues == null || sortedValues.isEmpty()) {
			return 0.0;
		}
		int size = sortedValues.size();
		int middle = size / 2;
		if ((size % 2) == 1) {
			return sortedValues.get(middle);
		}
		return (sortedValues.get(middle - 1) + sortedValues.get(middle)) / 2.0;
	}

	public record SaleStats(
		String itemKey,
		double averageCoins,
		double medianCoins,
		int sampleCount,
		long newestEndMs
	) {
		public SaleStats {
			itemKey = itemKey == null ? "" : itemKey.trim();
			averageCoins = Math.max(0.0, averageCoins);
			medianCoins = Math.max(0.0, medianCoins);
			sampleCount = Math.max(0, sampleCount);
			newestEndMs = Math.max(0L, newestEndMs);
		}
	}
}

