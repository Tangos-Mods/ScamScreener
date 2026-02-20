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
	void defaultMarketSafetyValuesAreSet() {
		ScamRulesConfig config = new ScamRulesConfig();

		assertTrue(config.marketSafetyEnabled);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_SAFETY_PROFILE, config.marketSafetyProfile);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_CONFIRM_CLICKS_REQUIRED, config.marketConfirmClicksRequired);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_AH_OVERBID_WARN_MULTIPLE, config.marketAhOverbidWarnMultiple, 0.0001);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_RARE_UNDERPRICE_BLOCK_RATIO, config.marketRareUnderpriceBlockRatio, 0.0001);
	}

	@Test
	void withDefaultsClampsInvalidMarketValues() throws Exception {
		ScamRulesConfig config = new ScamRulesConfig();
		config.marketSafetyProfile = "invalid";
		config.marketConfirmClicksRequired = 99;
		config.marketConfirmWindowSeconds = -5;
		config.marketAhOverbidWarnMultiple = 0.1;
		config.marketAhOverbidBlockMultiple = 0.2;
		config.marketInflatedWarnMultiple30d = 0.3;
		config.marketInflatedSevereMultiple30d = 0.4;
		config.marketNpcWarnMultiple = 1.0;
		config.marketNpcBlockMultiple = 1.5;
		config.marketRareUnderpriceWarnRatio = 1.5;
		config.marketRareUnderpriceBlockRatio = 2.0;

		invokeWithDefaults(config);

		assertEquals(ScamRulesConfig.DEFAULT_MARKET_SAFETY_PROFILE, config.marketSafetyProfile);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_CONFIRM_CLICKS_REQUIRED, config.marketConfirmClicksRequired);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_CONFIRM_WINDOW_SECONDS, config.marketConfirmWindowSeconds);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_AH_OVERBID_WARN_MULTIPLE, config.marketAhOverbidWarnMultiple, 0.0001);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_AH_OVERBID_BLOCK_MULTIPLE, config.marketAhOverbidBlockMultiple, 0.0001);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_INFLATED_WARN_MULTIPLE_30D, config.marketInflatedWarnMultiple30d, 0.0001);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_INFLATED_SEVERE_MULTIPLE_30D, config.marketInflatedSevereMultiple30d, 0.0001);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_NPC_WARN_MULTIPLE, config.marketNpcWarnMultiple, 0.0001);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_NPC_BLOCK_MULTIPLE, config.marketNpcBlockMultiple, 0.0001);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_RARE_UNDERPRICE_WARN_RATIO, config.marketRareUnderpriceWarnRatio, 0.0001);
		assertEquals(ScamRulesConfig.DEFAULT_MARKET_RARE_UNDERPRICE_BLOCK_RATIO, config.marketRareUnderpriceBlockRatio, 0.0001);
	}

	private static void invokeWithDefaults(ScamRulesConfig config) throws Exception {
		Method method = ScamRulesConfig.class.getDeclaredMethod("withDefaults");
		method.setAccessible(true);
		method.invoke(config);
	}
}
