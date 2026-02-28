package eu.tango.scamscreener.api;

import java.util.Optional;

public interface ScamScreenerApi {
	String ENTRYPOINT_KEY = "scamscreener-api";

	WhitelistAccess whitelist();

	BlacklistAccess blacklist();

	default <T> Optional<T> extension(Class<T> type) {
		if (type == null) {
			return Optional.empty();
		}
		if (type.isInstance(this)) {
			return Optional.of(type.cast(this));
		}
		WhitelistAccess whitelist = whitelist();
		if (type.isInstance(whitelist)) {
			return Optional.of(type.cast(whitelist));
		}
		BlacklistAccess blacklist = blacklist();
		if (type.isInstance(blacklist)) {
			return Optional.of(type.cast(blacklist));
		}
		return Optional.empty();
	}
}
