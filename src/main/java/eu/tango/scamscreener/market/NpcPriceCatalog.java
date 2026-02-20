package eu.tango.scamscreener.market;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

public final class NpcPriceCatalog {
	private static final String RESOURCE_PATH = "/assets/scam-screener/data/market-npc-prices.json";
	private static final String UPSERT_SQL = """
		INSERT OR REPLACE INTO npc_price (item_id, npc_price_coins, source, updated_ms)
		VALUES (?, ?, ?, ?)
		""";
	private static final String SELECT_SQL = "SELECT npc_price_coins FROM npc_price WHERE item_id = ?";

	private final MarketDatabase database;
	private final String resourcePath;
	private volatile boolean seeded;

	public NpcPriceCatalog(MarketDatabase database) {
		this(database, RESOURCE_PATH);
	}

	NpcPriceCatalog(MarketDatabase database, String resourcePath) {
		this.database = database;
		this.resourcePath = resourcePath == null || resourcePath.isBlank() ? RESOURCE_PATH : resourcePath;
	}

	public void seedIfNeeded(long nowMs) {
		if (seeded) {
			return;
		}
		synchronized (this) {
			if (seeded) {
				return;
			}
			List<NpcPriceEntry> entries = loadEntries(resourcePath);
			if (!entries.isEmpty()) {
				try {
					saveEntries(entries, nowMs);
				} catch (SQLException ignored) {
				}
			}
			seeded = true;
		}
	}

	public OptionalLong findPriceCoins(String itemId) {
		String key = MarketItemKey.normalize(itemId);
		if (key.isBlank()) {
			return OptionalLong.empty();
		}
		try (Connection connection = database.openConnection();
				PreparedStatement statement = connection.prepareStatement(SELECT_SQL)) {
			statement.setString(1, key);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (!resultSet.next()) {
					return OptionalLong.empty();
				}
				long value = resultSet.getLong("npc_price_coins");
				return value > 0L ? OptionalLong.of(value) : OptionalLong.empty();
			}
		} catch (SQLException ignored) {
			return OptionalLong.empty();
		}
	}

	private void saveEntries(List<NpcPriceEntry> entries, long nowMs) throws SQLException {
		if (entries == null || entries.isEmpty()) {
			return;
		}
		try (Connection connection = database.openConnection();
				PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
			connection.setAutoCommit(false);
			for (NpcPriceEntry entry : entries) {
				if (entry == null || entry.itemId().isBlank() || entry.npcPriceCoins() <= 0L) {
					continue;
				}
				statement.setString(1, MarketItemKey.normalize(entry.itemId()));
				statement.setLong(2, entry.npcPriceCoins());
				statement.setString(3, entry.source().isBlank() ? "manual-v1" : entry.source());
				statement.setLong(4, Math.max(0L, nowMs));
				statement.addBatch();
			}
			statement.executeBatch();
			connection.commit();
		}
	}

	private static List<NpcPriceEntry> loadEntries(String resourcePath) {
		InputStream stream = NpcPriceCatalog.class.getResourceAsStream(resourcePath);
		if (stream == null) {
			return List.of();
		}
		try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (!parsed.isJsonObject()) {
				return List.of();
			}
			JsonObject root = parsed.getAsJsonObject();
			JsonArray prices = array(root, "prices");
			if (prices == null || prices.isEmpty()) {
				return List.of();
			}
			List<NpcPriceEntry> out = new ArrayList<>();
			for (JsonElement raw : prices) {
				JsonObject row = object(raw);
				if (row == null) {
					continue;
				}
				String itemId = string(row, "item_id");
				long npcPrice = longValue(row, "npc_price_coins", 0L);
				String source = string(row, "source");
				if (itemId.isBlank() || npcPrice <= 0L) {
					continue;
				}
				out.add(new NpcPriceEntry(itemId, npcPrice, source));
			}
			return out;
		} catch (Exception ignored) {
			return List.of();
		}
	}

	private static JsonObject object(JsonElement element) {
		if (element == null || !element.isJsonObject()) {
			return null;
		}
		return element.getAsJsonObject();
	}

	private static JsonArray array(JsonObject object, String key) {
		if (object == null || key == null || key.isBlank()) {
			return null;
		}
		JsonElement element = object.get(key);
		return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
	}

	private static String string(JsonObject object, String key) {
		if (object == null || key == null || key.isBlank()) {
			return "";
		}
		JsonElement element = object.get(key);
		return (element == null || !element.isJsonPrimitive()) ? "" : element.getAsString().trim();
	}

	private static long longValue(JsonObject object, String key, long fallback) {
		if (object == null || key == null || key.isBlank()) {
			return fallback;
		}
		try {
			JsonElement element = object.get(key);
			return element == null ? fallback : element.getAsLong();
		} catch (Exception ignored) {
			return fallback;
		}
	}

	record NpcPriceEntry(String itemId, long npcPriceCoins, String source) {
		NpcPriceEntry {
			itemId = itemId == null ? "" : itemId.trim();
			npcPriceCoins = Math.max(0L, npcPriceCoins);
			source = source == null ? "" : source.trim();
		}
	}
}

