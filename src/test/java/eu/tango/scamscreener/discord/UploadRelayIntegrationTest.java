package eu.tango.scamscreener.discord;

import eu.tango.scamscreener.config.UploadRelayConfig;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadRelayIntegrationTest {
	private static final String API_URL_PROPERTY = "scamscreener.upload.api.url";
	private static final String CLIENT_ID_PROPERTY = "scamscreener.upload.client.id";
	private static final String CLIENT_SECRET_PROPERTY = "scamscreener.upload.client.secret";
	private static final String API_URL_ENV = "SCAMSCREENER_UPLOAD_API_URL";
	private static final String CLIENT_ID_ENV = "SCAMSCREENER_UPLOAD_CLIENT_ID";
	private static final String CLIENT_SECRET_ENV = "SCAMSCREENER_UPLOAD_CLIENT_SECRET";

	@Test
	void uploadsFileToRelayServer() throws Exception {
		String apiUrl = readRequired(API_URL_PROPERTY, API_URL_ENV);
		String clientId = readRequired(CLIENT_ID_PROPERTY, CLIENT_ID_ENV);
		String clientSecret = readRequired(CLIENT_SECRET_PROPERTY, CLIENT_SECRET_ENV);

		UploadRelayConfig config = new UploadRelayConfig();
		config.serverUrl = apiUrl;
		config.clientId = clientId;
		config.clientSecret = clientSecret;
		config.signatureVersion = "v1";
		UploadRelayClient client = new UploadRelayClient(config);

		Path file = Files.createTempFile("relay-integration-", ".csv.old.1");
		try {
			Files.writeString(file, "label,message\n1,relay integration test\n", StandardCharsets.UTF_8);
			UploadRelayClient.UploadResult result = client.uploadTrainingFile(file, new UploadRelayClient.UploaderContext(
				"ci-test-player",
				"00000000-0000-0000-0000-000000000001",
				"19.02.2026 22:10:00"
			));
			assertTrue(result.success(), () -> "Expected successful relay upload, got: " + result.detail());
			assertTrue(result.detail().contains("sha256="), "Expected upload result to include SHA-256 detail.");
		} finally {
			Files.deleteIfExists(file);
		}
	}

	private static String readRequired(String propertyName, String envName) {
		String value = System.getProperty(propertyName);
		if (value == null || value.isBlank()) {
			value = System.getenv(envName);
		}
		assertTrue(
			value != null && !value.isBlank(),
			"Missing relay integration credential: set -" + "D" + propertyName + "=... or " + envName + "."
		);
		return value.trim();
	}
}
