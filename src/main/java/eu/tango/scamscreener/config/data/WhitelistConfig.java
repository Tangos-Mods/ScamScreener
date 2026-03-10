package eu.tango.scamscreener.config.data;

import eu.tango.scamscreener.config.migration.VersionedConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Persisted whitelist payload stored in {@code whitelist.json}.
 */
@Getter
@NoArgsConstructor
public final class WhitelistConfig implements VersionedConfig {
    private int version;
    private List<String> playerUuids = new ArrayList<>();
    private List<String> playerNames = new ArrayList<>();

    @Override
    public int version() {
        return Math.max(0, version);
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Returns the normalized persisted UUID entries.
     *
     * @return non-null UUID entries
     */
    public List<String> playerUuids() {
        return playerUuids == null ? new ArrayList<>() : playerUuids;
    }

    /**
     * Returns the normalized persisted player-name entries.
     *
     * @return non-null player-name entries
     */
    public List<String> playerNames() {
        return playerNames == null ? new ArrayList<>() : playerNames;
    }
}
