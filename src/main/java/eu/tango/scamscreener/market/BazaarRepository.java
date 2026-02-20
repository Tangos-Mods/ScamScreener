package eu.tango.scamscreener.market;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class BazaarRepository {
	private static final String INSERT_SQL = """
		INSERT OR REPLACE INTO bz_snapshot (product_id, ts_ms, buy_price, sell_price, buy_volume, sell_volume)
		VALUES (?, ?, ?, ?, ?, ?)
		""";
	private static final String SELECT_30D_SQL = """
		SELECT buy_price, sell_price, ts_ms
		FROM bz_snapshot
		WHERE product_id = ? AND ts_ms >= ?
		ORDER BY ts_ms DESC
		""";
	private static final String PRUNE_SQL = "DELETE FROM bz_snapshot WHERE ts_ms < ?";

	private final MarketDatabase database;

	public BazaarRepository(MarketDatabase database) {
		this.database = database;
	}

	public void saveSnapshots(List<BazaarSnapshot> snapshots) throws SQLException {
		if (snapshots == null || snapshots.isEmpty()) {
			return;
		}
		try (Connection connection = database.openConnection();
				PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
			connection.setAutoCommit(false);
			for (BazaarSnapshot snapshot : snapshots) {
				if (snapshot == null || snapshot.productId().isBlank() || snapshot.tsMs() <= 0L) {
					continue;
				}
				statement.setString(1, snapshot.productId());
				statement.setLong(2, snapshot.tsMs());
				statement.setDouble(3, Math.max(0.0, snapshot.buyPrice()));
				statement.setDouble(4, Math.max(0.0, snapshot.sellPrice()));
				statement.setDouble(5, Math.max(0.0, snapshot.buyVolume()));
				statement.setDouble(6, Math.max(0.0, snapshot.sellVolume()));
				statement.addBatch();
			}
			statement.executeBatch();
			connection.commit();
		}
	}

	public Optional<PriceStats> stats30d(String productId, long nowMs) throws SQLException {
		String normalizedKey = MarketItemKey.normalize(productId);
		if (normalizedKey.isBlank()) {
			return Optional.empty();
		}
		long cutoff = nowMs - 30L * 24L * 60L * 60L * 1000L;
		List<Double> prices = new ArrayList<>();
		long newest = 0L;

		try (Connection connection = database.openConnection();
				PreparedStatement statement = connection.prepareStatement(SELECT_30D_SQL)) {
			statement.setString(1, normalizedKey);
			statement.setLong(2, cutoff);
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					double buy = resultSet.getDouble("buy_price");
					double sell = resultSet.getDouble("sell_price");
					double midpoint = midpoint(buy, sell);
					if (midpoint <= 0.0) {
						continue;
					}
					prices.add(midpoint);
					newest = Math.max(newest, resultSet.getLong("ts_ms"));
				}
			}
		}

		if (prices.isEmpty()) {
			return Optional.empty();
		}
		prices.sort(Comparator.naturalOrder());
		double average = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
		double median = median(prices);
		return Optional.of(new PriceStats(normalizedKey, average, median, prices.size(), newest));
	}

	public void pruneOlderThan(long cutoffMs) throws SQLException {
		try (Connection connection = database.openConnection();
				PreparedStatement statement = connection.prepareStatement(PRUNE_SQL)) {
			statement.setLong(1, Math.max(0L, cutoffMs));
			statement.executeUpdate();
		}
	}

	private static double midpoint(double buy, double sell) {
		double safeBuy = Math.max(0.0, buy);
		double safeSell = Math.max(0.0, sell);
		if (safeBuy > 0.0 && safeSell > 0.0) {
			return (safeBuy + safeSell) / 2.0;
		}
		return Math.max(safeBuy, safeSell);
	}

	private static double median(List<Double> sortedValues) {
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

	public record BazaarSnapshot(
		String productId,
		long tsMs,
		double buyPrice,
		double sellPrice,
		double buyVolume,
		double sellVolume
	) {
		public BazaarSnapshot {
			productId = MarketItemKey.normalize(productId);
			tsMs = Math.max(0L, tsMs);
			buyPrice = Math.max(0.0, buyPrice);
			sellPrice = Math.max(0.0, sellPrice);
			buyVolume = Math.max(0.0, buyVolume);
			sellVolume = Math.max(0.0, sellVolume);
		}
	}

	public record PriceStats(
		String productId,
		double averageCoins,
		double medianCoins,
		int sampleCount,
		long updatedMs
	) {
		public PriceStats {
			productId = productId == null ? "" : productId.trim();
			averageCoins = Math.max(0.0, averageCoins);
			medianCoins = Math.max(0.0, medianCoins);
			sampleCount = Math.max(0, sampleCount);
			updatedMs = Math.max(0L, updatedMs);
		}
	}
}

