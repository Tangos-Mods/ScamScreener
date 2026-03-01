package eu.tango.scamscreener.lists;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerListApiTest {
    @Test
    void whitelistSupportsLookupAndListingByUuidAndName() {
        Whitelist whitelist = new Whitelist();
        UUID playerUuid = UUID.randomUUID();

        assertTrue(whitelist.add(playerUuid, "TrustedPlayer"));
        assertTrue(whitelist.contains(playerUuid));
        assertTrue(whitelist.containsName("trustedplayer"));
        assertTrue(whitelist.get(playerUuid).isPresent());
        assertTrue(whitelist.findByName("TrustedPlayer").isPresent());
        assertEquals(1, whitelist.allEntries().size());
        assertTrue(whitelist.remove(playerUuid));
        assertFalse(whitelist.contains(playerUuid));
    }

    @Test
    void blacklistSupportsLookupAndRemovalByUuidAndName() {
        Blacklist blacklist = new Blacklist();
        UUID playerUuid = UUID.randomUUID();

        assertTrue(blacklist.add(playerUuid, "BlockedPlayer", 40, "manual", BlacklistSource.API));
        assertTrue(blacklist.contains(playerUuid));
        assertTrue(blacklist.containsName("blockedplayer"));
        assertTrue(blacklist.get(playerUuid).isPresent());
        assertTrue(blacklist.findByName("BlockedPlayer").isPresent());
        assertEquals(1, blacklist.allEntries().size());
        assertTrue(blacklist.removeByName("BlockedPlayer"));
        assertFalse(blacklist.contains(playerUuid));
    }
}
