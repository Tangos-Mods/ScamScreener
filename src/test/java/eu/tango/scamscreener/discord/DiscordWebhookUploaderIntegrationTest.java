package eu.tango.scamscreener.discord;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordWebhookUploaderIntegrationTest {
	private static final String WEBHOOK_OVERRIDE_PROPERTY = "scamscreener.discord.webhook.url";
	private static final String WEBHOOK_ENV = "SCAMSCREENER_DISCORD_WEBHOOK_URL";

	@Test
	void uploadsEmbedAndFileToWebhook() throws Exception {
		String configuredWebhook = System.getProperty(WEBHOOK_OVERRIDE_PROPERTY);
		if (configuredWebhook == null || configuredWebhook.isBlank()) {
			configuredWebhook = System.getenv(WEBHOOK_ENV);
		}
		assertTrue(
			configuredWebhook != null && configuredWebhook.startsWith("https://discord.com/api/webhooks/"),
			"Discord webhook is not configured. Set -Dscamscreener.discord.webhook.url=<discord webhook url> or SCAMSCREENER_DISCORD_WEBHOOK_URL."
		);

		String previousWebhook = System.getProperty(WEBHOOK_OVERRIDE_PROPERTY);
		System.setProperty(WEBHOOK_OVERRIDE_PROPERTY, configuredWebhook.trim());

		DiscordWebhookUploader uploader = new DiscordWebhookUploader();
		assertTrue(
			uploader.isConfigured(),
			"Discord webhook is not configured. Set -Dscamscreener.discord.webhook.url=<discord webhook url>."
		);

		Path tempUploadFile = null;
		try {
			tempUploadFile = Files.createTempFile("discord-upload-integration-", ".csv.old.42");
			Files.writeString(tempUploadFile, "label,message\n1,discord integration test\n", StandardCharsets.UTF_8);

			DiscordWebhookUploader.UploaderContext testContext = new DiscordWebhookUploader.UploaderContext(
				"ci-test-player",
				"00000000-0000-0000-0000-000000000001",
				"14.02.2026 01:23:45"
			);

			DiscordWebhookUploader.UploadResult result = uploader.uploadTrainingFile(tempUploadFile, testContext);
			assertTrue(result.success(), () -> "Expected successful Discord upload, got: " + result.detail());
			assertTrue(result.detail().contains("sha256="), "Expected upload result to include SHA-256 detail.");
		} finally {
			if (previousWebhook == null || previousWebhook.isBlank()) {
				System.clearProperty(WEBHOOK_OVERRIDE_PROPERTY);
			} else {
				System.setProperty(WEBHOOK_OVERRIDE_PROPERTY, previousWebhook);
			}
			if (tempUploadFile != null) {
				Files.deleteIfExists(tempUploadFile);
			}
		}
	}
}
