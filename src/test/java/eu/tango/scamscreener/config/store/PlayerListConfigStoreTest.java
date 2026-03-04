package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.BlacklistConfig;
import eu.tango.scamscreener.config.data.WhitelistConfig;
import eu.tango.scamscreener.lists.Blacklist;
import eu.tango.scamscreener.lists.BlacklistEntry;
import eu.tango.scamscreener.lists.BlacklistSource;
import eu.tango.scamscreener.lists.Whitelist;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerListConfigStoreTest {
    @Test
    void whitelistConversionKeepsUuidValues() {
        UUID playerUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Whitelist whitelist = new Whitelist();
        whitelist.add(playerUuid, "Alpha");

        WhitelistConfig config = WhitelistConfigStore.fromWhitelist(whitelist);

        assertTrue(config.playerUuids().contains(playerUuid.toString()));
        assertTrue(config.playerNames().contains("alpha"));
    }

    @Test
    void whitelistConfigLoadRestoresUuidEntries() {
        UUID playerUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        WhitelistConfig config = new WhitelistConfig();
        config.getPlayerUuids().add(playerUuid.toString());
        config.getPlayerNames().add("Alpha");

        Whitelist whitelist = new Whitelist();
        WhitelistConfigStore.applyToWhitelist(config, whitelist);

        assertTrue(whitelist.contains(playerUuid));
        assertTrue(whitelist.containsName("Alpha"));
    }

    @Test
    void blacklistConversionKeepsUuidValues() {
        UUID playerUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Blacklist blacklist = new Blacklist();
        blacklist.add(playerUuid, "Alpha", 75, "manual", BlacklistSource.PLAYER);

        BlacklistConfig config = BlacklistConfigStore.fromBlacklist(blacklist);

        assertEquals(playerUuid, config.entries().getFirst().playerUuid());
        assertEquals("Alpha", config.entries().getFirst().playerName());
    }

    @Test
    void blacklistConfigLoadRestoresUuidEntries() {
        UUID playerUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        BlacklistConfig config = new BlacklistConfig();
        config.getEntries().add(new BlacklistEntry(playerUuid, "Alpha", 75, "manual", BlacklistSource.PLAYER));

        Blacklist blacklist = new Blacklist();
        BlacklistConfigStore.applyToBlacklist(config, blacklist);

        assertTrue(blacklist.contains(playerUuid));
    }
}
