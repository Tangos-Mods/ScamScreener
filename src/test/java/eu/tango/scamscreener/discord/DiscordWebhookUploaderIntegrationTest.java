package eu.tango.scamscreener.discord;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordWebhookUploaderIntegrationTest {
	private static final String WEBHOOK_OVERRIDE_PROPERTY = "scamscreener.discord.webhook.url";
	private static final String LEGACY_TEST_WEBHOOK_URL = "https://discord.com/api/webhooks/1472021483323916461/3eXVN9BT-mGkKAbje2wgDx0A1LObfSbqiMSbLrPSgzm5DxRJ06snqjLJxvM3YULWIRa8";

	@Test
	void uploadsEmbedAndFileToWebhook() throws Exception {
		String previousWebhook = System.getProperty(WEBHOOK_OVERRIDE_PROPERTY);
		System.setProperty(WEBHOOK_OVERRIDE_PROPERTY, LEGACY_TEST_WEBHOOK_URL);

		DiscordWebhookUploader uploader = new DiscordWebhookUploader();

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
