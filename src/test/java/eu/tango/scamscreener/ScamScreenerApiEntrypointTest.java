package eu.tango.scamscreener;

import eu.tango.scamscreener.api.BlacklistAccess;
import eu.tango.scamscreener.api.ScamScreenerApi;
import eu.tango.scamscreener.api.WhitelistAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScamScreenerApiEntrypointTest {
	@Test
	void exposesSharedApiViaCustomEntrypoint() {
		ScamScreenerApi api = new ScamScreenerApiEntrypoint();
		WhitelistAccess whitelist = api.whitelist();
		BlacklistAccess blacklist = api.blacklist();

		assertSame(api, api.extension(ScamScreenerApi.class).orElseThrow());
		assertSame(whitelist, api.extension(WhitelistAccess.class).orElseThrow());
		assertSame(blacklist, api.extension(BlacklistAccess.class).orElseThrow());
		assertTrue(api.extension(Runnable.class).isEmpty());
	}
}
