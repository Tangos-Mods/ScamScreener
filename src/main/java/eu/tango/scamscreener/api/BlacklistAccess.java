package eu.tango.scamscreener.api;

import eu.tango.scamscreener.blacklist.BlacklistManager;

import java.util.Collection;
import java.util.UUID;

public interface BlacklistAccess {
	boolean add(UUID uuid);

	boolean add(UUID uuid, String name, int score, String reason);

	boolean update(UUID uuid, String name, int score, String reason);

	boolean remove(UUID uuid);

	boolean contains(UUID uuid);

	BlacklistManager.ScamEntry get(UUID uuid);

	BlacklistManager.ScamEntry findByName(String name);

	boolean isEmpty();

	Collection<BlacklistManager.ScamEntry> allEntries();
}
