package eu.tango.scamscreener.discord;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordWebhookUploaderIntegrationTest {
	private static final String ENABLED_PROPERTY = "scamscreener.discord.integration.enabled";

	@Test
	void uploadsEmbedAndFileToWebhook() throws Exception {
		Assumptions.assumeTrue(Boolean.getBoolean(ENABLED_PROPERTY), "Discord upload integration test disabled.");
		DiscordWebhookUploader uploader = new DiscordWebhookUploader();
		Assumptions.assumeTrue(uploader.isConfigured(), "No Discord webhook configured for integration test.");

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
			if (tempUploadFile != null) {
				Files.deleteIfExists(tempUploadFile);
			}
		}
	}
}
