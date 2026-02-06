package eu.tango.scamscreener.lookup;

import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.ui.Messages;
import eu.tango.scamscreener.util.UuidUtil;

import java.util.UUID;

public final class TargetResolutionService {
	private final PlayerLookup playerLookup;
	private final MojangProfileService mojangProfileService;
	private final BlacklistManager blacklistManager;

	public TargetResolutionService(PlayerLookup playerLookup, MojangProfileService mojangProfileService, BlacklistManager blacklistManager) {
		this.playerLookup = playerLookup;
		this.mojangProfileService = mojangProfileService;
		this.blacklistManager = blacklistManager;
	}

	public ResolvedTarget resolveTargetOrReply(String input) {
		UUID parsedUuid = UuidUtil.parse(input);
		if (parsedUuid != null) {
			return new ResolvedTarget(parsedUuid, playerLookup.findNameByUuid(parsedUuid));
		}

		UUID byName = playerLookup.findUuidByName(input);
		if (byName != null) {
			return new ResolvedTarget(byName, playerLookup.findNameByUuid(byName));
		}

		BlacklistManager.ScamEntry knownEntry = blacklistManager.findByName(input);
		if (knownEntry != null) {
			return new ResolvedTarget(knownEntry.uuid(), knownEntry.name());
		}

		ResolvedTarget mojangResolved = mojangProfileService.lookupCached(input);
		if (mojangResolved != null) {
			return mojangResolved;
		}

		mojangProfileService.lookupAsync(input).thenAccept(resolved -> {
			if (resolved == null) {
				MessageDispatcher.reply(Messages.unresolvedTarget(input));
				return;
			}
			MessageDispatcher.reply(Messages.mojangLookupCompleted(input, resolved.name()));
		});
		MessageDispatcher.reply(Messages.mojangLookupStarted(input));
		return null;
	}
}
