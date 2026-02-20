package eu.tango.scamscreener.market;

import eu.tango.scamscreener.config.ScamScreenerPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public final class MarketDatabase {
	private static final String CREATE_BZ_TABLE = """
		CREATE TABLE IF NOT EXISTS bz_snapshot (
			product_id TEXT NOT NULL,
			ts_ms INTEGER NOT NULL,
			buy_price REAL NOT NULL,
			sell_price REAL NOT NULL,
			buy_volume REAL NOT NULL,
			sell_volume REAL NOT NULL,
			PRIMARY KEY (product_id, ts_ms)
		)
		""";
	private static final String CREATE_AH_BIN_TABLE = """
		CREATE TABLE IF NOT EXISTS ah_bin (
			item_key TEXT PRIMARY KEY,
			price_coins INTEGER NOT NULL,
			source_auction_uuid TEXT,
			updated_ms INTEGER NOT NULL,
			sample_count INTEGER NOT NULL
		)
		""";
	private static final String CREATE_AH_SALE_TABLE = """
		CREATE TABLE IF NOT EXISTS ah_sale (
			auction_uuid TEXT PRIMARY KEY,
			item_key TEXT NOT NULL,
			price_coins INTEGER NOT NULL,
			end_ms INTEGER NOT NULL
		)
		""";
	private static final String CREATE_NPC_PRICE_TABLE = """
		CREATE TABLE IF NOT EXISTS npc_price (
			item_id TEXT PRIMARY KEY,
			npc_price_coins INTEGER NOT NULL,
			source TEXT NOT NULL,
			updated_ms INTEGER NOT NULL
		)
		""";
	private static final String CREATE_META_TABLE = """
		CREATE TABLE IF NOT EXISTS market_meta (
			key TEXT PRIMARY KEY,
			value TEXT NOT NULL
		)
		""";
	private static final String CREATE_AH_SALE_ITEM_KEY_INDEX = """
		CREATE INDEX IF NOT EXISTS idx_ah_sale_item_key_end_ms
		ON ah_sale(item_key, end_ms)
		""";
	private static final String CREATE_BZ_PRODUCT_INDEX = """
		CREATE INDEX IF NOT EXISTS idx_bz_snapshot_product_ts
		ON bz_snapshot(product_id, ts_ms)
		""";

	private final Path dbPath;
	private volatile boolean initialized;

	public MarketDatabase() {
		this(ScamScreenerPaths.inModConfigDir("scam-screener-market.db"));
	}

	public MarketDatabase(Path dbPath) {
		this.dbPath = Objects.requireNonNull(dbPath, "dbPath");
	}

	public Path path() {
		return dbPath;
	}

	public Connection openConnection() throws SQLException {
		initIfNeeded();
		return DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
	}

	private void initIfNeeded() throws SQLException {
		if (initialized) {
			return;
		}
		synchronized (this) {
			if (initialized) {
				return;
			}
			try {
				Class.forName("org.sqlite.JDBC");
				Path parent = dbPath.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}
			} catch (Exception e) {
				throw new SQLException("Failed to initialize market database path.", e);
			}
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
					Statement statement = connection.createStatement()) {
				statement.execute("PRAGMA journal_mode=WAL");
				statement.execute("PRAGMA synchronous=NORMAL");
				statement.execute(CREATE_BZ_TABLE);
				statement.execute(CREATE_AH_BIN_TABLE);
				statement.execute(CREATE_AH_SALE_TABLE);
				statement.execute(CREATE_NPC_PRICE_TABLE);
				statement.execute(CREATE_META_TABLE);
				statement.execute(CREATE_AH_SALE_ITEM_KEY_INDEX);
				statement.execute(CREATE_BZ_PRODUCT_INDEX);
			}
			initialized = true;
		}
	}
}
