package eu.tango.scamscreener.lists;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerUuidLookupTest {
    @Test
    void parsePlayerUuidReadsCompactUuidPayloads() {
        UUID expectedUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        UUID parsedUuid = PlayerUuidLookup.parsePlayerUuid(200, """
            {
              "id": "123e4567e89b12d3a456426614174000",
              "name": "Alpha"
            }
            """);

        assertEquals(expectedUuid, parsedUuid);
    }

    @Test
    void parsePlayerUuidIgnoresMissingAndInvalidResponses() {
        assertNull(PlayerUuidLookup.parsePlayerUuid(404, """
            {
              "error": "NOT_FOUND"
            }
            """));
        assertNull(PlayerUuidLookup.parsePlayerUuid(200, """
            {
              "id": "invalid"
            }
            """));
    }
}
