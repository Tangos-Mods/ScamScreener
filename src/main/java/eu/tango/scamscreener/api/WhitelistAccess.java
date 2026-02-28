package eu.tango.scamscreener.api;

import eu.tango.scamscreener.whitelist.WhitelistManager;

import java.util.Collection;
import java.util.UUID;

public interface WhitelistAccess {
	WhitelistManager.AddOrUpdateResult addOrUpdate(UUID uuid, String displayName);

	boolean remove(UUID uuid);

	WhitelistManager.WhitelistEntry removeByName(String name);

	boolean contains(UUID uuid);

	WhitelistManager.WhitelistEntry get(UUID uuid);

	WhitelistManager.WhitelistEntry findByName(String name);

	Collection<WhitelistManager.WhitelistEntry> allEntries();

	boolean isEmpty();

	boolean updateDisplayName(UUID uuid, String canonicalName);
}
