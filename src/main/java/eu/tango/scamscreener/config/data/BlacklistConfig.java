package eu.tango.scamscreener.config.data;

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
public final class BlacklistConfig {
    private List<BlacklistEntry> entries = new ArrayList<>();

    /**
     * Returns the normalized persisted blacklist entries.
     *
     * @return non-null blacklist entries
     */
    public List<BlacklistEntry> entries() {
        return entries == null ? new ArrayList<>() : entries;
    }
}
