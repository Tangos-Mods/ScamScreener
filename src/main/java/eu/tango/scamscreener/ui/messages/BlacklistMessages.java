package eu.tango.scamscreener.ui.messages;

import eu.tango.scamscreener.blacklist.BlacklistManager;
import eu.tango.scamscreener.util.TimestampFormatUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.UUID;

public final class BlacklistMessages extends StyledMessages {
	private BlacklistMessages() {
	}

	public static MutableComponent addedToBlacklist(String name, UUID uuid) {
		return prefixed()
			.append(aqua(safe(name)))
			.append(gray(" was added to the blacklist."));
	}

	public static MutableComponent addedToBlacklistWithScore(String name, UUID uuid, int score) {
		return prefixed()
			.append(aqua(safe(name)))
			.append(gray(" was added with score "))
			.append(darkRedBold(String.valueOf(score)))
			.append(gray("."));
	}

	public static MutableComponent addedToBlacklistWithMetadata(String name, UUID uuid) {
		return prefixed()
			.append(aqua(safe(name)))
			.append(gray(" was added with metadata."));
	}

	public static MutableComponent updatedBlacklistEntry(String name, int score, String reason) {
		return prefixed()
			.append(gray("Updated blacklist entry: "))
			.append(aqua(safe(name, "unknown")))
			.append(darkGray(" | "))
			.append(darkRed(String.valueOf(Math.max(0, score))))
			.append(darkGray(" | "))
			.append(yellow(safe(reason, "n/a")));
	}

	public static MutableComponent alreadyBlacklisted(String name, UUID uuid) {
		return prefixed()
			.append(aqua(safe(name)))
			.append(gray(" is already blacklisted."));
	}

	public static MutableComponent removedFromBlacklist(String name, UUID uuid) {
		return prefixed()
			.append(aqua(safe(name)))
			.append(gray(" was removed from the blacklist."));
	}

	public static MutableComponent notOnBlacklist(String name, UUID uuid) {
		return prefixed()
			.append(aqua(safe(name)))
			.append(gray(" is not on the blacklist."));
	}

	public static MutableComponent blacklistEmpty() {
		return prefixedGray("The blacklist is empty.");
	}

	public static MutableComponent blacklistHeader() {
		return prefixedGray("Blacklist entries:");
	}

	public static Component blacklistEntry(BlacklistManager.ScamEntry entry) {
		return aqua(entry.name())
			.append(darkGray(" | "))
			.append(darkRed(String.valueOf(entry.score())))
			.append(darkGray(" | "))
			.append(yellow(entry.reason()))
			.append(darkGray(" | "))
			.append(green(TimestampFormatUtil.formatIsoOrRaw(entry.addedAt())));
	}
}
