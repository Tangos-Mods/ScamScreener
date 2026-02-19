package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.pipeline.model.MessageContext;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.whitelist.WhitelistManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WhitelistStageTest {
	@TempDir
	Path tempDir;

	@Test
	void filterReturnsEmptyForWhitelistedPlayer() throws Exception {
		UUID uuid = UUID.randomUUID();
		WhitelistManager whitelistManager = manager();
		whitelistManager.addOrUpdate(uuid, "Trader123");
		WhitelistStage stage = new WhitelistStage(whitelistManager, name -> "Trader123".equalsIgnoreCase(name) ? uuid : null);

		Optional<MessageEvent> filtered = stage.filter(sampleEvent("Trader123"));

		assertTrue(filtered.isEmpty());
	}

	@Test
	void filterPassesNonWhitelistedPlayer() throws Exception {
		WhitelistManager whitelistManager = manager();
		WhitelistStage stage = new WhitelistStage(whitelistManager, name -> null);

		Optional<MessageEvent> filtered = stage.filter(sampleEvent("LegitPlayer"));

		assertTrue(filtered.isPresent());
		assertEquals("LegitPlayer", filtered.get().playerName());
	}

	@Test
	void filterReturnsEmptyWhenEventIsNull() throws Exception {
		WhitelistManager whitelistManager = manager();
		WhitelistStage stage = new WhitelistStage(whitelistManager, name -> null);

		assertTrue(stage.filter(null).isEmpty());
	}

	private MessageEvent sampleEvent(String playerName) {
		return MessageEvent.from(playerName, "hello", 1_000L, MessageContext.GENERAL, "public");
	}

	private WhitelistManager manager() throws Exception {
		Constructor<WhitelistManager> constructor = WhitelistManager.class.getDeclaredConstructor(Path.class, Path.class);
		constructor.setAccessible(true);
		WhitelistManager manager = constructor.newInstance(
			tempDir.resolve("scam-screener-whitelist.json"),
			tempDir.resolve("legacy-scam-screener-whitelist.json")
		);
		manager.load();
		return manager;
	}
}
