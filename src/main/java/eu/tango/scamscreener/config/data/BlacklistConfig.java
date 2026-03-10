package eu.tango.scamscreener.config.data;

import eu.tango.scamscreener.config.migration.VersionedConfig;
import eu.tango.scamscreener.lists.BlacklistEntry;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Persisted blacklist payload stored in {@code blacklist.json}.
 */
@Getter
@NoArgsConstructor
public final class BlacklistConfig implements VersionedConfig {
    private int version;
    private List<BlacklistEntry> entries = new ArrayList<>();

    @Override
    public int version() {
        return Math.max(0, version);
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Returns the normalized persisted blacklist entries.
     *
     * @return non-null blacklist entries
     */
    public List<BlacklistEntry> entries() {
        return entries == null ? new ArrayList<>() : entries;
    }
}
