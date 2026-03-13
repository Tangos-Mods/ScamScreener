package eu.tango.scamscreener.api;

import eu.tango.scamscreener.config.data.AlertRiskLevel;
import eu.tango.scamscreener.config.data.RuntimeConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeConfigSettingsApiTest {
    @Test
    void gettersReflectCurrentRuntimeConfigValues() {
        RuntimeConfig config = new RuntimeConfig();
        config.output().setPingOnRiskWarning(false);
        config.output().setPingOnBlacklistWarning(true);
        config.alerts().setMinimumRiskLevel(AlertRiskLevel.CRITICAL);

        RuntimeConfigSettingsApi api = new RuntimeConfigSettingsApi(() -> config, () -> {
        });

        assertFalse(api.pingOnRiskWarning());
        assertTrue(api.pingOnBlacklistWarning());
        assertEquals(ScamScreenerAlertLevel.CRITICAL, api.alertMinimumRiskLevel());
    }

    @Test
    void settersUpdateConfigAndPersistOnlyOnEffectiveChanges() {
        RuntimeConfig config = new RuntimeConfig();
        AtomicInteger saves = new AtomicInteger();
        RuntimeConfigSettingsApi api = new RuntimeConfigSettingsApi(() -> config, saves::incrementAndGet);

        api.setPingOnRiskWarning(false);
        api.setPingOnRiskWarning(false);
        api.setPingOnBlacklistWarning(false);
        api.setAlertMinimumRiskLevel(ScamScreenerAlertLevel.HIGH);
        api.setAlertMinimumRiskLevel(ScamScreenerAlertLevel.HIGH);

        assertFalse(config.output().isPingOnRiskWarning());
        assertFalse(config.output().isPingOnBlacklistWarning());
        assertEquals(AlertRiskLevel.HIGH, config.alerts().minimumRiskLevel());
        assertEquals(3, saves.get());
    }

    @Test
    void setAlertMinimumRiskLevelRejectsNull() {
        RuntimeConfigSettingsApi api = new RuntimeConfigSettingsApi(RuntimeConfig::new, () -> {
        });

        assertThrows(NullPointerException.class, () -> api.setAlertMinimumRiskLevel(null));
    }
}
