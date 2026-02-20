package eu.tango.scamscreener.market;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HypixelMarketApiClient {
	private static final String HYPIXEL_API_BASE = "https://api.hypixel.net";
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(REQUEST_TIMEOUT)
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	public List<BazaarRepository.BazaarSnapshot> fetchBazaarSnapshots(long nowMs) throws IOException {
		JsonObject root = getJsonObject(HYPIXEL_API_BASE + "/v2/skyblock/bazaar");
		JsonObject products = object(root, "products");
		if (products == null) {
			return List.of();
		}

		List<BazaarRepository.BazaarSnapshot> out = new ArrayList<>();
		for (var entry : products.entrySet()) {
			String productId = entry.getKey();
			JsonObject product = object(entry.getValue());
			if (product == null) {
				continue;
			}
			JsonObject quick = object(product, "quick_status");
			if (quick == null) {
				continue;
			}
			double buyPrice = number(quick, "buyPrice", 0.0);
			double sellPrice = number(quick, "sellPrice", 0.0);
			double buyVolume = number(quick, "buyVolume", 0.0);
			double sellVolume = number(quick, "sellVolume", 0.0);
			if (buyPrice <= 0.0 && sellPrice <= 0.0) {
				continue;
			}
			out.add(new BazaarRepository.BazaarSnapshot(
				MarketItemKey.normalize(productId),
				nowMs,
				buyPrice,
				sellPrice,
				buyVolume,
				sellVolume
			));
		}
		return out;
	}

	public AuctionPage fetchAuctionsPage(int page, long nowMs) throws IOException {
		int safePage = Math.max(0, page);
		JsonObject root = getJsonObject(HYPIXEL_API_BASE + "/v2/skyblock/auctions?page=" + safePage);
		int totalPages = integer(root, "totalPages", 0);
		JsonArray auctions = array(root, "auctions");
		if (auctions == null) {
			return new AuctionPage(safePage, totalPages, List.of());
		}

		List<AuctionEntry> out = new ArrayList<>();
		for (JsonElement raw : auctions) {
			JsonObject auction = object(raw);
			if (auction == null || !bool(auction, "bin", false)) {
				continue;
			}
			String auctionUuid = string(auction, "uuid");
			String itemName = string(auction, "item_name");
			String itemLore = string(auction, "item_lore");
			long startingBid = longValue(auction, "starting_bid", 0L);
			if (startingBid <= 0L) {
				continue;
			}
			String itemKey = MarketItemKey.fromDisplayNameAndLore(itemName, List.of(itemLore));
			if (itemKey.isBlank()) {
				continue;
			}
			out.add(new AuctionEntry(auctionUuid, itemKey, startingBid, nowMs, itemName, itemLore));
		}

		return new AuctionPage(safePage, totalPages, out);
	}

	public List<EndedAuction> fetchEndedAuctions() throws IOException {
		JsonObject root = getJsonObject(HYPIXEL_API_BASE + "/v2/skyblock/auctions_ended");
		JsonArray auctions = array(root, "auctions");
		if (auctions == null) {
			return List.of();
		}

		List<EndedAuction> out = new ArrayList<>();
		for (JsonElement raw : auctions) {
			JsonObject auction = object(raw);
			if (auction == null) {
				continue;
			}
			String auctionUuid = string(auction, "auction_id");
			if (auctionUuid.isBlank()) {
				auctionUuid = string(auction, "uuid");
			}
			String itemName = string(auction, "item_name");
			String itemLore = string(auction, "item_lore");
			String itemKey = MarketItemKey.fromDisplayNameAndLore(itemName, List.of(itemLore));
			long price = longValue(auction, "price", 0L);
			long endMs = longValue(auction, "timestamp", System.currentTimeMillis());
			if (auctionUuid.isBlank() || itemKey.isBlank() || price <= 0L || endMs <= 0L) {
				continue;
			}
			out.add(new EndedAuction(auctionUuid, itemKey, price, endMs, itemName));
		}
		return out;
	}

	private JsonObject getJsonObject(String url) throws IOException {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.GET()
			.timeout(REQUEST_TIMEOUT)
			.header("User-Agent", "ScamScreener/market")
			.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IOException("Hypixel API responded with status " + response.statusCode());
			}
			JsonElement parsed = JsonParser.parseString(response.body());
			JsonObject root = object(parsed);
			if (root == null || !bool(root, "success", true)) {
				throw new IOException("Hypixel API returned unsuccessful payload.");
			}
			return root;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Hypixel API request interrupted.", e);
		}
	}

	private static JsonObject object(JsonElement element) {
		if (element == null || !element.isJsonObject()) {
			return null;
		}
		return element.getAsJsonObject();
	}

	private static JsonObject object(JsonObject object, String key) {
		if (object == null || key == null || key.isBlank()) {
			return null;
		}
		return object(object.get(key));
	}

	private static JsonArray array(JsonObject object, String key) {
		if (object == null || key == null || key.isBlank()) {
			return null;
		}
		JsonElement element = object.get(key);
		return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
	}

	private static String string(JsonObject object, String key) {
		if (object == null || key == null || key.isBlank()) {
			return "";
		}
		JsonElement element = object.get(key);
		return (element == null || !element.isJsonPrimitive()) ? "" : element.getAsString().trim();
	}

	private static double number(JsonObject object, String key, double fallback) {
		if (object == null || key == null || key.isBlank()) {
			return fallback;
		}
		try {
			JsonElement element = object.get(key);
			return element == null ? fallback : element.getAsDouble();
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static long longValue(JsonObject object, String key, long fallback) {
		if (object == null || key == null || key.isBlank()) {
			return fallback;
		}
		try {
			JsonElement element = object.get(key);
			return element == null ? fallback : element.getAsLong();
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static int integer(JsonObject object, String key, int fallback) {
		if (object == null || key == null || key.isBlank()) {
			return fallback;
		}
		try {
			JsonElement element = object.get(key);
			return element == null ? fallback : element.getAsInt();
		} catch (Exception ignored) {
			return fallback;
		}
	}

	private static boolean bool(JsonObject object, String key, boolean fallback) {
		if (object == null || key == null || key.isBlank()) {
			return fallback;
		}
		try {
			JsonElement element = object.get(key);
			return element == null ? fallback : element.getAsBoolean();
		} catch (Exception ignored) {
			return fallback;
		}
	}

	public record AuctionEntry(
		String auctionUuid,
		String itemKey,
		long startingBidCoins,
		long updatedMs,
		String itemName,
		String itemLore
	) {
		public AuctionEntry {
			auctionUuid = auctionUuid == null ? "" : auctionUuid.trim();
			itemKey = itemKey == null ? "" : itemKey.trim();
			itemName = itemName == null ? "" : itemName.trim();
			itemLore = itemLore == null ? "" : itemLore.trim();
			startingBidCoins = Math.max(0L, startingBidCoins);
			updatedMs = Math.max(0L, updatedMs);
		}
	}

	public record AuctionPage(int page, int totalPages, List<AuctionEntry> auctions) {
		public AuctionPage {
			page = Math.max(0, page);
			totalPages = Math.max(0, totalPages);
			auctions = auctions == null ? List.of() : List.copyOf(auctions);
		}
	}

	public record EndedAuction(
		String auctionUuid,
		String itemKey,
		long priceCoins,
		long endMs,
		String itemName
	) {
		public EndedAuction {
			auctionUuid = auctionUuid == null ? "" : auctionUuid.trim();
			itemKey = itemKey == null ? "" : itemKey.trim();
			itemName = itemName == null ? "" : itemName.trim();
			priceCoins = Math.max(0L, priceCoins);
			endMs = Math.max(0L, endMs);
		}
	}
}
