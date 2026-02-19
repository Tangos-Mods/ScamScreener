package eu.tango.scamscreener.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class UploadRelayConfig {
	public static final String DEFAULT_SERVER_URL = "https://scamscreener.creepans.net";
	private static final String LEGACY_DEFAULT_SERVER_URL = "https://upload.scamscreener.net";
	public static final String DEFAULT_SIGNATURE_VERSION = "v1";
	public static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 8;
	public static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 25;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE_PATH = ScamScreenerPaths.inModConfigDir("scam-screener-upload-relay.json");

	public String serverUrl = DEFAULT_SERVER_URL;
	public String clientId = "";
	public String clientSecret = "";
	public String installId = UUID.randomUUID().toString();
	public String signatureVersion = DEFAULT_SIGNATURE_VERSION;
	public int connectTimeoutSeconds = DEFAULT_CONNECT_TIMEOUT_SECONDS;
	public int requestTimeoutSeconds = DEFAULT_REQUEST_TIMEOUT_SECONDS;

	public static UploadRelayConfig loadOrCreate() {
		if (!Files.exists(FILE_PATH)) {
			UploadRelayConfig defaults = new UploadRelayConfig();
			save(defaults);
			return defaults;
		}

		UploadRelayConfig loaded = loadFromPath(FILE_PATH);
		if (loaded == null) {
			loaded = new UploadRelayConfig();
		}
		loaded.normalizeInPlace();
		save(loaded);
		return loaded;
	}

	private static UploadRelayConfig loadFromPath(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, UploadRelayConfig.class);
		} catch (IOException ignored) {
			return null;
		}
	}

	public static void save(UploadRelayConfig config) {
		UploadRelayConfig value = config == null ? new UploadRelayConfig() : config;
		value.normalizeInPlace();
		try {
			Files.createDirectories(FILE_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE_PATH, StandardCharsets.UTF_8)) {
				GSON.toJson(value, writer);
			}
		} catch (IOException ignored) {
		}
	}

	public boolean hasCredentials() {
		return !safeTrim(clientId).isEmpty() && !safeTrim(clientSecret).isEmpty();
	}

	public void clearCredentials() {
		clientId = "";
		clientSecret = "";
	}

	public String normalizedServerUrl() {
		String normalized = safeTrim(serverUrl);
		if (normalized.isEmpty()) {
			return DEFAULT_SERVER_URL;
		}
		if (isLegacyDefaultServerUrl(normalized)) {
			return DEFAULT_SERVER_URL;
		}
		return normalized;
	}

	public UploadRelayConfig normalizeInPlace() {
		serverUrl = normalizedServerUrl();
		clientId = safeTrim(clientId);
		clientSecret = safeTrim(clientSecret);
		signatureVersion = safeTrim(signatureVersion);
		if (signatureVersion.isEmpty()) {
			signatureVersion = DEFAULT_SIGNATURE_VERSION;
		}
		if (!isLikelyUuid(installId)) {
			installId = UUID.randomUUID().toString();
		} else {
			installId = safeTrim(installId);
		}
		connectTimeoutSeconds = clamp(connectTimeoutSeconds, 1, 120, DEFAULT_CONNECT_TIMEOUT_SECONDS);
		requestTimeoutSeconds = clamp(requestTimeoutSeconds, 1, 120, DEFAULT_REQUEST_TIMEOUT_SECONDS);
		return this;
	}

	private static String safeTrim(String value) {
		return value == null ? "" : value.trim();
	}

	private static int clamp(int value, int min, int max, int fallback) {
		if (value < min || value > max) {
			return fallback;
		}
		return value;
	}

	private static boolean isLikelyUuid(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}
		try {
			UUID.fromString(value.trim());
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}

	private static boolean isLegacyDefaultServerUrl(String value) {
		if (value == null || value.isBlank()) {
			return false;
		}
		String normalized = value.trim();
		if (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return LEGACY_DEFAULT_SERVER_URL.equalsIgnoreCase(normalized);
	}
}
