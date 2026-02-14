package eu.tango.scamscreener.discord;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordWebhookUploaderIntegrationTest {
	private static final String ENABLED_PROPERTY = "scamscreener.discord.integration.enabled";
	private static final String WEBHOOK_URL_PROPERTY = "scamscreener.discord.webhookUrl";
	private static final String WEBHOOK_URL_ENV = "SCAMSCREENER_DISCORD_WEBHOOK_URL";

	@Test
	void uploadsEmbedAndFileToConfiguredWebhook() throws Exception {
		Assumptions.assumeTrue(Boolean.getBoolean(ENABLED_PROPERTY), "Discord upload integration test disabled.");

		String configuredWebhook = configuredWebhookUrl();
		Assumptions.assumeTrue(configuredWebhook != null && !configuredWebhook.isBlank(), "No Discord webhook configured for integration test.");

		String previousWebhook = System.getProperty(WEBHOOK_URL_PROPERTY);
		Path tempUploadFile = null;
		try {
			// Keep the test isolated from the hardcoded default webhook.
			System.setProperty(WEBHOOK_URL_PROPERTY, configuredWebhook);
			tempUploadFile = Files.createTempFile("discord-upload-integration-", ".csv.old.42");
			Files.writeString(tempUploadFile, "label,message\n1,discord integration test\n", StandardCharsets.UTF_8);

			DiscordWebhookUploader uploader = new DiscordWebhookUploader();
			DiscordWebhookUploader.UploaderContext testContext = new DiscordWebhookUploader.UploaderContext(
				"ci-test-player",
				"00000000-0000-0000-0000-000000000001",
				"14.02.2026 01:23:45"
			);

			DiscordWebhookUploader.UploadResult result = uploader.uploadTrainingFile(tempUploadFile, testContext);
			assertTrue(result.success(), () -> "Expected successful Discord upload, got: " + result.detail());
			assertTrue(result.detail().contains("sha256="), "Expected upload result to include SHA-256 detail.");
		} finally {
			if (previousWebhook == null) {
				System.clearProperty(WEBHOOK_URL_PROPERTY);
			} else {
				System.setProperty(WEBHOOK_URL_PROPERTY, previousWebhook);
			}
			if (tempUploadFile != null) {
				Files.deleteIfExists(tempUploadFile);
			}
		}
	}

	private static String configuredWebhookUrl() {
		String fromProperty = System.getProperty(WEBHOOK_URL_PROPERTY);
		if (fromProperty != null && !fromProperty.isBlank()) {
			return fromProperty.trim();
		}
		String fromEnv = System.getenv(WEBHOOK_URL_ENV);
		if (fromEnv != null && !fromEnv.isBlank()) {
			return fromEnv.trim();
		}
		return null;
	}
}
