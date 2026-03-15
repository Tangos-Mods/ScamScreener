package eu.tango.scamscreener.update;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import eu.tango.scamscreener.ScamScreenerMod;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Lightweight Modrinth update checker used for join-time notifications.
 */
public final class ModrinthUpdateChecker {
    private static final String MODRINTH_PROJECT_ID = "XTB0bgAW";
    private static final String MODRINTH_PROJECT_URL = "https://modrinth.com/project/" + MODRINTH_PROJECT_ID;
    private static final String RELEASE_CHANNEL = "release";
    private static final long MIN_CHECK_INTERVAL_MS = 5L * 60L * 1000L;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build();
    private static final Gson GSON = new Gson();
    private static final Object LOCK = new Object();

    private static volatile boolean checkInProgress;
    private static volatile long lastCheckFinishedAtMs;
    private static volatile UpdateInfo cachedUpdate;

    private ModrinthUpdateChecker() {
    }

    /**
     * Checks Modrinth for a newer listed release for the current Fabric target.
     *
     * @param onUpdateAvailable callback invoked when a newer version is found
     */
    public static void checkOnJoin(Consumer<UpdateInfo> onUpdateAvailable) {
        if (onUpdateAvailable == null || FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return;
        }

        long now = System.currentTimeMillis();
        UpdateInfo cached = cachedUpdate;
        if (cached != null && now - lastCheckFinishedAtMs < MIN_CHECK_INTERVAL_MS) {
            onUpdateAvailable.accept(cached);
            return;
        }
        if (now - lastCheckFinishedAtMs < MIN_CHECK_INTERVAL_MS) {
            return;
        }

        synchronized (LOCK) {
            if (checkInProgress) {
                return;
            }
            if (cachedUpdate != null && now - lastCheckFinishedAtMs < MIN_CHECK_INTERVAL_MS) {
                onUpdateAvailable.accept(cachedUpdate);
                return;
            }

            checkInProgress = true;
        }

        fetchLatestUpdateAsync()
            .whenComplete((updateInfo, throwable) -> {
                synchronized (LOCK) {
                    checkInProgress = false;
                    lastCheckFinishedAtMs = System.currentTimeMillis();
                    cachedUpdate = throwable == null ? updateInfo : null;
                }

                if (throwable != null) {
                    ScamScreenerMod.LOGGER.debug("Modrinth update check failed.", throwable);
                    return;
                }
                if (updateInfo != null) {
                    onUpdateAvailable.accept(updateInfo);
                }
            });
    }

    private static CompletableFuture<UpdateInfo> fetchLatestUpdateAsync() {
        String currentVersion = currentVersion();
        HttpRequest request = HttpRequest.newBuilder(buildVersionsUri())
            .header("Accept", "application/json")
            .header("User-Agent", "ScamScreener/" + currentVersion)
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .thenApply(response -> parseLatestUpdate(response, currentVersion));
    }

    private static URI buildVersionsUri() {
        String loaders = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
        String gameVersions = URLEncoder.encode("[\"" + ScamScreenerMod.MINECRAFT + "\"]", StandardCharsets.UTF_8);
        return URI.create(
            "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT_ID
                + "/version?loaders=" + loaders
                + "&game_versions=" + gameVersions
                + "&include_changelog=true"
        );
    }

    private static UpdateInfo parseLatestUpdate(HttpResponse<String> response, String currentVersion) {
        if (response == null || response.statusCode() / 100 != 2 || response.body() == null || response.body().isBlank()) {
            return null;
        }

        ModrinthVersion[] versions = GSON.fromJson(response.body(), ModrinthVersion[].class);
        if (versions == null || versions.length == 0) {
            return null;
        }

        ModrinthVersion latestVersion = Arrays.stream(versions)
            .filter(ModrinthUpdateChecker::isEligibleVersion)
            .max(Comparator.comparing(ModrinthUpdateChecker::publishedAt))
            .orElse(null);
        if (latestVersion == null) {
            return null;
        }

        String latestVersionNumber = normalizeVersion(latestVersion.versionNumber);
        if (latestVersionNumber.isBlank() || latestVersionNumber.equals(normalizeVersion(currentVersion))) {
            return null;
        }

        return new UpdateInfo(
            currentVersion,
            latestVersionNumber,
            MODRINTH_PROJECT_URL,
            latestVersion.changelog == null ? "" : latestVersion.changelog
        );
    }

    private static boolean isEligibleVersion(ModrinthVersion version) {
        if (version == null) {
            return false;
        }

        String versionNumber = normalizeVersion(version.versionNumber);
        if (versionNumber.isBlank()) {
            return false;
        }

        String versionType = version.versionType == null ? "" : version.versionType.trim();
        if (!versionType.isBlank() && !RELEASE_CHANNEL.equalsIgnoreCase(versionType)) {
            return false;
        }

        String status = version.status == null ? "" : version.status.trim();
        return status.isBlank() || "listed".equalsIgnoreCase(status);
    }

    private static Instant publishedAt(ModrinthVersion version) {
        if (version == null || version.datePublished == null || version.datePublished.isBlank()) {
            return Instant.EPOCH;
        }

        try {
            return Instant.parse(version.datePublished);
        } catch (RuntimeException exception) {
            return Instant.EPOCH;
        }
    }

    private static String currentVersion() {
        return FabricLoader.getInstance()
            .getModContainer("scamscreener")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse(ScamScreenerMod.VERSION + "+" + ScamScreenerMod.MINECRAFT);
    }

    private static String normalizeVersion(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    private static final class ModrinthVersion {
        @SerializedName("version_number")
        private String versionNumber;
        @SerializedName("version_type")
        private String versionType;
        @SerializedName("date_published")
        private String datePublished;
        private String status;
        private String changelog;
    }

    public record UpdateInfo(
        String currentVersion,
        String latestVersion,
        String modrinthUrl,
        String changelog
    ) {
    }
}
