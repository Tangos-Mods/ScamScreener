package eu.tango.scamscreener.lookup;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class MojangProfileService {
	private static final Logger LOGGER = LoggerFactory.getLogger(MojangProfileService.class);
	private static final Gson GSON = new Gson();
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
	private static final Duration LOOKUP_TIMEOUT = Duration.ofSeconds(4);
	private static final Duration CACHE_TTL = Duration.ofMinutes(30);

	private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
	private final Map<String, CompletableFuture<ResolvedTarget>> pendingLookups = new ConcurrentHashMap<>();
	private final Map<UUID, CacheEntry> uuidCache = new ConcurrentHashMap<>();
	private final Map<UUID, CompletableFuture<ResolvedTarget>> pendingUuidLookups = new ConcurrentHashMap<>();

	public ResolvedTarget lookupCached(String input) {
		String key = normalizeKey(input);
		if (key == null) {
			return null;
		}
		CacheEntry cached = cache.get(key);
		if (isExpired(cached)) {
			cache.remove(key);
			return null;
		}
		return cached.target();
	}

	public CompletableFuture<ResolvedTarget> lookupAsync(String input) {
		String key = normalizeKey(input);
		if (key == null) {
			return CompletableFuture.completedFuture(null);
		}

		ResolvedTarget cached = lookupCached(key);
		if (cached != null) {
			return CompletableFuture.completedFuture(cached);
		}

		return pendingLookups.computeIfAbsent(key, this::fetchAsync);
	}

	public ResolvedTarget lookupByUuidCached(UUID uuid) {
		if (uuid == null) {
			return null;
		}
		CacheEntry cached = uuidCache.get(uuid);
		if (isExpired(cached)) {
			uuidCache.remove(uuid);
			return null;
		}
		return cached.target();
	}

	public CompletableFuture<ResolvedTarget> lookupByUuidAsync(UUID uuid) {
		if (uuid == null) {
			return CompletableFuture.completedFuture(null);
		}
		ResolvedTarget cached = lookupByUuidCached(uuid);
		if (cached != null) {
			return CompletableFuture.completedFuture(cached);
		}
		return pendingUuidLookups.computeIfAbsent(uuid, this::fetchByUuidAsync);
	}

	private CompletableFuture<ResolvedTarget> fetchAsync(String key) {
		try {
			String encodedName = URLEncoder.encode(key, StandardCharsets.UTF_8);
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + encodedName))
				.timeout(LOOKUP_TIMEOUT)
				.GET()
				.build();

			return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
				.thenApply(this::parseResponse)
				.exceptionally(error -> {
					LOGGER.debug("Failed Mojang profile lookup for {}", key, error);
					return null;
				})
				.whenComplete((result, error) -> {
					pendingLookups.remove(key);
					if (result != null) {
						cache.put(key, new CacheEntry(result, Instant.now().plus(CACHE_TTL)));
						uuidCache.put(result.uuid(), new CacheEntry(result, Instant.now().plus(CACHE_TTL)));
					}
				});
		} catch (Exception e) {
			LOGGER.debug("Failed to create Mojang profile request for {}", key, e);
			return CompletableFuture.completedFuture(null);
		}
	}

	private CompletableFuture<ResolvedTarget> fetchByUuidAsync(UUID uuid) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + undashed(uuid)))
				.timeout(LOOKUP_TIMEOUT)
				.GET()
				.build();

			return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
				.thenApply(this::parseResponse)
				.exceptionally(error -> {
					LOGGER.debug("Failed Mojang profile lookup for UUID {}", uuid, error);
					return null;
				})
				.whenComplete((result, error) -> {
					pendingUuidLookups.remove(uuid);
					if (result != null) {
						Instant expiresAt = Instant.now().plus(CACHE_TTL);
						uuidCache.put(uuid, new CacheEntry(result, expiresAt));
						cache.put(normalizeKey(result.name()), new CacheEntry(result, expiresAt));
					}
				});
		} catch (Exception e) {
			LOGGER.debug("Failed to create Mojang UUID profile request for {}", uuid, e);
			return CompletableFuture.completedFuture(null);
		}
	}

	private ResolvedTarget parseResponse(HttpResponse<String> response) {
		if (response.statusCode() != 200) {
			return null;
		}

		MojangProfile profile = GSON.fromJson(response.body(), MojangProfile.class);
		if (profile == null || profile.id == null || profile.id.length() != 32 || profile.name == null || profile.name.isBlank()) {
			return null;
		}

		UUID uuid = uuidFromUndashed(profile.id);
		return uuid == null ? null : new ResolvedTarget(uuid, profile.name);
	}

	private static String normalizeKey(String input) {
		if (input == null) {
			return null;
		}

		String trimmed = input.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		return trimmed.toLowerCase(Locale.ROOT);
	}

	private static String undashed(UUID uuid) {
		return uuid.toString().replace("-", "");
	}

	private static boolean isExpired(CacheEntry cached) {
		return cached == null || cached.expiresAt().isBefore(Instant.now());
	}

	private static UUID uuidFromUndashed(String undashed) {
		String dashed = undashed.replaceFirst(
			"([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
			"$1-$2-$3-$4-$5"
		);
		try {
			return UUID.fromString(dashed);
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private record CacheEntry(ResolvedTarget target, Instant expiresAt) {
	}

	private static final class MojangProfile {
		String id;
		String name;
	}
}
