package eu.tango.scamscreener.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScamRulesConfigTest {
	@Test
	void defaultAlertThresholdIsMedium() {
		ScamRulesConfig config = new ScamRulesConfig();

		assertEquals("MEDIUM", config.minAlertRiskLevel);
		assertTrue(Boolean.FALSE.equals(config.alertThresholdMediumMigrationDone));
	}

	@Test
	void withDefaultsForcesMediumWhenMigrationNotDone() throws Exception {
		ScamRulesConfig config = new ScamRulesConfig();
		config.minAlertRiskLevel = "HIGH";
		config.alertThresholdMediumMigrationDone = false;

		invokeWithDefaults(config);

		assertEquals("MEDIUM", config.minAlertRiskLevel);
		assertTrue(Boolean.TRUE.equals(config.alertThresholdMediumMigrationDone));
	}

	@Test
	void withDefaultsKeepsPlayerChoiceAfterMigration() throws Exception {
		ScamRulesConfig config = new ScamRulesConfig();
		config.minAlertRiskLevel = "CRITICAL";
		config.alertThresholdMediumMigrationDone = true;

		invokeWithDefaults(config);

		assertEquals("CRITICAL", config.minAlertRiskLevel);
		assertTrue(Boolean.TRUE.equals(config.alertThresholdMediumMigrationDone));
	}

	@Test
	void defaultCapturedChatCacheSizeIsSet() {
		ScamRulesConfig config = new ScamRulesConfig();

		assertEquals(ScamRulesConfig.DEFAULT_CAPTURED_CHAT_CACHE_SIZE, config.capturedChatCacheSize);
	}

	@Test
	void withDefaultsResetsInvalidCapturedChatCacheSizeToDefault() throws Exception {
		ScamRulesConfig config = new ScamRulesConfig();
		config.capturedChatCacheSize = -1;

		invokeWithDefaults(config);

		assertEquals(ScamRulesConfig.DEFAULT_CAPTURED_CHAT_CACHE_SIZE, config.capturedChatCacheSize);
	}

	@Test
	void withDefaultsCapsCapturedChatCacheSizeAtMemorySafeLimit() throws Exception {
		ScamRulesConfig config = new ScamRulesConfig();
		config.capturedChatCacheSize = 20_000;

		invokeWithDefaults(config);

		assertEquals(ScamRulesConfig.DEFAULT_CAPTURED_CHAT_CACHE_SIZE, config.capturedChatCacheSize);
	}

	private static void invokeWithDefaults(ScamRulesConfig config) throws Exception {
		Method method = ScamRulesConfig.class.getDeclaredMethod("withDefaults");
		method.setAccessible(true);
		method.invoke(config);
	}
}
