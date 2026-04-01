package eu.tango.scamscreener.config.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeConfigTest {
    @Test
    void trainingUploadRetrySettingsUseDefaultsAndClampInvalidValues() {
        RuntimeConfig config = new RuntimeConfig();

        assertEquals(3, config.trainingUploadRetryCount());
        assertEquals(30, config.trainingUploadRetryDelaySeconds());

        config.setTrainingUploadRetryCount(-4);
        config.setTrainingUploadRetryDelaySeconds(0);

        assertEquals(0, config.trainingUploadRetryCount());
        assertEquals(1, config.trainingUploadRetryDelaySeconds());
    }
}
