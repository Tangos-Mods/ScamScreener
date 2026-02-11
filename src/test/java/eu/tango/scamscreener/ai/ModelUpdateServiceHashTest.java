package eu.tango.scamscreener.ai;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelUpdateServiceHashTest {
	private static Method hashMatchesExpectedMethod;
	private static Method compareModelVersionsMethod;

	@BeforeAll
	static void setUpReflection() throws Exception {
		hashMatchesExpectedMethod = ModelUpdateService.class.getDeclaredMethod("hashMatchesExpected", byte[].class, String.class);
		hashMatchesExpectedMethod.setAccessible(true);
		compareModelVersionsMethod = ModelUpdateService.class.getDeclaredMethod("compareModelVersions", String.class, byte[].class);
		compareModelVersionsMethod.setAccessible(true);
	}

	@Test
	void hashMatchesExpectedAcceptsExactHash() throws Exception {
		byte[] payload = "line-one\nline-two\n".getBytes(StandardCharsets.UTF_8);
		String expected = sha256(payload);

		assertTrue(hashMatchesExpected(payload, expected));
	}

	@Test
	void hashMatchesExpectedAcceptsLineEndingAndBomVariants() throws Exception {
		byte[] localPayload = "line-one\nline-two\n".getBytes(StandardCharsets.UTF_8);
		byte[] remoteVariant = ("\uFEFF" + "line-one\r\nline-two\r\n").getBytes(StandardCharsets.UTF_8);
		String expected = sha256(remoteVariant);

		assertTrue(hashMatchesExpected(localPayload, expected));
	}

	@Test
	void hashMatchesExpectedRejectsDifferentContent() throws Exception {
		byte[] localPayload = "line-one\nline-two\n".getBytes(StandardCharsets.UTF_8);
		String expected = sha256("line-one\nother\n".getBytes(StandardCharsets.UTF_8));

		assertFalse(hashMatchesExpected(localPayload, expected));
	}

	@Test
	void hashMatchesExpectedRejectsMissingInputs() throws Exception {
		assertFalse(hashMatchesExpected(null, "abc"));
		assertFalse(hashMatchesExpected("abc".getBytes(StandardCharsets.UTF_8), null));
		assertFalse(hashMatchesExpected("abc".getBytes(StandardCharsets.UTF_8), " "));
	}

	@Test
	void compareModelVersionsMarksOlderOrEqualRemoteAsUpToDate() throws Exception {
		byte[] localPayload = "{\"version\":13}".getBytes(StandardCharsets.UTF_8);

		Object equal = compareModelVersions("13", localPayload);
		Object older = compareModelVersions("12", localPayload);

		assertTrue(readBooleanRecordField(equal, "comparable"));
		assertTrue(readBooleanRecordField(equal, "upToDate"));
		assertTrue(readBooleanRecordField(older, "upToDate"));
	}

	@Test
	void compareModelVersionsMarksNewerRemoteAsNotUpToDate() throws Exception {
		byte[] localPayload = "{\"version\":13}".getBytes(StandardCharsets.UTF_8);

		Object comparison = compareModelVersions("14", localPayload);

		assertTrue(readBooleanRecordField(comparison, "comparable"));
		assertFalse(readBooleanRecordField(comparison, "upToDate"));
	}

	@Test
	void compareModelVersionsFallsBackWhenVersionsCannotBeParsed() throws Exception {
		Object comparison = compareModelVersions("invalid", "not-json".getBytes(StandardCharsets.UTF_8));

		assertNotNull(comparison);
		assertFalse(readBooleanRecordField(comparison, "comparable"));
		assertFalse(readBooleanRecordField(comparison, "upToDate"));
	}

	private static boolean hashMatchesExpected(byte[] payload, String expectedSha) throws Exception {
		return (boolean) hashMatchesExpectedMethod.invoke(null, payload, expectedSha);
	}

	private static Object compareModelVersions(String remoteVersion, byte[] localBytes) throws Exception {
		return compareModelVersionsMethod.invoke(null, remoteVersion, localBytes);
	}

	private static boolean readBooleanRecordField(Object target, String accessor) throws Exception {
		Method method = target.getClass().getDeclaredMethod(accessor);
		method.setAccessible(true);
		return (boolean) method.invoke(target);
	}

	private static String sha256(byte[] bytes) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(bytes);
		StringBuilder out = new StringBuilder(hash.length * 2);
		for (byte value : hash) {
			out.append(String.format("%02x", value));
		}
		return out.toString();
	}
}
