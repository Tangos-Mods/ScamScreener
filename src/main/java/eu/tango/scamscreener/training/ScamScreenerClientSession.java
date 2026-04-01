package eu.tango.scamscreener.training;

import com.google.gson.Gson;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Minimal authenticated client session for the ScamScreener Training Hub API.
 */
public final class ScamScreenerClientSession {
    private static final URI DEFAULT_BASE_URI = URI.create("https://scamscreener.creepans.net");
    private static final String TRAINING_UPLOAD_FILENAME = "training-cases-v2.jsonl";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build();
    private static final Gson GSON = new Gson();

    private final URI baseUri;
    private final HttpClient httpClient;
    private final String sessionToken;
    private final Instant expiresAt;
    private final String username;

    private ScamScreenerClientSession(
        URI baseUri,
        HttpClient httpClient,
        String sessionToken,
        Instant expiresAt,
        String username
    ) {
        this.baseUri = baseUri;
        this.httpClient = httpClient;
        this.sessionToken = sessionToken;
        this.expiresAt = expiresAt;
        this.username = username;
    }

    public static CompletableFuture<ScamScreenerClientSession> loginAsync(String usernameOrEmail, String password) {
        String normalizedUsernameOrEmail = usernameOrEmail == null ? "" : usernameOrEmail.trim();
        String submittedPassword = password == null ? "" : password;
        if (normalizedUsernameOrEmail.isBlank() || submittedPassword.isBlank()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Enter your ScamScreener username/email and password."));
        }

        HttpRequest request = HttpRequest.newBuilder(DEFAULT_BASE_URI.resolve("/api/v1/client/auth/login"))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", userAgent())
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(
                GSON.toJson(new LoginRequest(normalizedUsernameOrEmail, submittedPassword)),
                StandardCharsets.UTF_8
            ))
            .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(response -> parseLoginResponse(response.statusCode(), response.body(), normalizedUsernameOrEmail));
    }

    public CompletableFuture<UploadResult> uploadTrainingDataAsync(Path trainingCasesFile) {
        if (trainingCasesFile == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Training export file is unavailable."));
        }
        if (isExpired()) {
            return CompletableFuture.failedFuture(new SessionExpiredException("Session expired. Please log in again."));
        }

        return AsyncFileWorkQueue.submitTask(() -> readTrainingPayload(trainingCasesFile))
            .thenCompose(payload -> {
                HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/v1/client/uploads"))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + sessionToken)
                    .header("Content-Type", "application/x-ndjson")
                    .header("X-ScamScreener-Filename", TRAINING_UPLOAD_FILENAME)
                    .header("User-Agent", userAgent())
                    .timeout(REQUEST_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

                return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            })
            .thenApply(response -> parseUploadResponse(response.statusCode(), response.body()));
    }

    public CompletableFuture<Void> logoutAsync() {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("/api/v1/client/auth/logout"))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer " + sessionToken)
            .header("User-Agent", userAgent())
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(response -> {
                parseLogoutResponse(response.statusCode(), response.body());
                return null;
            });
    }

    public boolean isExpired() {
        return !expiresAt.isAfter(Instant.now());
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public String username() {
        return username;
    }

    static ScamScreenerClientSession parseLoginResponse(int statusCode, String body, String fallbackUsername) {
        JsonObject json = parseJsonObject(body);
        if (statusCode == 200) {
            String responseStatus = stringValue(json, "status");
            if (!responseStatus.isBlank() && !"ok".equalsIgnoreCase(responseStatus)) {
                throw new IllegalStateException(responseMessage(json, "Login failed."));
            }

            String sessionToken = stringValue(json, "sessionToken");
            String expiresAtText = stringValue(json, "expiresAt");
            if (sessionToken.isBlank() || expiresAtText.isBlank()) {
                throw new IllegalStateException("Login response was incomplete.");
            }

            Instant expiresAt;
            try {
                expiresAt = Instant.parse(expiresAtText);
            } catch (RuntimeException exception) {
                throw new IllegalStateException("Login response did not contain a valid expiry.", exception);
            }

            JsonObject user = objectValue(json, "user");
            String username = stringValue(user, "username");
            if (username.isBlank()) {
                username = fallbackUsername == null ? "" : fallbackUsername.trim();
            }

            return new ScamScreenerClientSession(
                DEFAULT_BASE_URI,
                HTTP_CLIENT,
                sessionToken,
                expiresAt,
                username
            );
        }
        if (statusCode == 401) {
            throw new IllegalStateException(responseMessage(json, "Invalid credentials."));
        }
        if (statusCode == 403) {
            throw new IllegalStateException(responseMessage(json, "Admin accounts with required web MFA cannot use the mod upload flow."));
        }
        if (statusCode == 429) {
            throw new IllegalStateException(rateLimitMessage(json, "Too many failed login attempts. Please try again later."));
        }
        if (statusCode == 415) {
            throw new IllegalStateException(responseMessage(json, "Login request rejected by the server."));
        }

        throw new IllegalStateException(responseMessage(json, "Login failed (" + statusCode + ")."));
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
        if (statusCode == 401) {
            throw new SessionExpiredException("Session expired. Please log in again.");
        }
        if (statusCode == 400) {
            throw new UploadRejectedException(responseMessage(json, "Upload rejected by the server."));
        }
        if (statusCode == 403) {
            throw new UploadRejectedException(responseMessage(json, "This account cannot upload through the mod."));
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

    static void parseLogoutResponse(int statusCode, String body) {
        JsonObject json = parseJsonObject(body);
        if (statusCode == 200 || statusCode == 401) {
            return;
        }

        throw new IllegalStateException(responseMessage(json, "Logout failed (" + statusCode + ")."));
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

    private static JsonObject objectValue(JsonObject json, String key) {
        if (json == null || key == null || key.isBlank() || !json.has(key)) {
            return new JsonObject();
        }

        JsonElement element = json.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
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

    public static final class SessionExpiredException extends IllegalStateException {
        public SessionExpiredException(String message) {
            super(message);
        }
    }

    public static final class UploadRejectedException extends IllegalStateException {
        public UploadRejectedException(String message) {
            super(message);
        }
    }

    private record LoginRequest(
        String usernameOrEmail,
        String password
    ) {
    }
}
