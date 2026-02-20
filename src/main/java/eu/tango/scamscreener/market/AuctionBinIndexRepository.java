package eu.tango.scamscreener.market;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AuctionBinIndexRepository {
	private static final String DELETE_ALL_SQL = "DELETE FROM ah_bin";
	private static final String INSERT_SQL = """
		INSERT INTO ah_bin (item_key, price_coins, source_auction_uuid, updated_ms, sample_count)
		VALUES (?, ?, ?, ?, ?)
		""";
	private static final String SELECT_SQL = """
		SELECT item_key, price_coins, source_auction_uuid, updated_ms, sample_count
		FROM ah_bin
		WHERE item_key = ?
		""";

	private final MarketDatabase database;

	public AuctionBinIndexRepository(MarketDatabase database) {
		this.database = database;
	}

	public void rebuildFromAuctions(List<HypixelMarketApiClient.AuctionEntry> auctions, long nowMs) throws SQLException {
		Map<String, BinAccumulator> index = new LinkedHashMap<>();
		if (auctions != null) {
			for (HypixelMarketApiClient.AuctionEntry auction : auctions) {
				if (auction == null || auction.itemKey().isBlank() || auction.startingBidCoins() <= 0L) {
					continue;
				}
				String key = MarketItemKey.normalize(auction.itemKey());
				BinAccumulator current = index.get(key);
				if (current == null) {
					index.put(key, new BinAccumulator(auction.startingBidCoins(), auction.auctionUuid(), 1));
					continue;
				}
				long lowest = Math.min(current.lowestCoins(), auction.startingBidCoins());
				String sourceUuid = current.sourceAuctionUuid();
				if (auction.startingBidCoins() < current.lowestCoins()) {
					sourceUuid = auction.auctionUuid();
				}
				index.put(key, new BinAccumulator(lowest, sourceUuid, current.sampleCount() + 1));
			}
		}

		try (Connection connection = database.openConnection();
				PreparedStatement delete = connection.prepareStatement(DELETE_ALL_SQL);
				PreparedStatement insert = connection.prepareStatement(INSERT_SQL)) {
			connection.setAutoCommit(false);
			delete.executeUpdate();
			for (Map.Entry<String, BinAccumulator> entry : index.entrySet()) {
				String key = entry.getKey();
				BinAccumulator value = entry.getValue();
				if (key == null || key.isBlank() || value == null || value.lowestCoins() <= 0L) {
					continue;
				}
				insert.setString(1, key);
				insert.setLong(2, value.lowestCoins());
				insert.setString(3, value.sourceAuctionUuid());
				insert.setLong(4, Math.max(0L, nowMs));
				insert.setInt(5, Math.max(1, value.sampleCount()));
				insert.addBatch();
			}
			insert.executeBatch();
			connection.commit();
		}
	}

	public Optional<BinQuote> findByItemKey(String itemKey) throws SQLException {
		String normalized = MarketItemKey.normalize(itemKey);
		if (normalized.isBlank()) {
			return Optional.empty();
		}
		try (Connection connection = database.openConnection();
				PreparedStatement statement = connection.prepareStatement(SELECT_SQL)) {
			statement.setString(1, normalized);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return Optional.empty();
				}
				return Optional.of(new BinQuote(
					resultSet.getString("item_key"),
					resultSet.getLong("price_coins"),
					resultSet.getString("source_auction_uuid"),
					resultSet.getLong("updated_ms"),
					resultSet.getInt("sample_count")
				));
			}
		}
	}

	private record BinAccumulator(long lowestCoins, String sourceAuctionUuid, int sampleCount) {
	}

	public record BinQuote(
		String itemKey,
		long priceCoins,
		String sourceAuctionUuid,
		long updatedMs,
		int sampleCount
	) {
		public BinQuote {
			itemKey = itemKey == null ? "" : itemKey.trim();
			priceCoins = Math.max(0L, priceCoins);
			sourceAuctionUuid = sourceAuctionUuid == null ? "" : sourceAuctionUuid.trim();
			updatedMs = Math.max(0L, updatedMs);
			sampleCount = Math.max(0, sampleCount);
		}
	}
}

