package eu.tango.scamscreener.lists;

import eu.tango.scamscreener.api.WhitelistAccess;
import eu.tango.scamscreener.api.event.PlayerListChangeType;
import eu.tango.scamscreener.api.event.WhitelistEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory whitelist for trusted players.
 */
public final class Whitelist implements WhitelistAccess {
    private final Map<UUID, WhitelistEntry> entriesByUuid = new LinkedHashMap<>();
    private final Map<String, WhitelistEntry> entriesByName = new LinkedHashMap<>();
    private final Runnable saveHook;

    /**
     * Creates an in-memory whitelist without persistence hooks.
     */
    public Whitelist() {
        this(() -> {
        });
    }

    /**
     * Creates an in-memory whitelist with a persistence hook.
     *
     * @param saveHook callback triggered after mutating changes
     */
    public Whitelist(Runnable saveHook) {
        this.saveHook = saveHook == null ? () -> {
        } : saveHook;
    }

    /**
     * Adds a player to the whitelist.
     *
     * @param playerUuid the player UUID, if available
     * @param playerName the player name, if available
     * @return {@code true} when a new entry was stored
     */
    public boolean add(UUID playerUuid, String playerName) {
        String normalizedName = normalizeName(playerName);
        if (playerUuid == null && normalizedName.isEmpty()) {
            return false;
        }

        Optional<WhitelistEntry> existing = find(playerUuid, playerName);
        existing.ifPresent(this::removeEntry);

        WhitelistEntry entry = new WhitelistEntry(playerUuid, playerName);
        if (entry.playerUuid() != null) {
            entriesByUuid.put(entry.playerUuid(), entry);
        }
        if (entry.hasPlayerName()) {
            entriesByName.put(normalizedName, entry);
        }

        saveHook.run();
        PlayerListChangeType changeType = existing.isPresent() ? PlayerListChangeType.UPDATED : PlayerListChangeType.ADDED;
        WhitelistEvent.EVENT.invoker().onWhitelistChanged(changeType, entry);
        return true;
    }

    /**
     * Looks up a whitelist entry by UUID.
     *
     * @param playerUuid the player UUID to look up
     * @return the matching whitelist entry, when present
     */
    public Optional<WhitelistEntry> get(UUID playerUuid) {
        if (playerUuid == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(entriesByUuid.get(playerUuid));
    }

    /**
     * Looks up a whitelist entry by player name.
     *
     * @param playerName the player name to look up
     * @return the matching whitelist entry, when present
     */
    public Optional<WhitelistEntry> findByName(String playerName) {
        String normalizedName = normalizeName(playerName);
        if (normalizedName.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(entriesByName.get(normalizedName));
    }

    /**
     * Looks up a whitelist entry by UUID or player name.
     *
     * @param playerUuid the player UUID, if available
     * @param playerName the player name, if available
     * @return the matching whitelist entry, when present
     */
    public Optional<WhitelistEntry> find(UUID playerUuid, String playerName) {
        Optional<WhitelistEntry> byUuid = get(playerUuid);
        if (byUuid.isPresent()) {
            return byUuid;
        }

        return findByName(playerName);
    }

    /**
     * Returns every unique whitelist entry.
     *
     * @return the stored whitelist entries in insertion order
     */
    public List<WhitelistEntry> allEntries() {
        Set<WhitelistEntry> unique = new LinkedHashSet<>();
        unique.addAll(entriesByUuid.values());
        unique.addAll(entriesByName.values());
        return new ArrayList<>(unique);
    }

    public boolean contains(UUID playerUuid, String playerName) {
        return find(playerUuid, playerName).isPresent();
    }

    /**
     * Checks whether the whitelist contains the given UUID.
     *
     * @param playerUuid the player UUID to check
     * @return {@code true} when a matching entry exists
     */
    public boolean contains(UUID playerUuid) {
        return get(playerUuid).isPresent();
    }

    /**
     * Checks whether the whitelist contains the given player name.
     *
     * @param playerName the player name to check
     * @return {@code true} when a matching entry exists
     */
    public boolean containsName(String playerName) {
        return findByName(playerName).isPresent();
    }

    public boolean remove(UUID playerUuid, String playerName) {
        Optional<WhitelistEntry> existing = find(playerUuid, playerName);
        if (existing.isEmpty()) {
            return false;
        }

        WhitelistEntry entry = existing.get();
        removeEntry(entry);
        saveHook.run();
        WhitelistEvent.EVENT.invoker().onWhitelistChanged(PlayerListChangeType.REMOVED, entry);
        return true;
    }

    /**
     * Removes a whitelist entry by UUID.
     *
     * @param playerUuid the player UUID to remove
     * @return {@code true} when an entry was removed
     */
    public boolean remove(UUID playerUuid) {
        return remove(playerUuid, null);
    }

    /**
     * Removes a whitelist entry by player name.
     *
     * @param playerName the player name to remove
     * @return {@code true} when an entry was removed
     */
    public boolean removeByName(String playerName) {
        return remove(null, playerName);
    }

    @Override
    public void clear() {
        if (isEmpty()) {
            return;
        }

        entriesByUuid.clear();
        entriesByName.clear();
        saveHook.run();
        WhitelistEvent.EVENT.invoker().onWhitelistChanged(PlayerListChangeType.CLEARED, null);
    }

    @Override
    public boolean isEmpty() {
        return entriesByUuid.isEmpty() && entriesByName.isEmpty();
    }

    /**
     * Replaces the current whitelist contents without triggering persistence.
     *
     * @param playerUuids the UUID entries to load
     * @param playerNames the player-name entries to load
     */
    public void replaceAll(Iterable<UUID> playerUuids, Iterable<String> playerNames) {
        entriesByUuid.clear();
        entriesByName.clear();

        if (playerUuids != null) {
            for (UUID playerUuid : playerUuids) {
                if (playerUuid != null) {
                    entriesByUuid.put(playerUuid, new WhitelistEntry(playerUuid, ""));
                }
            }
        }

        if (playerNames != null) {
            for (String playerName : playerNames) {
                String normalizedName = normalizeName(playerName);
                if (!normalizedName.isEmpty()) {
                    entriesByName.put(normalizedName, new WhitelistEntry(null, playerName));
                }
            }
        }
    }

    /**
     * Returns a snapshot of all stored whitelist UUIDs.
     *
     * @return the stored UUID entries
     */
    public Set<UUID> playerUuids() {
        return Set.copyOf(entriesByUuid.keySet());
    }

    /**
     * Returns a snapshot of all stored whitelist names.
     *
     * @return the stored normalized player-name entries
     */
    public Set<String> playerNames() {
        return Set.copyOf(entriesByName.keySet());
    }

    private void removeEntry(WhitelistEntry entry) {
        if (entry == null) {
            return;
        }

        if (entry.playerUuid() != null) {
            entriesByUuid.remove(entry.playerUuid());
        }

        String normalizedName = normalizeName(entry.playerName());
        if (!normalizedName.isEmpty()) {
            entriesByName.remove(normalizedName);
        }
    }

    private static String normalizeName(String playerName) {
        if (playerName == null) {
            return "";
        }

        return playerName.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
