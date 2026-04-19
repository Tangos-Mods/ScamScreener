package eu.tango.scamscreener.lists;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.tango.scamscreener.ScamScreenerMod;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Small Mojang profile lookup used to enrich blacklist entries with UUIDs.
 */
public final class PlayerUuidLookup {
    private static final URI LOOKUP_BASE_URI = URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/name/");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build();

    private PlayerUuidLookup() {
    }

    /**
     * Resolves one Minecraft player UUID from the current Mojang lookup endpoint.
     *
     * @param playerName the player name to resolve
     * @return a future completed with the matching UUID, or {@code null} when unavailable
     */
    public static CompletableFuture<UUID> resolveByNameAsync(String playerName) {
        String normalizedName = playerName == null ? "" : playerName.trim();
        if (normalizedName.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        HttpRequest request = HttpRequest.newBuilder(buildLookupUri(normalizedName))
            .header("Accept", "application/json")
            .header("User-Agent", userAgent())
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(response -> parsePlayerUuid(response.statusCode(), response.body()))
            .exceptionally(throwable -> {
                ScamScreenerMod.LOGGER.debug("Could not resolve UUID for blacklist entry {}.", normalizedName, throwable);
                return null;
            });
    }

    static UUID parsePlayerUuid(int statusCode, String body) {
        if (statusCode != 200) {
            return null;
        }

        JsonObject json = parseJsonObject(body);
        return parseUuid(stringValue(json, "id"));
    }

    static UUID parseUuid(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String compactUuid = rawValue.trim().replace("-", "");
        if (compactUuid.length() != 32) {
            return null;
        }

        String formattedUuid = compactUuid.substring(0, 8)
            + "-" + compactUuid.substring(8, 12)
            + "-" + compactUuid.substring(12, 16)
            + "-" + compactUuid.substring(16, 20)
            + "-" + compactUuid.substring(20);
        try {
            return UUID.fromString(formattedUuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static URI buildLookupUri(String playerName) {
        return LOOKUP_BASE_URI.resolve(URLEncoder.encode(playerName, StandardCharsets.UTF_8));
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

    private static String userAgent() {
        return "ScamScreener/" + ScamScreenerMod.VERSION + "+" + ScamScreenerMod.MINECRAFT;
    }
}
