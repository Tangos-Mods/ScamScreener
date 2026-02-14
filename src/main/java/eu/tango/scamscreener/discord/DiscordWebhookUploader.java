package eu.tango.scamscreener.discord;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

public final class DiscordWebhookUploader {
	private static final Gson GSON = new GsonBuilder().create();
	private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1472021483323916461/3eXVN9BT-mGkKAbje2wgDx0A1LObfSbqiMSbLrPSgzm5DxRJ06snqjLJxvM3YULWIRa8";
	private static final String WEBHOOK_USERNAME = "ScamScreener";
	private static final int CONNECT_TIMEOUT_SECONDS = 8;
	private static final int REQUEST_TIMEOUT_SECONDS = 25;
	private static final int MAX_FILE_BYTES = 25 * 1024 * 1024;
	private static final DateTimeFormatter EMBED_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

	public boolean isConfigured() {
		return looksLikeWebhookUrl(normalizedWebhookUrl());
	}

	public UploadResult uploadTrainingFile(Path trainingFile) {
		return uploadTrainingFile(trainingFile, captureCurrentUploader());
	}

	public UploadResult uploadTrainingFile(Path trainingFile, UploaderContext uploaderContext) {
		if (!isConfigured()) {
			return UploadResult.failed("Discord webhook is not configured.");
		}
		if (trainingFile == null || !Files.isRegularFile(trainingFile)) {
			return UploadResult.failed("Training data file not found.");
		}

		try {
			UploadPayload uploadPayload = readUploadPayload(trainingFile);
			UploaderContext normalizedUploader = UploaderContext.normalized(uploaderContext, nowFormattedTimestamp());

			String boundary = "scamscreener-" + UUID.randomUUID().toString().replace("-", "");
			byte[] requestBody = buildMultipartBody(boundary, uploadPayload, normalizedUsername(), normalizedUploader);
			HttpRequest request = HttpRequest.newBuilder(URI.create(withWaitFlag(normalizedWebhookUrl())))
				.timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
				.build();
			HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
				.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			int status = response.statusCode();
			if (status >= 200 && status < 300) {
				UploadVerification verification = verifyWebhookResponse(response.body(), uploadPayload.filename(), uploadPayload.fileBytes().length);
				if (!verification.success()) {
					return UploadResult.failed(verification.detail());
				}
				return UploadResult.ok("Discord upload completed. sha256=" + uploadPayload.sha256());
			}
			String responseSnippet = compactResponse(response.body());
			return UploadResult.failed("HTTP " + status + (responseSnippet.isBlank() ? "" : " - " + responseSnippet));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return UploadResult.failed("Upload interrupted.");
		} catch (Exception e) {
			return UploadResult.failed(messageOrFallback(e, "Discord upload failed."));
		}
	}

	private static String normalizedWebhookUrl() {
		return WEBHOOK_URL == null ? "" : WEBHOOK_URL.trim();
	}

	private static String normalizedUsername() {
		String value = WEBHOOK_USERNAME == null ? "" : WEBHOOK_USERNAME.trim();
		return value.isBlank() ? "ScamScreener" : value;
	}

	public static UploaderContext captureCurrentUploader() {
		String timestamp = nowFormattedTimestamp();
		Minecraft client = Minecraft.getInstance();
		if (client == null) {
			return UploaderContext.unknown(timestamp);
		}

		String playerName = "unknown";
		String playerUuid = "unknown";
		if (client.player != null) {
			var profile = client.player.getGameProfile();
			if (profile != null) {
				if (profile.name() != null && !profile.name().isBlank()) {
					playerName = profile.name().trim();
				}
				if (profile.id() != null) {
					playerUuid = profile.id().toString();
				}
			}
		}

		if ("unknown".equals(playerName)) {
			var user = client.getUser();
			if (user != null && user.getName() != null && !user.getName().isBlank()) {
				playerName = user.getName().trim();
			}
		}
		return new UploaderContext(playerName, playerUuid, timestamp);
	}

	private static String nowFormattedTimestamp() {
		return EMBED_TIMESTAMP_FORMATTER.format(LocalDateTime.now());
	}

	private static UploadPayload readUploadPayload(Path trainingFile) throws IOException {
		byte[] fileBytes = Files.readAllBytes(trainingFile);
		if (fileBytes.length <= 0) {
			throw new IOException("Training data file is empty.");
		}
		if (fileBytes.length > MAX_FILE_BYTES) {
			throw new IOException("Training data file is larger than Discord upload limit (" + MAX_FILE_BYTES + " bytes).");
		}
		String filename = safeFileName(trainingFile);
		String sha256 = sha256Hex(fileBytes);
		return new UploadPayload(fileBytes, filename, sha256);
	}

	private static byte[] buildMultipartBody(String boundary, UploadPayload uploadPayload, String username, UploaderContext uploaderContext) throws IOException {
		byte[] fileBytes = uploadPayload.fileBytes();
		String filename = uploadPayload.filename();

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("username", username);
		payload.put("content", "New ScamScreener training data: " + filename);

		Map<String, Object> embed = new LinkedHashMap<>();
		embed.put("title", "ScamScreener Training Upload");
		embed.put("color", 0xFF5555);
		List<Map<String, Object>> fields = new ArrayList<>();
		fields.add(embedField("Uploader", uploaderContext.playerName(), true));
		fields.add(embedField("UUID", uploaderContext.playerUuid(), false));
		fields.add(embedField("Timestamp", uploaderContext.timestamp(), true));
		fields.add(embedField("Hash-Code", uploadPayload.sha256(), false));
		embed.put("fields", fields);
		payload.put("embeds", List.of(embed));

		String payloadJson = GSON.toJson(payload);

		ByteArrayOutputStream out = new ByteArrayOutputStream(fileBytes.length + 1024);
		writeUtf8(out, "--" + boundary + "\r\n");
		writeUtf8(out, "Content-Disposition: form-data; name=\"payload_json\"\r\n");
		writeUtf8(out, "Content-Type: application/json\r\n\r\n");
		writeUtf8(out, payloadJson + "\r\n");

		writeUtf8(out, "--" + boundary + "\r\n");
		writeUtf8(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n");
		writeUtf8(out, "Content-Type: text/csv\r\n\r\n");
		out.write(fileBytes);
		writeUtf8(out, "\r\n");

		writeUtf8(out, "--" + boundary + "--\r\n");
		return out.toByteArray();
	}

	private static Map<String, Object> embedField(String name, String value, boolean inline) {
		Map<String, Object> field = new LinkedHashMap<>();
		field.put("name", name == null || name.isBlank() ? "unknown" : name);
		field.put("value", value == null || value.isBlank() ? "unknown" : value);
		field.put("inline", inline);
		return field;
	}

	private static void writeUtf8(ByteArrayOutputStream out, String text) {
		if (text == null || text.isEmpty()) {
			return;
		}
		out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
	}

	private static String safeFileName(Path file) {
		if (file == null || file.getFileName() == null) {
			return "training-data.csv";
		}
		String name = file.getFileName().toString().trim();
		if (name.isBlank()) {
			return "training-data.csv";
		}
		StringBuilder sanitized = new StringBuilder(name.length());
		for (int i = 0; i < name.length(); i++) {
			char current = name.charAt(i);
			if (current == '"' || current == '\r' || current == '\n') {
				sanitized.append('_');
				continue;
			}
			sanitized.append(current);
		}
		String out = stripArchiveSuffix(sanitized.toString().trim());
		return out.isBlank() ? "training-data.csv" : out;
	}

	private static String stripArchiveSuffix(String name) {
		if (name == null || name.isBlank()) {
			return "";
		}
		String lowered = name.toLowerCase(Locale.ROOT);
		int marker = lowered.lastIndexOf(".old.");
		if (marker < 0) {
			return name;
		}
		int digitsStart = marker + 5;
		if (digitsStart >= name.length()) {
			return name;
		}
		for (int i = digitsStart; i < name.length(); i++) {
			if (!Character.isDigit(name.charAt(i))) {
				return name;
			}
		}
		return name.substring(0, marker);
	}

	private static UploadVerification verifyWebhookResponse(String responseBody, String expectedFilename, int expectedSize) {
		if (responseBody == null || responseBody.isBlank()) {
			return UploadVerification.failed("Upload verification failed: empty webhook response.");
		}

		try {
			JsonElement root = JsonParser.parseString(responseBody);
			if (!root.isJsonObject()) {
				return UploadVerification.failed("Upload verification failed: invalid webhook response format.");
			}
			JsonObject object = root.getAsJsonObject();
			JsonArray attachments = object.getAsJsonArray("attachments");
			if (attachments == null || attachments.size() != 1) {
				return UploadVerification.failed("Upload verification failed: webhook response does not contain exactly one attachment.");
			}

			JsonElement attachmentElement = attachments.get(0);
			if (!attachmentElement.isJsonObject()) {
				return UploadVerification.failed("Upload verification failed: malformed attachment metadata.");
			}
			JsonObject attachment = attachmentElement.getAsJsonObject();
			String uploadedFilename = jsonString(attachment, "filename");
			long uploadedSize = jsonLong(attachment, "size");

			if (!expectedFilename.equals(uploadedFilename)) {
				return UploadVerification.failed("Upload verification failed: filename mismatch.");
			}
			if (uploadedSize != expectedSize) {
				return UploadVerification.failed("Upload verification failed: size mismatch.");
			}
			return UploadVerification.ok();
		} catch (Exception e) {
			return UploadVerification.failed("Upload verification failed: " + messageOrFallback(e, "unknown parse error"));
		}
	}

	private static String jsonString(JsonObject object, String key) {
		if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
			return "";
		}
		try {
			return object.get(key).getAsString();
		} catch (Exception ignored) {
			return "";
		}
	}

	private static long jsonLong(JsonObject object, String key) {
		if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
			return -1L;
		}
		try {
			return object.get(key).getAsLong();
		} catch (Exception ignored) {
			return -1L;
		}
	}

	private static String sha256Hex(byte[] bytes) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(bytes);
			StringBuilder out = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				out.append(Character.forDigit((b >>> 4) & 0xF, 16));
				out.append(Character.forDigit(b & 0xF, 16));
			}
			return out.toString();
		} catch (Exception e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}

	private static String compactResponse(String body) {
		if (body == null || body.isBlank()) {
			return "";
		}
		String singleLine = body.replace('\r', ' ').replace('\n', ' ').trim();
		if (singleLine.length() <= 180) {
			return singleLine;
		}
		return singleLine.substring(0, 177) + "...";
	}

	private static String withWaitFlag(String url) {
		if (url == null || url.isBlank()) {
			return "";
		}
		String trimmed = url.trim();
		if (trimmed.matches(".*[?&]wait=[^&]*.*")) {
			return trimmed;
		}
		return trimmed + (trimmed.contains("?") ? "&" : "?") + "wait=true";
	}

	private static boolean looksLikeWebhookUrl(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}
		String normalized = value.toLowerCase(Locale.ROOT);
		return normalized.startsWith("https://discord.com/api/webhooks/")
			|| normalized.startsWith("https://ptb.discord.com/api/webhooks/")
			|| normalized.startsWith("https://canary.discord.com/api/webhooks/");
	}

	private static String messageOrFallback(Exception error, String fallback) {
		if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
			return fallback;
		}
		return error.getMessage().trim();
	}

	private record UploadPayload(byte[] fileBytes, String filename, String sha256) {
	}

	public record UploaderContext(String playerName, String playerUuid, String timestamp) {
		private static UploaderContext unknown(String timestamp) {
			return new UploaderContext("unknown", "unknown", timestamp == null || timestamp.isBlank() ? "unknown" : timestamp);
		}

		private static UploaderContext normalized(UploaderContext value, String fallbackTimestamp) {
			if (value == null) {
				return unknown(fallbackTimestamp);
			}
			String normalizedName = value.playerName() == null || value.playerName().isBlank() ? "unknown" : value.playerName().trim();
			String normalizedUuid = value.playerUuid() == null || value.playerUuid().isBlank() ? "unknown" : value.playerUuid().trim();
			String normalizedTimestamp = value.timestamp() == null || value.timestamp().isBlank()
				? (fallbackTimestamp == null || fallbackTimestamp.isBlank() ? "unknown" : fallbackTimestamp)
				: value.timestamp().trim();
			return new UploaderContext(normalizedName, normalizedUuid, normalizedTimestamp);
		}
	}

	private record UploadVerification(boolean success, String detail) {
		private static UploadVerification ok() {
			return new UploadVerification(true, "");
		}

		private static UploadVerification failed(String detail) {
			return new UploadVerification(false, detail == null ? "unknown error" : detail);
		}
	}

	public record UploadResult(boolean success, String detail) {
		public static UploadResult ok(String detail) {
			return new UploadResult(true, detail == null ? "" : detail);
		}

		public static UploadResult failed(String detail) {
			return new UploadResult(false, detail == null ? "unknown error" : detail);
		}
	}
}
