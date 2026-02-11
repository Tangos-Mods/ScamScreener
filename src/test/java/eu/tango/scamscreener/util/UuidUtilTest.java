package eu.tango.scamscreener.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UuidUtilTest {
	@Test
	void parseAcceptsTrimmedValidUuid() {
		UUID uuid = UUID.randomUUID();

		assertEquals(uuid, UuidUtil.parse("  " + uuid + "  "));
	}

	@Test
	void parseReturnsNullForInvalidInput() {
		assertNull(UuidUtil.parse(null));
		assertNull(UuidUtil.parse(""));
		assertNull(UuidUtil.parse("not-a-uuid"));
	}
}
