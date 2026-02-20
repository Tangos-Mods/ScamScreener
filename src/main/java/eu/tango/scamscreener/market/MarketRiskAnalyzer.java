package eu.tango.scamscreener.market;

import eu.tango.scamscreener.rules.ScamRules;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class MarketRiskAnalyzer {
	private static final long HOT_CACHE_TTL_MS = 5_000L;

	private final AuctionBinIndexRepository binRepository;
	private final AuctionSalesRepository salesRepository;
	private final BazaarRepository bazaarRepository;
	private final NpcPriceCatalog npcPriceCatalog;
	private final Supplier<Settings> settingsSupplier;
	private final Map<String, CachedReference> hotCache = new ConcurrentHashMap<>();

	public MarketRiskAnalyzer(
		AuctionBinIndexRepository binRepository,
		AuctionSalesRepository salesRepository,
		BazaarRepository bazaarRepository,
		NpcPriceCatalog npcPriceCatalog
	) {
		this(binRepository, salesRepository, bazaarRepository, npcPriceCatalog, Settings::fromScamRules);
	}

	MarketRiskAnalyzer(
		AuctionBinIndexRepository binRepository,
		AuctionSalesRepository salesRepository,
		BazaarRepository bazaarRepository,
		NpcPriceCatalog npcPriceCatalog,
		Supplier<Settings> settingsSupplier
	) {
		this.binRepository = binRepository;
		this.salesRepository = salesRepository;
		this.bazaarRepository = bazaarRepository;
		this.npcPriceCatalog = npcPriceCatalog;
		this.settingsSupplier = settingsSupplier == null ? Settings::fromScamRules : settingsSupplier;
	}

	public RiskDecision evaluate(EvaluationInput input) {
		if (input == null || input.action() == Action.NONE || input.itemKey().isBlank()) {
			return RiskDecision.none();
		}
		Settings settings = safeSettings();
		long nowMs = Math.max(0L, input.nowMs());
		if (input.action() == Action.TRADE) {
			if (input.rareItem() && settings.marketRareTradeProtectionEnabled()) {
				return new RiskDecision(
					true,
					true,
					Reason.RARE_TRADE_PROTECTION,
					0.0,
					0.0,
					0.0,
					0.0,
					0L,
					0L,
					true,
					false,
					false,
					false,
					false
				);
			}
			return RiskDecision.none();
		}

		long offeredPrice = Math.max(0L, input.priceCoins());
		ReferenceData reference = resolveReference(input.itemKey(), nowMs);
		boolean lowConfidence = reference.confidence() < 0.60;

		boolean block = false;
		boolean warn = false;
		Reason reason = Reason.NONE;
		double overbidMultiple = 0.0;
		double rareUnderpriceRatio = 0.0;
		double inflatedMultiple30d = 0.0;
		double npcMultiple = 0.0;
		boolean inflatedWarn = false;
		boolean inflatedSevere = false;
		boolean npcWarn = false;
		boolean npcSevere = false;

		if ((input.action() == Action.AH_LIST || input.action() == Action.BZ_LIST) && offeredPrice > 0L && reference.fairPriceCoins() > 0L) {
			double listingRatio = offeredPrice / (double) reference.fairPriceCoins();
			overbidMultiple = listingRatio;
			rareUnderpriceRatio = listingRatio;
			if (listingRatio >= settings.marketAhOverbidBlockMultiple()) {
				block = true;
				warn = true;
				reason = Reason.OVERBID;
			} else if (listingRatio >= settings.marketAhOverbidWarnMultiple()) {
				warn = true;
				if (reason == Reason.NONE) {
					reason = Reason.OVERBID;
				}
			}

			if (listingRatio <= settings.marketUnderbidBlockRatio()) {
				block = true;
				warn = true;
				reason = Reason.UNDERBID;
			} else if (listingRatio <= settings.marketUnderbidWarnRatio()) {
				warn = true;
				if (reason == Reason.NONE) {
					reason = Reason.UNDERBID;
				}
			}
		}

		if ((input.action() == Action.AH_BUY || input.action() == Action.BZ_BUY) && offeredPrice > 0L && reference.fairPriceCoins() > 0L) {
			overbidMultiple = offeredPrice / (double) reference.fairPriceCoins();
			if (overbidMultiple >= settings.marketAhOverbidBlockMultiple()) {
				if (lowConfidence) {
					warn = true;
					reason = Reason.OVERBID;
				} else {
					block = true;
					warn = true;
					reason = Reason.OVERBID;
				}
			} else if (overbidMultiple >= settings.marketAhOverbidWarnMultiple()) {
				warn = true;
				reason = Reason.OVERBID;
			}
		}

		if ((input.action() == Action.AH_LIST || input.action() == Action.BZ_LIST) && input.rareItem() && offeredPrice > 0L && reference.fairPriceCoins() > 0L) {
			rareUnderpriceRatio = offeredPrice / (double) reference.fairPriceCoins();
			if (rareUnderpriceRatio <= settings.marketRareUnderpriceBlockRatio()) {
				block = true;
				warn = true;
				reason = Reason.RARE_UNDERPRICE;
			} else if (rareUnderpriceRatio <= settings.marketRareUnderpriceWarnRatio()) {
				warn = true;
				if (reason == Reason.NONE) {
					reason = Reason.RARE_UNDERPRICE;
				}
			}
		}

		if (offeredPrice > 0L && reference.median30dCoins() > 0L) {
			inflatedMultiple30d = offeredPrice / reference.median30dCoins();
			if (inflatedMultiple30d >= settings.marketInflatedSevereMultiple30d()) {
				inflatedWarn = true;
				inflatedSevere = true;
			} else if (inflatedMultiple30d >= settings.marketInflatedWarnMultiple30d()) {
				inflatedWarn = true;
			}
		}

		OptionalLong npcBase = npcPriceCatalog.findPriceCoins(input.itemKey());
		if (offeredPrice > 0L && npcBase.isPresent() && npcBase.getAsLong() > 0L) {
			npcMultiple = offeredPrice / (double) npcBase.getAsLong();
			if (npcMultiple >= settings.marketNpcBlockMultiple()) {
				npcWarn = true;
				npcSevere = true;
			} else if (npcMultiple >= settings.marketNpcWarnMultiple()) {
				npcWarn = true;
			}
		}

		if (!warn && reason != Reason.NONE && reason != Reason.ONLY_HIGHLIGHT) {
			warn = true;
		}
		if (!warn && (inflatedWarn || npcWarn)) {
			reason = Reason.ONLY_HIGHLIGHT;
		}

		return new RiskDecision(
			block,
			warn,
			reason,
			overbidMultiple,
			rareUnderpriceRatio,
			inflatedMultiple30d,
			npcMultiple,
			offeredPrice,
			reference.fairPriceCoins(),
			lowConfidence,
			inflatedWarn,
			inflatedSevere,
			npcWarn,
			npcSevere
		);
	}

	private Settings safeSettings() {
		try {
			Settings settings = settingsSupplier.get();
			return settings == null ? Settings.defaults() : settings;
		} catch (Exception ignored) {
			return Settings.defaults();
		}
	}

	public void clearHotCache() {
		hotCache.clear();
	}

	private ReferenceData resolveReference(String itemKey, long nowMs) {
		String key = MarketItemKey.normalize(itemKey);
		if (key.isBlank()) {
			return ReferenceData.empty();
		}
		CachedReference cached = hotCache.get(key);
		if (cached != null && cached.expiresMs() >= nowMs) {
			return cached.reference();
		}
		ReferenceData loaded = loadReferenceFromStorage(key, nowMs);
		hotCache.put(key, new CachedReference(loaded, nowMs + HOT_CACHE_TTL_MS));
		return loaded;
	}

	private ReferenceData loadReferenceFromStorage(String itemKey, long nowMs) {
		try {
			Optional<AuctionBinIndexRepository.BinQuote> bin = binRepository.findByItemKey(itemKey);
			Optional<AuctionSalesRepository.SaleStats> sales = salesRepository.stats30d(itemKey, nowMs);
			Optional<BazaarRepository.PriceStats> bazaar = bazaarRepository.stats30d(itemKey, nowMs);

			double median30d = sales.map(AuctionSalesRepository.SaleStats::medianCoins)
				.orElseGet(() -> bazaar.map(BazaarRepository.PriceStats::medianCoins).orElse(0.0));

			if (bin.isPresent() && bin.get().priceCoins() > 0L) {
				int samples = Math.max(1, bin.get().sampleCount());
				double confidence = samples >= 8 ? 0.90 : (samples >= 3 ? 0.72 : 0.55);
				return new ReferenceData(bin.get().priceCoins(), median30d, confidence);
			}
			if (sales.isPresent() && sales.get().medianCoins() > 0.0) {
				int samples = Math.max(1, sales.get().sampleCount());
				double confidence = samples >= 12 ? 0.84 : (samples >= 5 ? 0.68 : 0.48);
				return new ReferenceData(Math.round(sales.get().medianCoins()), median30d, confidence);
			}
			if (bazaar.isPresent() && bazaar.get().medianCoins() > 0.0) {
				int samples = Math.max(1, bazaar.get().sampleCount());
				double confidence = samples >= 12 ? 0.64 : (samples >= 5 ? 0.52 : 0.35);
				return new ReferenceData(Math.round(bazaar.get().medianCoins()), bazaar.get().medianCoins(), confidence);
			}
		} catch (SQLException ignored) {
		}
		return ReferenceData.empty();
	}

	public enum Action {
		NONE,
		AH_BUY,
		BZ_BUY,
		AH_LIST,
		BZ_LIST,
		TRADE
	}

	public enum Reason {
		NONE,
		OVERBID,
		UNDERBID,
		RARE_UNDERPRICE,
		RARE_TRADE_PROTECTION,
		ONLY_HIGHLIGHT
	}

	public record EvaluationInput(
		Action action,
		String itemKey,
		long priceCoins,
		boolean rareItem,
		long nowMs
	) {
		public EvaluationInput {
			action = action == null ? Action.NONE : action;
			itemKey = itemKey == null ? "" : MarketItemKey.normalize(itemKey);
			priceCoins = Math.max(0L, priceCoins);
			nowMs = Math.max(0L, nowMs);
		}
	}

	public record RiskDecision(
		boolean block,
		boolean warn,
		Reason reason,
		double overbidMultiple,
		double rareUnderpriceRatio,
		double inflatedMultiple30d,
		double npcMultiple,
		long offeredPriceCoins,
		long fairPriceCoins,
		boolean lowConfidence,
		boolean inflatedWarn,
		boolean inflatedSevere,
		boolean npcWarn,
		boolean npcSevere
	) {
		public RiskDecision {
			reason = reason == null ? Reason.NONE : reason;
			overbidMultiple = Math.max(0.0, overbidMultiple);
			rareUnderpriceRatio = Math.max(0.0, rareUnderpriceRatio);
			inflatedMultiple30d = Math.max(0.0, inflatedMultiple30d);
			npcMultiple = Math.max(0.0, npcMultiple);
			offeredPriceCoins = Math.max(0L, offeredPriceCoins);
			fairPriceCoins = Math.max(0L, fairPriceCoins);
		}

		public boolean hasHighlight() {
			return inflatedWarn || inflatedSevere || npcWarn || npcSevere;
		}

		private static RiskDecision none() {
			return new RiskDecision(false, false, Reason.NONE, 0.0, 0.0, 0.0, 0.0, 0L, 0L, true, false, false, false, false);
		}
	}

	private record CachedReference(ReferenceData reference, long expiresMs) {
	}

	private record ReferenceData(long fairPriceCoins, double median30dCoins, double confidence) {
		private static ReferenceData empty() {
			return new ReferenceData(0L, 0.0, 0.0);
		}
	}

	record Settings(
		double marketAhOverbidWarnMultiple,
		double marketAhOverbidBlockMultiple,
		double marketInflatedWarnMultiple30d,
		double marketInflatedSevereMultiple30d,
		double marketNpcWarnMultiple,
		double marketNpcBlockMultiple,
		double marketRareUnderpriceWarnRatio,
		double marketRareUnderpriceBlockRatio,
		boolean marketRareTradeProtectionEnabled
	) {
		private double marketUnderbidWarnRatio() {
			if (marketAhOverbidWarnMultiple <= 0.0) {
				return 0.0;
			}
			return 1.0 / marketAhOverbidWarnMultiple;
		}

		private double marketUnderbidBlockRatio() {
			if (marketAhOverbidBlockMultiple <= 0.0) {
				return 0.0;
			}
			return 1.0 / marketAhOverbidBlockMultiple;
		}

		private static Settings fromScamRules() {
			return new Settings(
				ScamRules.marketAhOverbidWarnMultiple(),
				ScamRules.marketAhOverbidBlockMultiple(),
				ScamRules.marketInflatedWarnMultiple30d(),
				ScamRules.marketInflatedSevereMultiple30d(),
				ScamRules.marketNpcWarnMultiple(),
				ScamRules.marketNpcBlockMultiple(),
				ScamRules.marketRareUnderpriceWarnRatio(),
				ScamRules.marketRareUnderpriceBlockRatio(),
				ScamRules.marketRareTradeProtectionEnabled()
			);
		}

		private static Settings defaults() {
			return new Settings(2.5, 4.0, 3.0, 6.0, 25.0, 100.0, 0.65, 0.45, true);
		}
	}
}
