package eu.tango.scamscreener.discord;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import eu.tango.scamscreener.config.UploadRelayConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadRelayContractTest {
	private HttpServer server;

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
			server = null;
		}
	}

	@Test
	void redeemInviteCodeStoresCredentialsInSeparateConfig() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/v1/client/redeem", exchange -> {
			try {
				assertEquals("POST", exchange.getRequestMethod());
				String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
				JsonObject request = JsonParser.parseString(body).getAsJsonObject();
				assertEquals("invite-123", request.get("inviteCode").getAsString());
				assertNotNull(request.get("installId").getAsString());
				sendJson(exchange, 200, """
					{
					  "ok": true,
					  "clientId": "relay-client-a",
					  "clientSecret": "relay-secret-a",
					  "signatureVersion": "v1"
					}
					""");
			} catch (Throwable error) {
				sendJson(exchange, 500, "{\"ok\":false,\"message\":\"" + safe(error.getMessage()) + "\"}");
			}
		});
		server.start();

		UploadRelayConfig config = new UploadRelayConfig();
		config.serverUrl = "http://127.0.0.1:" + server.getAddress().getPort();
		config.clientId = "";
		config.clientSecret = "";
		config.installId = UUID.randomUUID().toString();
		UploadRelayClient client = new UploadRelayClient(config);

		UploadRelayClient.RedeemResult result = client.redeemInviteCode("invite-123");
		assertTrue(result.success(), result::detail);
		assertEquals("relay-client-a", config.clientId);
		assertEquals("relay-secret-a", config.clientSecret);
		assertEquals("v1", config.signatureVersion);
	}

	@Test
	void uploadTrainingFileSendsSignedMultipartRequest() throws Exception {
		AtomicReference<Throwable> handlerError = new AtomicReference<>();
		String clientId = "relay-client-1";
		String secret = "relay-secret-1";

		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/v1/training-uploads", exchange -> {
			try {
				assertEquals("POST", exchange.getRequestMethod());
				assertEquals(clientId, exchange.getRequestHeaders().getFirst("X-ScamScreener-Client-Id"));
				String timestamp = exchange.getRequestHeaders().getFirst("X-ScamScreener-Timestamp");
				String nonce = exchange.getRequestHeaders().getFirst("X-ScamScreener-Nonce");
				String signature = exchange.getRequestHeaders().getFirst("X-ScamScreener-Signature");
				String signatureVersion = exchange.getRequestHeaders().getFirst("X-ScamScreener-Signature-Version");
				assertEquals("v1", signatureVersion);
				assertNotNull(timestamp);
				assertNotNull(nonce);
				assertNotNull(signature);

				String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
				String boundary = boundaryFromContentType(contentType);
				byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
				String body = new String(bodyBytes, StandardCharsets.UTF_8);
				assertTrue(body.contains("name=\"metadata\""));
				assertTrue(body.contains("name=\"training_file\""));

				String metadataJson = extractPartBody(body, boundary, "metadata");
				JsonObject metadata = JsonParser.parseString(metadataJson).getAsJsonObject();
				String fileSha256 = metadata.get("fileSha256").getAsString();
				String fileSize = metadata.get("fileSizeBytes").getAsString();
				String schemaVersion = metadata.get("schemaVersion").getAsString();

				String expectedCanonical = "POST\n/api/v1/training-uploads\n"
					+ clientId + "\n"
					+ timestamp + "\n"
					+ nonce + "\n"
					+ fileSha256 + "\n"
					+ fileSize + "\n"
					+ schemaVersion;
				String expectedSignature = hmacSha256Hex(secret, expectedCanonical);
				assertEquals(expectedSignature, signature);

				sendJson(exchange, 200, """
					{
					  "ok": true,
					  "requestId": "req-123",
					  "discordMessageId": "disc-123"
					}
					""");
			} catch (Throwable error) {
				handlerError.set(error);
				sendJson(exchange, 500, "{\"ok\":false,\"message\":\"" + safe(error.getMessage()) + "\"}");
			}
		});
		server.start();

		UploadRelayConfig config = new UploadRelayConfig();
		config.serverUrl = "http://127.0.0.1:" + server.getAddress().getPort();
		config.clientId = clientId;
		config.clientSecret = secret;
		config.signatureVersion = "v1";
		UploadRelayClient client = new UploadRelayClient(config);

		Path file = Files.createTempFile("relay-contract-", ".csv.old.1");
		try {
			Files.writeString(file, "label,message\n1,relay contract test\n", StandardCharsets.UTF_8);
			UploadRelayClient.UploadResult result = client.uploadTrainingFile(file, new UploadRelayClient.UploaderContext(
				"contract-player",
				"00000000-0000-0000-0000-000000000001",
				"19.02.2026 22:10:00"
			));
			assertTrue(result.success(), result::detail);
			assertTrue(result.detail().contains("sha256="));
			assertNull(handlerError.get(), () -> safe(handlerError.get().getMessage()));
		} finally {
			Files.deleteIfExists(file);
		}
	}

	@Test
	void uploadTrainingFileBootstrapsCredentialsWhenMissing() throws Exception {
		AtomicReference<Throwable> handlerError = new AtomicReference<>();
		AtomicReference<String> bootstrapInstallId = new AtomicReference<>();
		String clientId = "relay-bootstrap-client";
		String secret = "relay-bootstrap-secret";

		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/v1/client/bootstrap", exchange -> {
			try {
				assertEquals("POST", exchange.getRequestMethod());
				String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
				JsonObject request = JsonParser.parseString(body).getAsJsonObject();
				String installId = request.get("installId").getAsString();
				assertNotNull(installId);
				assertTrue(!installId.isBlank());
				bootstrapInstallId.set(installId);
				sendJson(exchange, 200, """
					{
					  "ok": true,
					  "clientId": "relay-bootstrap-client",
					  "clientSecret": "relay-bootstrap-secret",
					  "signatureVersion": "v1"
					}
					""");
			} catch (Throwable error) {
				handlerError.set(error);
				sendJson(exchange, 500, "{\"ok\":false,\"message\":\"" + safe(error.getMessage()) + "\"}");
			}
		});
		server.createContext("/api/v1/training-uploads", exchange -> {
			try {
				assertEquals("POST", exchange.getRequestMethod());
				assertEquals(clientId, exchange.getRequestHeaders().getFirst("X-ScamScreener-Client-Id"));
				String timestamp = exchange.getRequestHeaders().getFirst("X-ScamScreener-Timestamp");
				String nonce = exchange.getRequestHeaders().getFirst("X-ScamScreener-Nonce");
				String signature = exchange.getRequestHeaders().getFirst("X-ScamScreener-Signature");
				assertNotNull(timestamp);
				assertNotNull(nonce);
				assertNotNull(signature);

				String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
				String boundary = boundaryFromContentType(contentType);
				String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
				String metadataJson = extractPartBody(body, boundary, "metadata");
				JsonObject metadata = JsonParser.parseString(metadataJson).getAsJsonObject();
				String fileSha256 = metadata.get("fileSha256").getAsString();
				String fileSize = metadata.get("fileSizeBytes").getAsString();
				String schemaVersion = metadata.get("schemaVersion").getAsString();
				String expectedCanonical = "POST\n/api/v1/training-uploads\n"
					+ clientId + "\n"
					+ timestamp + "\n"
					+ nonce + "\n"
					+ fileSha256 + "\n"
					+ fileSize + "\n"
					+ schemaVersion;
				assertEquals(hmacSha256Hex(secret, expectedCanonical), signature);
				sendJson(exchange, 200, "{\"ok\":true,\"requestId\":\"req-bootstrap\"}");
			} catch (Throwable error) {
				handlerError.set(error);
				sendJson(exchange, 500, "{\"ok\":false,\"message\":\"" + safe(error.getMessage()) + "\"}");
			}
		});
		server.start();

		UploadRelayConfig config = new UploadRelayConfig();
		config.serverUrl = "http://127.0.0.1:" + server.getAddress().getPort();
		config.clientId = "";
		config.clientSecret = "";
		config.signatureVersion = "v1";
		config.installId = UUID.randomUUID().toString();
		UploadRelayClient client = new UploadRelayClient(config);

		Path file = Files.createTempFile("relay-bootstrap-", ".csv.old.1");
		try {
			Files.writeString(file, "label,message\n1,relay bootstrap test\n", StandardCharsets.UTF_8);
			UploadRelayClient.UploadResult result = client.uploadTrainingFile(file, new UploadRelayClient.UploaderContext(
				"bootstrap-player",
				"00000000-0000-0000-0000-000000000001",
				"19.02.2026 22:10:00"
			));
			assertTrue(result.success(), result::detail);
			assertEquals(clientId, config.clientId);
			assertEquals(secret, config.clientSecret);
			assertTrue(bootstrapInstallId.get() != null && !bootstrapInstallId.get().isBlank());
			assertNull(handlerError.get(), () -> safe(handlerError.get().getMessage()));
		} finally {
			Files.deleteIfExists(file);
		}
	}

	@Test
	void uploadTrainingFileReturnsHttpFailureDetails() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/api/v1/training-uploads", exchange ->
			sendJson(exchange, 401, "{\"ok\":false,\"errorCode\":\"AUTH_FAILED\",\"message\":\"signature mismatch\"}"));
		server.start();

		UploadRelayConfig config = new UploadRelayConfig();
		config.serverUrl = "http://127.0.0.1:" + server.getAddress().getPort();
		config.clientId = "relay-client";
		config.clientSecret = "relay-secret";
		config.signatureVersion = "v1";
		UploadRelayClient client = new UploadRelayClient(config);

		Path file = Files.createTempFile("relay-contract-fail-", ".csv.old.1");
		try {
			Files.writeString(file, "label,message\n1,relay fail case\n", StandardCharsets.UTF_8);
			UploadRelayClient.UploadResult result = client.uploadTrainingFile(file, new UploadRelayClient.UploaderContext(
				"contract-player",
				"00000000-0000-0000-0000-000000000001",
				"19.02.2026 22:10:00"
			));
			assertTrue(!result.success());
			assertTrue(result.detail().contains("HTTP 401"), result::detail);
		} finally {
			Files.deleteIfExists(file);
		}
	}

	private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
		byte[] body = (json == null ? "" : json).getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, body.length);
		exchange.getResponseBody().write(body);
		exchange.close();
	}

	private static String boundaryFromContentType(String contentType) {
		if (contentType == null) {
			return "";
		}
		for (String part : contentType.split(";")) {
			String trimmed = part.trim();
			if (trimmed.toLowerCase(Locale.ROOT).startsWith("boundary=")) {
				String value = trimmed.substring("boundary=".length()).trim();
				if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
					return value.substring(1, value.length() - 1);
				}
				return value;
			}
		}
		return "";
	}

	private static String extractPartBody(String fullBody, String boundary, String partName) {
		String marker = "name=\"" + partName + "\"";
		int markerIndex = fullBody.indexOf(marker);
		if (markerIndex < 0) {
			return "";
		}
		int bodyStart = fullBody.indexOf("\r\n\r\n", markerIndex);
		if (bodyStart < 0) {
			return "";
		}
		bodyStart += 4;
		String boundaryToken = "\r\n--" + boundary;
		int bodyEnd = fullBody.indexOf(boundaryToken, bodyStart);
		if (bodyEnd < 0) {
			bodyEnd = fullBody.length();
		}
		return fullBody.substring(bodyStart, bodyEnd);
	}

	private static String hmacSha256Hex(String secret, String payload) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec((secret == null ? "" : secret).getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			byte[] bytes = mac.doFinal((payload == null ? "" : payload).getBytes(StandardCharsets.UTF_8));
			StringBuilder out = new StringBuilder(bytes.length * 2);
			for (byte b : bytes) {
				out.append(Character.forDigit((b >>> 4) & 0xF, 16));
				out.append(Character.forDigit(b & 0xF, 16));
			}
			return out.toString();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to calculate HMAC.", e);
		}
	}

	private static String safe(String value) {
		return value == null ? "" : value.replace('"', '\'');
	}
}
