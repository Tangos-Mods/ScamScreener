package eu.tango.scamscreener.config.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseConfigAsyncWriteQueueTest {
    @TempDir
    Path tempDir;

    @Test
    void syncSaveWinsOverOlderPendingAsyncWrite() throws IOException {
        Path configFile = tempDir.resolve("runtime.json");
        TestConfigStore store = new TestConfigStore(configFile);

        store.saveAsync(new TestConfig("older", 1));
        store.save(new TestConfig("newer", 2));

        TestConfig reloaded = store.reload();

        assertEquals("newer", reloaded.name);
        assertEquals(2, reloaded.value);
        assertTrue(Files.readString(configFile, StandardCharsets.UTF_8).contains("\"name\": \"newer\""));
    }

    @Test
    void reloadFlushesLatestPendingAsyncWrite() {
        Path configFile = tempDir.resolve("rules.json");
        TestConfigStore store = new TestConfigStore(configFile);

        store.saveAsync(new TestConfig("first", 1));
        store.saveAsync(new TestConfig("second", 2));

        TestConfig reloaded = store.reload();

        assertEquals("second", reloaded.name);
        assertEquals(2, reloaded.value);
    }

    @Test
    void asyncWriterBecomesIdleForTests() throws IOException {
        Path configFile = tempDir.resolve("review.json");
        TestConfigStore store = new TestConfigStore(configFile);

        store.saveAsync(new TestConfig("idle-check", 7));
        AsyncFileWorkQueue.awaitIdleForTests();

        assertTrue(Files.readString(configFile, StandardCharsets.UTF_8).contains("\"value\": 7"));
    }

    private static final class TestConfigStore extends BaseConfig<TestConfig> {
        private TestConfigStore(Path path) {
            super(path, TestConfig.class);
        }

        @Override
        protected TestConfig createDefault() {
            return new TestConfig("default", 0);
        }
    }

    private static final class TestConfig {
        private String name;
        private int value;

        private TestConfig() {
        }

        private TestConfig(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
}
