package eu.tango.scamscreener.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.tango.scamscreener.config.UploadRelayConfig;
import eu.tango.scamscreener.util.VersionInfo;
import net.minecraft.client.Minecraft;

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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class UploadRelayClient {
	private static final Gson GSON = new GsonBuilder().create();
	private static final String BOOTSTRAP_PATH = "/api/v1/client/bootstrap";
	private static final String REDEEM_PATH = "/api/v1/client/redeem";
	private static final String UPLOAD_PATH = "/api/v1/training-uploads";
	private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
	private static final String SIGNATURE_VERSION_V1 = "v1";
	private static final int MAX_FILE_BYTES = 25 * 1024 * 1024;
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

	private final Object lock = new Object();
	private final UploadRelayConfig config;

	public UploadRelayClient() {
		this(UploadRelayConfig.loadOrCreate());
	}

	public UploadRelayClient(UploadRelayConfig config) {
		this.config = (config == null ? new UploadRelayConfig() : config).normalizeInPlace();
	}

	public RelayStatus status() {
		synchronized (lock) {
			String clientId = config.clientId == null ? "" : config.clientId.trim();
			String preview = clientId.isBlank() ? "not configured" : previewClientId(clientId);
			return new RelayStatus(config.normalizedServerUrl(), config.hasCredentials(), preview);
		}
	}

	public boolean isConfigured() {
		synchronized (lock) {
			return config.hasCredentials() && isAllowedEndpoint(config.normalizedServerUrl());
		}
	}

	public RedeemResult redeemInviteCode(String inviteCode) {
		String safeCode = inviteCode == null ? "" : inviteCode.trim();
		if (safeCode.isBlank()) {
			return RedeemResult.failed("Invite code is empty.");
		}

		ResolvedConfig snapshot = resolvedConfig();
		if (!isAllowedEndpoint(snapshot.serverUrl())) {
			return RedeemResult.failed("Upload relay server URL is invalid or not allowed.");
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("inviteCode", safeCode);
		payload.put("installId", snapshot.installId());
		payload.put("modVersion", VersionInfo.modVersion());
		String payloadJson = GSON.toJson(payload);

		try {
			HttpRequest request = HttpRequest.newBuilder(resolveEndpoint(snapshot.serverUrl(), REDEEM_PATH))
				.timeout(Duration.ofSeconds(snapshot.requestTimeoutSeconds()))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8))
				.build();
			HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(snapshot.connectTimeoutSeconds()))
				.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return RedeemResult.failed("HTTP " + response.statusCode() + withResponseSnippet(response.body()));
			}

			JsonObject body = parseObject(response.body());
			if (body == null || !jsonBoolean(body, "ok")) {
				return RedeemResult.failed("Redeem failed: invalid server response.");
			}

			CredentialMaterial credentialMaterial = credentialMaterialFromResponse(body);
			if (!credentialMaterial.isValid()) {
				return RedeemResult.failed("Redeem failed: missing credentials in server response.");
			}

			saveCredentials(credentialMaterial);
			return RedeemResult.ok("Upload relay credentials received.");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return RedeemResult.failed("Redeem interrupted.");
		} catch (Exception e) {
			return RedeemResult.failed(messageOrFallback(e, "Invite redeem failed."));
		}
	}

	public BootstrapResult bootstrapCredentials() {
		ResolvedConfig snapshot = resolvedConfig();
		if (!isAllowedEndpoint(snapshot.serverUrl())) {
			return BootstrapResult.failed("Upload relay server URL is invalid or not allowed.");
		}

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("installId", snapshot.installId());
		payload.put("modVersion", VersionInfo.modVersion());
		payload.put("signatureVersion", snapshot.signatureVersion().isBlank() ? SIGNATURE_VERSION_V1 : snapshot.signatureVersion());
		String payloadJson = GSON.toJson(payload);

		try {
			HttpRequest request = HttpRequest.newBuilder(resolveEndpoint(snapshot.serverUrl(), BOOTSTRAP_PATH))
				.timeout(Duration.ofSeconds(snapshot.requestTimeoutSeconds()))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8))
				.build();
			HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(snapshot.connectTimeoutSeconds()))
				.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return BootstrapResult.failed("HTTP " + response.statusCode() + withResponseSnippet(response.body()));
			}

			JsonObject body = parseObject(response.body());
			if (body == null || !jsonBoolean(body, "ok")) {
				return BootstrapResult.failed("Bootstrap failed: invalid server response.");
			}
			CredentialMaterial credentialMaterial = credentialMaterialFromResponse(body);
			if (!credentialMaterial.isValid()) {
				return BootstrapResult.failed("Bootstrap failed: missing credentials in server response.");
			}

			saveCredentials(credentialMaterial);
			return BootstrapResult.ok("Upload relay credentials provisioned automatically.");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return BootstrapResult.failed("Bootstrap interrupted.");
		} catch (Exception e) {
			return BootstrapResult.failed(messageOrFallback(e, "Automatic credential bootstrap failed."));
		}
	}

	public void clearCredentials() {
		synchronized (lock) {
			config.clearCredentials();
			UploadRelayConfig.save(config);
		}
	}

	public UploadResult uploadTrainingFile(Path trainingFile) {
		return uploadTrainingFile(trainingFile, captureCurrentUploader());
	}

	public UploadResult uploadTrainingFile(Path trainingFile, UploaderContext uploaderContext) {
		if (trainingFile == null || !Files.isRegularFile(trainingFile)) {
			return UploadResult.failed("Training data file not found.");
		}

		ResolvedConfig snapshot = resolvedConfig();
		if (!isAllowedEndpoint(snapshot.serverUrl())) {
			return UploadResult.failed("Upload relay server URL is invalid or not allowed.");
		}
		if (!snapshot.hasCredentials()) {
			BootstrapResult bootstrapResult = bootstrapCredentials();
			if (!bootstrapResult.success()) {
				return UploadResult.failed("Upload relay credentials are not configured and bootstrap failed: " + bootstrapResult.detail());
			}
			snapshot = resolvedConfig();
			if (!snapshot.hasCredentials()) {
				return UploadResult.failed("Upload relay credentials are still missing after bootstrap.");
			}
		}

		try {
			UploadPayload uploadPayload = readUploadPayload(trainingFile);
			UploaderContext normalizedUploader = UploaderContext.normalized(uploaderContext, nowFormattedTimestamp());
			String timestamp = String.valueOf(Instant.now().getEpochSecond());
			String nonce = UUID.randomUUID().toString();
			String schemaVersion = "1";
			String signatureVersion = snapshot.signatureVersion().isBlank() ? SIGNATURE_VERSION_V1 : snapshot.signatureVersion();

			Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("schemaVersion", schemaVersion);
			metadata.put("modVersion", VersionInfo.modVersion());
			metadata.put("aiModelVersion", VersionInfo.aiModelVersion());
			metadata.put("playerName", safeEmbedValue(normalizedUploader.playerName()));
			metadata.put("playerUuid", safeEmbedValue(normalizedUploader.playerUuid()));
			metadata.put("clientTimestamp", safeEmbedValue(normalizedUploader.timestamp()));
			metadata.put("fileSha256", uploadPayload.sha256());
			metadata.put("fileSizeBytes", uploadPayload.fileBytes().length);

			String canonical = buildCanonicalString(
				"POST",
				UPLOAD_PATH,
				snapshot.clientId(),
				timestamp,
				nonce,
				uploadPayload.sha256(),
				String.valueOf(uploadPayload.fileBytes().length),
				schemaVersion
			);
			String signature = hmacSha256Hex(snapshot.clientSecret(), canonical);

			String boundary = "scamscreenerrelay-" + UUID.randomUUID().toString().replace("-", "");
			byte[] requestBody = buildMultipartBody(boundary, metadata, uploadPayload);
			HttpRequest request = HttpRequest.newBuilder(resolveEndpoint(snapshot.serverUrl(), UPLOAD_PATH))
				.timeout(Duration.ofSeconds(snapshot.requestTimeoutSeconds()))
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.header("X-ScamScreener-Client-Id", snapshot.clientId())
				.header("X-ScamScreener-Timestamp", timestamp)
				.header("X-ScamScreener-Nonce", nonce)
				.header("X-ScamScreener-Signature", signature)
				.header("X-ScamScreener-Signature-Version", signatureVersion)
				.POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
				.build();
			HttpClient client = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(snapshot.connectTimeoutSeconds()))
				.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				return UploadResult.failed("HTTP " + response.statusCode() + withResponseSnippet(response.body()));
			}

			JsonObject body = parseObject(response.body());
			if (body != null && !jsonBoolean(body, "ok")) {
				return UploadResult.failed("Upload relay server rejected upload." + withResponseSnippet(response.body()));
			}
			String requestId = body == null ? "" : jsonString(body, "requestId");
			String detail = "Upload relay accepted file. sha256=" + uploadPayload.sha256();
			if (!requestId.isBlank()) {
				detail = detail + " requestId=" + requestId;
			}
			return UploadResult.ok(detail);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return UploadResult.failed("Upload interrupted.");
		} catch (Exception e) {
			return UploadResult.failed(messageOrFallback(e, "Upload relay request failed."));
		}
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

	private ResolvedConfig resolvedConfig() {
		synchronized (lock) {
			config.normalizeInPlace();
			return new ResolvedConfig(
				config.normalizedServerUrl(),
				config.clientId == null ? "" : config.clientId.trim(),
				config.clientSecret == null ? "" : config.clientSecret.trim(),
				config.installId == null ? "" : config.installId.trim(),
				config.signatureVersion == null ? SIGNATURE_VERSION_V1 : config.signatureVersion.trim(),
				config.connectTimeoutSeconds,
				config.requestTimeoutSeconds
			);
		}
	}

	private static UploadPayload readUploadPayload(Path trainingFile) throws IOException {
		byte[] fileBytes = Files.readAllBytes(trainingFile);
		if (fileBytes.length <= 0) {
			throw new IOException("Training data file is empty.");
		}
		if (fileBytes.length > MAX_FILE_BYTES) {
			throw new IOException("Training data file is larger than upload limit (" + MAX_FILE_BYTES + " bytes).");
		}
		String filename = safeFileName(trainingFile);
		String sha256 = sha256Hex(fileBytes);
		return new UploadPayload(fileBytes, filename, sha256);
	}

	private static byte[] buildMultipartBody(String boundary, Map<String, Object> metadata, UploadPayload uploadPayload) throws IOException {
		String metadataJson = GSON.toJson(metadata == null ? Map.of() : metadata);
		ByteArrayOutputStream out = new ByteArrayOutputStream(uploadPayload.fileBytes().length + 2048);

		writeUtf8(out, "--" + boundary + "\r\n");
		writeUtf8(out, "Content-Disposition: form-data; name=\"metadata\"\r\n");
		writeUtf8(out, "Content-Type: application/json\r\n\r\n");
		writeUtf8(out, metadataJson + "\r\n");

		writeUtf8(out, "--" + boundary + "\r\n");
		writeUtf8(out, "Content-Disposition: form-data; name=\"training_file\"; filename=\"" + uploadPayload.filename() + "\"\r\n");
		writeUtf8(out, "Content-Type: text/csv\r\n\r\n");
		out.write(uploadPayload.fileBytes());
		writeUtf8(out, "\r\n");

		writeUtf8(out, "--" + boundary + "--\r\n");
		return out.toByteArray();
	}

	private static URI resolveEndpoint(String serverUrl, String path) {
		String base = serverUrl == null ? "" : serverUrl.trim();
		if (!base.endsWith("/")) {
			base = base + "/";
		}
		String relative = path == null ? "" : path.trim();
		if (relative.startsWith("/")) {
			relative = relative.substring(1);
		}
		return URI.create(base).resolve(relative);
	}

	private static String buildCanonicalString(
		String method,
		String path,
		String clientId,
		String timestamp,
		String nonce,
		String fileSha256,
		String fileSize,
		String schemaVersion
	) {
		return safe(method) + "\n"
			+ safe(path) + "\n"
			+ safe(clientId) + "\n"
			+ safe(timestamp) + "\n"
			+ safe(nonce) + "\n"
			+ safe(fileSha256) + "\n"
			+ safe(fileSize) + "\n"
			+ safe(schemaVersion);
	}

	private static String hmacSha256Hex(String secret, String payload) {
		try {
			Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
			mac.init(new SecretKeySpec((secret == null ? "" : secret).getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM));
			byte[] bytes = mac.doFinal((payload == null ? "" : payload).getBytes(StandardCharsets.UTF_8));
			StringBuilder out = new StringBuilder(bytes.length * 2);
			for (byte b : bytes) {
				out.append(Character.forDigit((b >>> 4) & 0xF, 16));
				out.append(Character.forDigit(b & 0xF, 16));
			}
			return out.toString();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to create HMAC signature.", e);
		}
	}

	private static boolean isAllowedEndpoint(String url) {
		if (url == null || url.isBlank()) {
			return false;
		}
		try {
			URI uri = URI.create(url.trim());
			String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
			if ("https".equals(scheme)) {
				return true;
			}
			if (!"http".equals(scheme)) {
				return false;
			}
			String host = uri.getHost();
			return "localhost".equalsIgnoreCase(host)
				|| "127.0.0.1".equals(host)
				|| "::1".equals(host);
		} catch (Exception ignored) {
			return false;
		}
	}

	private static String previewClientId(String clientId) {
		if (clientId == null || clientId.isBlank()) {
			return "not configured";
		}
		String value = clientId.trim();
		if (value.length() <= 8) {
			return value;
		}
		return value.substring(0, 8) + "...";
	}

	private static String nowFormattedTimestamp() {
		return TIMESTAMP_FORMATTER.format(LocalDateTime.now());
	}

	private static String safeEmbedValue(String value) {
		if (value == null || value.isBlank()) {
			return "unknown";
		}
		return value.trim();
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

	private static JsonObject parseObject(String body) {
		if (body == null || body.isBlank()) {
			return null;
		}
		try {
			JsonElement root = JsonParser.parseString(body);
			return root.isJsonObject() ? root.getAsJsonObject() : null;
		} catch (Exception ignored) {
			return null;
		}
	}

	private static boolean jsonBoolean(JsonObject object, String key) {
		if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
			return false;
		}
		try {
			return object.get(key).getAsBoolean();
		} catch (Exception ignored) {
			return false;
		}
	}

	private static String jsonString(JsonObject object, String key) {
		if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
			return "";
		}
		try {
			return object.get(key).getAsString().trim();
		} catch (Exception ignored) {
			return "";
		}
	}

	private static String withResponseSnippet(String responseBody) {
		String compact = compactResponse(responseBody);
		return compact.isBlank() ? "" : " - " + compact;
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

	private static String messageOrFallback(Exception error, String fallback) {
		if (error == null || error.getMessage() == null || error.getMessage().isBlank()) {
			return fallback;
		}
		return error.getMessage().trim();
	}

	private void saveCredentials(CredentialMaterial credentialMaterial) {
		if (credentialMaterial == null || !credentialMaterial.isValid()) {
			return;
		}
		synchronized (lock) {
			config.clientId = credentialMaterial.clientId();
			config.clientSecret = credentialMaterial.clientSecret();
			config.signatureVersion = credentialMaterial.signatureVersion();
			UploadRelayConfig.save(config);
		}
	}

	private static CredentialMaterial credentialMaterialFromResponse(JsonObject body) {
		String clientId = jsonString(body, "clientId");
		String clientSecret = jsonString(body, "clientSecret");
		String signatureVersion = jsonString(body, "signatureVersion");
		if (signatureVersion.isBlank()) {
			signatureVersion = SIGNATURE_VERSION_V1;
		}
		return new CredentialMaterial(clientId, clientSecret, signatureVersion);
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}

	private record UploadPayload(byte[] fileBytes, String filename, String sha256) {
	}

	private record CredentialMaterial(
		String clientId,
		String clientSecret,
		String signatureVersion
	) {
		private boolean isValid() {
			return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
		}
	}

	private record ResolvedConfig(
		String serverUrl,
		String clientId,
		String clientSecret,
		String installId,
		String signatureVersion,
		int connectTimeoutSeconds,
		int requestTimeoutSeconds
	) {
		private boolean hasCredentials() {
			return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
		}
	}

	public record RelayStatus(String serverUrl, boolean configured, String clientIdPreview) {
	}

	public record RedeemResult(boolean success, String detail) {
		public static RedeemResult ok(String detail) {
			return new RedeemResult(true, detail == null ? "" : detail);
		}

		public static RedeemResult failed(String detail) {
			return new RedeemResult(false, detail == null ? "unknown error" : detail);
		}
	}

	public record BootstrapResult(boolean success, String detail) {
		public static BootstrapResult ok(String detail) {
			return new BootstrapResult(true, detail == null ? "" : detail);
		}

		public static BootstrapResult failed(String detail) {
			return new BootstrapResult(false, detail == null ? "unknown error" : detail);
		}
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

	public record UploadResult(boolean success, String detail) {
		public static UploadResult ok(String detail) {
			return new UploadResult(true, detail == null ? "" : detail);
		}

		public static UploadResult failed(String detail) {
			return new UploadResult(false, detail == null ? "unknown error" : detail);
		}
	}
}
