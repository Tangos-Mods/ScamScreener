package eu.tango.scamscreener.lists;

import eu.tango.scamscreener.api.BlacklistAccess;
import eu.tango.scamscreener.api.event.BlacklistEvent;
import eu.tango.scamscreener.api.event.PlayerListChangeType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory blacklist for explicitly blocked players.
 */
public final class Blacklist implements BlacklistAccess {
    private final Map<UUID, BlacklistEntry> entriesByUuid = new LinkedHashMap<>();
    private final Map<String, BlacklistEntry> entriesByName = new LinkedHashMap<>();
    private final Runnable saveHook;

    /**
     * Creates an in-memory blacklist without persistence hooks.
     */
    public Blacklist() {
        this(() -> {
        });
    }

    /**
     * Creates an in-memory blacklist with a persistence hook.
     *
     * @param saveHook callback triggered after mutating changes
     */
    public Blacklist(Runnable saveHook) {
        this.saveHook = saveHook == null ? () -> {
        } : saveHook;
    }

    /**
     * Adds or updates a blacklist entry.
     *
     * @param playerUuid the player UUID, if available
     * @param playerName the player name, if available
     * @param score the score associated with the blacklist entry
     * @param reason the reason for the blacklist entry
     * @param source the origin of the blacklist entry
     * @return {@code true} when the entry was stored
     */
    public boolean add(UUID playerUuid, String playerName, int score, String reason, BlacklistSource source) {
        String normalizedName = normalizeName(playerName);
        if (playerUuid == null && normalizedName.isEmpty()) {
            return false;
        }

        Optional<BlacklistEntry> existing = find(playerUuid, playerName);
        existing.ifPresent(this::removeEntry);

        BlacklistEntry entry = new BlacklistEntry(playerUuid, playerName, score, reason, source);
        if (entry.playerUuid() != null) {
            entriesByUuid.put(entry.playerUuid(), entry);
        }
        if (!normalizedName.isEmpty()) {
            entriesByName.put(normalizedName, entry);
        }

        saveHook.run();
        PlayerListChangeType changeType = existing.isPresent() ? PlayerListChangeType.UPDATED : PlayerListChangeType.ADDED;
        BlacklistEvent.EVENT.invoker().onBlacklistChanged(changeType, entry);
        return true;
    }

    /**
     * Looks up a blacklist entry by UUID.
     *
     * @param playerUuid the player UUID to look up
     * @return the matching blacklist entry, when present
     */
    public Optional<BlacklistEntry> get(UUID playerUuid) {
        if (playerUuid == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(entriesByUuid.get(playerUuid));
    }

    /**
     * Looks up a blacklist entry by player name.
     *
     * @param playerName the player name to look up
     * @return the matching blacklist entry, when present
     */
    public Optional<BlacklistEntry> findByName(String playerName) {
        String normalizedName = normalizeName(playerName);
        if (normalizedName.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(entriesByName.get(normalizedName));
    }

    /**
     * Looks up a blacklist entry by UUID or player name.
     *
     * @param playerUuid the player UUID, if available
     * @param playerName the player name, if available
     * @return the matching blacklist entry, when present
     */
    public Optional<BlacklistEntry> find(UUID playerUuid, String playerName) {
        Optional<BlacklistEntry> byUuid = get(playerUuid);
        if (byUuid.isPresent()) {
            return byUuid;
        }

        return findByName(playerName);
    }

    /**
     * Returns every unique blacklist entry.
     *
     * @return the stored blacklist entries in insertion order
     */
    public List<BlacklistEntry> entries() {
        Set<BlacklistEntry> unique = new LinkedHashSet<>();
        unique.addAll(entriesByUuid.values());
        unique.addAll(entriesByName.values());
        return new ArrayList<>(unique);
    }

    /**
     * Returns every unique blacklist entry.
     *
     * @return the stored blacklist entries in insertion order
     */
    public List<BlacklistEntry> allEntries() {
        return entries();
    }

    public boolean contains(UUID playerUuid, String playerName) {
        return find(playerUuid, playerName).isPresent();
    }

    /**
     * Checks whether the blacklist contains the given UUID.
     *
     * @param playerUuid the player UUID to check
     * @return {@code true} when a matching entry exists
     */
    public boolean contains(UUID playerUuid) {
        return get(playerUuid).isPresent();
    }

    /**
     * Checks whether the blacklist contains the given player name.
     *
     * @param playerName the player name to check
     * @return {@code true} when a matching entry exists
     */
    public boolean containsName(String playerName) {
        return findByName(playerName).isPresent();
    }

    public boolean remove(UUID playerUuid, String playerName) {
        Optional<BlacklistEntry> existing = find(playerUuid, playerName);
        if (existing.isEmpty()) {
            return false;
        }

        BlacklistEntry entry = existing.get();
        removeEntry(entry);
        saveHook.run();
        BlacklistEvent.EVENT.invoker().onBlacklistChanged(PlayerListChangeType.REMOVED, entry);
        return true;
    }

    /**
     * Removes a blacklist entry by UUID.
     *
     * @param playerUuid the player UUID to remove
     * @return {@code true} when an entry was removed
     */
    public boolean remove(UUID playerUuid) {
        return remove(playerUuid, null);
    }

    /**
     * Removes a blacklist entry by player name.
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
        BlacklistEvent.EVENT.invoker().onBlacklistChanged(PlayerListChangeType.CLEARED, null);
    }

    @Override
    public boolean isEmpty() {
        return entriesByUuid.isEmpty() && entriesByName.isEmpty();
    }

    /**
     * Replaces the current blacklist contents without triggering persistence.
     *
     * @param entries the entries to load
     */
    public void replaceAll(Iterable<BlacklistEntry> entries) {
        entriesByUuid.clear();
        entriesByName.clear();
        if (entries == null) {
            return;
        }

        for (BlacklistEntry entry : entries) {
            if (entry == null) {
                continue;
            }

            String normalizedName = normalizeName(entry.playerName());
            if (entry.playerUuid() == null && normalizedName.isEmpty()) {
                continue;
            }

            if (entry.playerUuid() != null) {
                entriesByUuid.put(entry.playerUuid(), entry);
            }
            if (!normalizedName.isEmpty()) {
                entriesByName.put(normalizedName, entry);
            }
        }
    }

    private void removeEntry(BlacklistEntry entry) {
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
