package eu.tango.scamscreener.market;

import eu.tango.scamscreener.ui.DebugReporter;
import eu.tango.scamscreener.util.AsyncDispatcher;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MarketDataRefreshService {
	private static final long DEFAULT_REFRESH_INTERVAL_MS = 120_000L;
	private static final int MAX_AUCTION_PAGES = 60;
	private static final long RETENTION_MS = 35L * 24L * 60L * 60L * 1000L;

	private final HypixelMarketApiClient apiClient;
	private final AuctionBinIndexRepository auctionBinIndexRepository;
	private final AuctionSalesRepository auctionSalesRepository;
	private final BazaarRepository bazaarRepository;
	private final NpcPriceCatalog npcPriceCatalog;
	private final DebugReporter debugReporter;
	private final long refreshIntervalMs;
	private final AtomicBoolean refreshing = new AtomicBoolean(false);

	private volatile long lastRefreshMs;
	private volatile boolean started;

	public MarketDataRefreshService(
		HypixelMarketApiClient apiClient,
		AuctionBinIndexRepository auctionBinIndexRepository,
		AuctionSalesRepository auctionSalesRepository,
		BazaarRepository bazaarRepository,
		NpcPriceCatalog npcPriceCatalog,
		DebugReporter debugReporter
	) {
		this(apiClient, auctionBinIndexRepository, auctionSalesRepository, bazaarRepository, npcPriceCatalog, debugReporter, DEFAULT_REFRESH_INTERVAL_MS);
	}

	MarketDataRefreshService(
		HypixelMarketApiClient apiClient,
		AuctionBinIndexRepository auctionBinIndexRepository,
		AuctionSalesRepository auctionSalesRepository,
		BazaarRepository bazaarRepository,
		NpcPriceCatalog npcPriceCatalog,
		DebugReporter debugReporter,
		long refreshIntervalMs
	) {
		this.apiClient = apiClient;
		this.auctionBinIndexRepository = auctionBinIndexRepository;
		this.auctionSalesRepository = auctionSalesRepository;
		this.bazaarRepository = bazaarRepository;
		this.npcPriceCatalog = npcPriceCatalog;
		this.debugReporter = debugReporter;
		this.refreshIntervalMs = Math.max(30_000L, refreshIntervalMs);
	}

	public void tick(boolean active, long nowMs) {
		if (!active) {
			return;
		}
		if (!started) {
			started = true;
			npcPriceCatalog.seedIfNeeded(nowMs);
		}
		long safeNow = Math.max(0L, nowMs);
		if ((safeNow - lastRefreshMs) < refreshIntervalMs) {
			return;
		}
		if (!refreshing.compareAndSet(false, true)) {
			return;
		}
		lastRefreshMs = safeNow;

		AsyncDispatcher.runIo(() -> {
			try {
				refreshNow(safeNow);
			} finally {
				refreshing.set(false);
			}
		});
	}

	public void close() {
		refreshing.set(false);
	}

	private void refreshNow(long nowMs) {
		long cutoff = Math.max(0L, nowMs - RETENTION_MS);
		try {
			List<BazaarRepository.BazaarSnapshot> bazaarSnapshots = apiClient.fetchBazaarSnapshots(nowMs);
			bazaarRepository.saveSnapshots(bazaarSnapshots);
			bazaarRepository.pruneOlderThan(cutoff);

			List<HypixelMarketApiClient.EndedAuction> endedAuctions = apiClient.fetchEndedAuctions();
			auctionSalesRepository.saveEndedAuctions(endedAuctions);
			auctionSalesRepository.pruneOlderThan(cutoff);

			List<HypixelMarketApiClient.AuctionEntry> liveBins = fetchAllBinAuctions(nowMs);
			auctionBinIndexRepository.rebuildFromAuctions(liveBins, nowMs);

			debug("refresh ok bz=" + bazaarSnapshots.size() + " ended=" + endedAuctions.size() + " bins=" + liveBins.size());
		} catch (IOException e) {
			debug("refresh api failed: " + safeMessage(e));
		} catch (SQLException e) {
			debug("refresh db failed: " + safeMessage(e));
		} catch (Exception e) {
			debug("refresh failed: " + safeMessage(e));
		}
	}

	private List<HypixelMarketApiClient.AuctionEntry> fetchAllBinAuctions(long nowMs) throws IOException {
		HypixelMarketApiClient.AuctionPage firstPage = apiClient.fetchAuctionsPage(0, nowMs);
		List<HypixelMarketApiClient.AuctionEntry> entries = new ArrayList<>(firstPage.auctions());
		int totalPages = Math.max(0, firstPage.totalPages());
		int pageLimit = Math.min(totalPages, MAX_AUCTION_PAGES);
		for (int page = 1; page < pageLimit; page++) {
			HypixelMarketApiClient.AuctionPage next = apiClient.fetchAuctionsPage(page, nowMs);
			entries.addAll(next.auctions());
		}
		return entries;
	}

	private void debug(String message) {
		if (debugReporter == null) {
			return;
		}
		debugReporter.debugMarket(message);
	}

	private static String safeMessage(Throwable throwable) {
		if (throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()) {
			return "unknown";
		}
		return throwable.getMessage();
	}
}

