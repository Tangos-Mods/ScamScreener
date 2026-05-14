package eu.tango.scamscreener.training;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.tango.scamscreener.ScamScreenerMod;
import eu.tango.scamscreener.config.store.AsyncFileWorkQueue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Minimal anonymous Training Hub upload client.
 */
public final class TrainingHubClient {
    private static final URI DEFAULT_BASE_URI = URI.create("https://scamscreener.creepans.net");
    private static final String TRAINING_UPLOAD_FILENAME = "training-cases-v2.jsonl";
    private static final String PAYLOAD_SHA256_HEADER = "X-ScamScreener-Payload-Sha256";
    private static final String HANDSHAKE_SHA256_HEADER = "X-ScamScreener-Handshake-Sha256";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build();

    private TrainingHubClient() {
    }

    public static CompletableFuture<UploadResult> uploadTrainingDataAsync(String trainingClientId, Path trainingCasesFile) {
        return uploadTrainingDataAsync(DEFAULT_BASE_URI, trainingClientId, trainingCasesFile);
    }

    static CompletableFuture<UploadResult> uploadTrainingDataAsync(URI baseUri, String trainingClientId, Path trainingCasesFile) {
        String normalizedTrainingClientId = normalizeTrainingClientId(trainingClientId);
        if (normalizedTrainingClientId.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Training client ID is unavailable."));
        }
        if (trainingCasesFile == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Training export file is unavailable."));
        }

        return AsyncFileWorkQueue.submitTask(() -> readTrainingPayload(trainingCasesFile))
            .thenCompose(payload -> {
                UploadHandshake handshake = createUploadHandshake(normalizedTrainingClientId, payload);
                HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/v1/client/uploads/anonymous"))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-ndjson")
                    .header("X-ScamScreener-Filename", TRAINING_UPLOAD_FILENAME)
                    .header("X-ScamScreener-Client-Id", normalizedTrainingClientId)
                    .header(PAYLOAD_SHA256_HEADER, handshake.payloadSha256())
                    .header(HANDSHAKE_SHA256_HEADER, handshake.handshakeSha256())
                    .header("User-Agent", userAgent())
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

                return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            })
            .thenApply(response -> parseUploadResponse(response.statusCode(), response.body()));
    }

    static UploadResult parseUploadResponse(int statusCode, String body) {
        JsonObject json = parseJsonObject(body);
        String responseStatus = stringValue(json, "status").toLowerCase(Locale.ROOT);
        if ((statusCode == 200 || statusCode == 201 || statusCode == 429)
            && ("accepted".equals(responseStatus) || "duplicate".equals(responseStatus) || "quota-exceeded".equals(responseStatus))) {
            return new UploadResult(
                responseStatus,
                longValue(json, "uploadId"),
                intValue(json, "caseCount"),
                intValue(json, "insertedCases"),
                intValue(json, "updatedCases"),
                stringValue(json, "detail"),
                stringValue(json, "sha256")
            );
        }
        if (statusCode == 400) {
            throw new UploadRejectedException(responseMessage(json, "Upload rejected by the server."));
        }
        if (statusCode == 401 || statusCode == 403) {
            throw new UploadRejectedException(responseMessage(json, "Anonymous upload is not available."));
        }
        if (statusCode == 413) {
            throw new UploadRejectedException("Upload rejected because the file is too large.");
        }
        if (statusCode == 415) {
            throw new UploadRejectedException("Upload rejected because the content type was invalid.");
        }
        if (statusCode == 429) {
            throw new IllegalStateException(rateLimitMessage(json, "Upload rate limited. Please try again later."));
        }

        throw new IllegalStateException(responseMessage(json, "Upload failed (" + statusCode + ")."));
    }

    private static String readTrainingPayload(Path trainingCasesFile) {
        try {
            String payload = Files.readString(trainingCasesFile, StandardCharsets.UTF_8);
            if (payload.isBlank()) {
                throw new IllegalStateException("No reviewed SAFE/RISK cases are available for upload.");
            }

            return payload;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read training export from " + trainingCasesFile + ".", exception);
        }
    }

    static UploadHandshake createUploadHandshake(String normalizedTrainingClientId, String payload) {
        if (normalizedTrainingClientId == null || normalizedTrainingClientId.isBlank()) {
            throw new IllegalStateException("Training client ID is unavailable.");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalStateException("Training payload is unavailable.");
        }

        String payloadSha256 = sha256Hex(payload);
        String handshakeSha256 = sha256Hex(normalizedTrainingClientId + ":" + payloadSha256);
        return new UploadHandshake(payloadSha256, handshakeSha256);
    }

    private static String normalizeTrainingClientId(String trainingClientId) {
        if (trainingClientId == null) {
            return "";
        }

        return trainingClientId.trim().toLowerCase(Locale.ROOT);
    }

    private static String responseMessage(JsonObject json, String fallback) {
        String detail = stringValue(json, "detail");
        if (!detail.isBlank()) {
            return detail;
        }

        String message = stringValue(json, "message");
        if (!message.isBlank()) {
            return message;
        }

        String status = stringValue(json, "status");
        if (!status.isBlank()) {
            return status;
        }

        return fallback;
    }

    private static String rateLimitMessage(JsonObject json, String fallback) {
        String message = responseMessage(json, fallback);
        long retryAfterSeconds = longValue(json, "retryAfter");
        if (retryAfterSeconds <= 0L) {
            return message;
        }

        return message + " Retry after " + retryAfterSeconds + "s.";
    }

    private static JsonObject parseJsonObject(String body) {
        if (body == null || body.isBlank()) {
            return new JsonObject();
        }

        try {
            JsonElement element = JsonParser.parseString(body);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException ignored) {
            return new JsonObject();
        }
    }

    private static String stringValue(JsonObject json, String key) {
        if (json == null || key == null || key.isBlank() || !json.has(key)) {
            return "";
        }

        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }

        try {
            String value = element.getAsString();
            return value == null ? "" : value.trim();
        } catch (UnsupportedOperationException | ClassCastException | IllegalStateException ignored) {
            return "";
        }
    }

    private static int intValue(JsonObject json, String key) {
        long value = longValue(json, key);
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }

    private static long longValue(JsonObject json, String key) {
        if (json == null || key == null || key.isBlank() || !json.has(key)) {
            return 0L;
        }

        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return 0L;
        }

        try {
            return element.getAsLong();
        } catch (NumberFormatException | UnsupportedOperationException | ClassCastException | IllegalStateException ignored) {
            return 0L;
        }
    }

    private static String userAgent() {
        return "ScamScreener/" + ScamScreenerMod.VERSION + "+" + ScamScreenerMod.MINECRAFT;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    public record UploadResult(
        String status,
        long uploadId,
        int caseCount,
        int insertedCases,
        int updatedCases,
        String detail,
        String sha256
    ) {
    }

    public static final class UploadRejectedException extends IllegalStateException {
        public UploadRejectedException(String message) {
            super(message);
        }
    }

    record UploadHandshake(
        String payloadSha256,
        String handshakeSha256
    ) {
    }
}
