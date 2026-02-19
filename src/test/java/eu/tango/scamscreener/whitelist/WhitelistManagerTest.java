package eu.tango.scamscreener.whitelist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhitelistManagerTest {
	@TempDir
	Path tempDir;

	private WhitelistManager whitelistManager;

	@BeforeEach
	void setUp() {
		whitelistManager = new WhitelistManager(
			tempDir.resolve("scam-screener-whitelist.json"),
			tempDir.resolve("legacy-scam-screener-whitelist.json")
		);
		whitelistManager.load();
	}

	@Test
	void addUpdateAndRemoveFlowWorks() {
		UUID uuid = UUID.randomUUID();

		assertEquals(WhitelistManager.AddOrUpdateResult.ADDED, whitelistManager.addOrUpdate(uuid, "PlayerOne"));
		assertEquals(WhitelistManager.AddOrUpdateResult.UNCHANGED, whitelistManager.addOrUpdate(uuid, "PlayerOne"));
		assertEquals(WhitelistManager.AddOrUpdateResult.UPDATED, whitelistManager.addOrUpdate(uuid, "PlayerRenamed"));
		assertTrue(whitelistManager.contains(uuid));
		assertEquals("PlayerRenamed", whitelistManager.get(uuid).name());

		WhitelistManager.WhitelistEntry removed = whitelistManager.removeByName("PlayerRenamed");
		assertNotNull(removed);
		assertEquals(uuid, removed.uuid());
		assertFalse(whitelistManager.contains(uuid));
		assertNull(whitelistManager.get(uuid));
	}

	@Test
	void removeAndFindByNameAreCaseInsensitive() {
		UUID uuid = UUID.randomUUID();
		whitelistManager.addOrUpdate(uuid, "SkyTrader");

		assertNotNull(whitelistManager.findByName("skytrader"));
		assertNotNull(whitelistManager.removeByName("SKYTRADER"));
		assertNull(whitelistManager.findByName("SkyTrader"));
	}

	@Test
	void isWhitelistedPrefersUuidWhenResolverProvidesOne() {
		UUID uuid = UUID.randomUUID();
		whitelistManager.addOrUpdate(uuid, "DisplayOnly");

		boolean whitelisted = whitelistManager.isWhitelisted("CompletelyDifferentName", ignored -> uuid);
		assertTrue(whitelisted);
	}

	@Test
	void isWhitelistedFallsBackToDisplayNameWhenUuidMissing() {
		UUID uuid = UUID.randomUUID();
		whitelistManager.addOrUpdate(uuid, "TrustedSeller");

		assertTrue(whitelistManager.isWhitelisted("trustedseller", ignored -> null));
		assertFalse(whitelistManager.isWhitelisted("unknown", ignored -> null));
	}
}
