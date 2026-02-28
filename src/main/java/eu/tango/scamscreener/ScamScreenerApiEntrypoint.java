package eu.tango.scamscreener;

import eu.tango.scamscreener.api.BlacklistAccess;
import eu.tango.scamscreener.api.ScamScreenerApi;
import eu.tango.scamscreener.api.WhitelistAccess;

public final class ScamScreenerApiEntrypoint implements ScamScreenerApi {
	private final WhitelistAccess whitelist = ScamScreenerClient.sharedWhitelistAccess();
	private final BlacklistAccess blacklist = ScamScreenerClient.sharedBlacklistAccess();

	@Override
	public WhitelistAccess whitelist() {
		return whitelist;
	}

	@Override
	public BlacklistAccess blacklist() {
		return blacklist;
	}
}
