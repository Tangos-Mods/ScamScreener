package eu.tango.scamscreener.market;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketDatabaseSchemaTest {
	@Test
	void createsExpectedTables(@TempDir Path tempDir) throws Exception {
		MarketDatabase database = new MarketDatabase(tempDir.resolve("market.db"));
		Set<String> tables = new HashSet<>();

		try (Connection connection = database.openConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT name FROM sqlite_master WHERE type='table'");
				ResultSet resultSet = statement.executeQuery()) {
			while (resultSet.next()) {
				tables.add(resultSet.getString("name"));
			}
		}

		assertTrue(tables.contains("bz_snapshot"));
		assertTrue(tables.contains("ah_bin"));
		assertTrue(tables.contains("ah_sale"));
		assertTrue(tables.contains("npc_price"));
		assertTrue(tables.contains("market_meta"));
	}
}

