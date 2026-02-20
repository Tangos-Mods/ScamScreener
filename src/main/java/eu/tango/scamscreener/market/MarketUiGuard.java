package eu.tango.scamscreener.market;

import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.ui.DebugReporter;
import eu.tango.scamscreener.ui.MessageDispatcher;
import eu.tango.scamscreener.ui.Messages;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarketUiGuard {
	private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d{1,3}(?:[., ]\\d{3})+|\\d+(?:[.,]\\d+)?)([kmb])?", Pattern.CASE_INSENSITIVE);
	private static final Pattern ITEM_LINE_PATTERN = Pattern.compile("^(?:item|product|selling|buying|selected item|bazaar item)\\s*:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ITEM_VALUE_PATTERN = Pattern.compile("(?i)(?:item|product)\\s*[:|-]\\s*(.+)$");
	private static final long HIGHLIGHT_COOLDOWN_MS = 2_000L;

	private final MarketRiskAnalyzer marketRiskAnalyzer;
	private final RareItemClassifier rareItemClassifier;
	private final MarketClickConfirmStore confirmStore;
	private final DebugReporter debugReporter;
	private final Map<String, Long> lastHighlightAtMs = new LinkedHashMap<>();

	public MarketUiGuard(
		MarketRiskAnalyzer marketRiskAnalyzer,
		RareItemClassifier rareItemClassifier,
		MarketClickConfirmStore confirmStore,
		DebugReporter debugReporter
	) {
		this.marketRiskAnalyzer = marketRiskAnalyzer;
		this.rareItemClassifier = rareItemClassifier;
		this.confirmStore = confirmStore;
		this.debugReporter = debugReporter;
	}

	public boolean allowInventoryClick(int containerId, int slotId, int buttonNum, ClickType clickType) {
		if (!ScamRules.marketSafetyEnabled()) {
			return true;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.getConnection() == null || client.screen == null) {
			return true;
		}
		if (!(client.screen instanceof AbstractContainerScreen<?> containerScreen)) {
			return true;
		}
		if (slotId < 0 || slotId >= client.player.containerMenu.slots.size()) {
			return true;
		}
		Slot slot = client.player.containerMenu.getSlot(slotId);
		if (slot == null || !slot.hasItem()) {
			return true;
		}
		ItemStack stack = slot.getItem();
		if (stack == null || stack.isEmpty()) {
			return true;
		}
		String title = safeLower(containerScreen.getTitle().getString());
		MarketRiskAnalyzer.Action action = detectAction(title);
		if (action == MarketRiskAnalyzer.Action.NONE) {
			return true;
		}

		List<String> tooltip = readTooltipLines(client, stack);
		String itemName = stack.getHoverName().getString();
		String itemKey = resolveRiskItemKey(action, stack, tooltip);
		RareItemClassifier.Match rare = rareItemClassifier.classify(stack, itemKey, tooltip);
		long offeredPrice = extractLikelyCoins(itemName, tooltip);
		long nowMs = System.currentTimeMillis();

		MarketRiskAnalyzer.RiskDecision risk = marketRiskAnalyzer.evaluate(new MarketRiskAnalyzer.EvaluationInput(
			action,
			itemKey,
			offeredPrice,
			rare.rare(),
			nowMs
		));
		debug(action, slotId, itemKey, offeredPrice, risk);

		if (risk.hasHighlight() && ScamRules.marketTooltipHighlightEnabled() && shouldSendHighlight(itemKey, nowMs)) {
			double multiple = risk.npcSevere() || risk.npcWarn() ? risk.npcMultiple() : risk.inflatedMultiple30d();
			boolean severe = risk.inflatedSevere() || risk.npcSevere();
			boolean npcBased = risk.npcSevere() || risk.npcWarn();
			MessageDispatcher.reply(Messages.marketInflatedHighlight(itemName, multiple, severe, npcBased));
		}

		if (!risk.block() && !risk.warn()) {
			return true;
		}

		if (requiresConfirm(action, risk)) {
			String fingerprint = fingerprint(action, slotId, itemKey, offeredPrice, title);
			int required = ScamRules.marketConfirmClicksRequired();
			long windowMs = ScamRules.marketConfirmWindowSeconds() * 1_000L;
			MarketClickConfirmStore.Decision decision = confirmStore.registerAttempt(fingerprint, nowMs, required, windowMs);
			if (!decision.allow()) {
				if (risk.reason() == MarketRiskAnalyzer.Reason.RARE_TRADE_PROTECTION) {
					MessageDispatcher.reply(Messages.marketRareTradeBlocked(itemName, decision.currentCount(), decision.requiredCount()));
				} else {
					MessageDispatcher.reply(Messages.marketSafetyBlocked(
						reasonLabel(risk.reason()),
						primaryRatio(risk),
						risk.offeredPriceCoins(),
						risk.fairPriceCoins(),
						decision.currentCount(),
						decision.requiredCount()
					));
				}
				return false;
			}
			MessageDispatcher.reply(Messages.marketConfirmProgress(required, required, ScamRules.marketConfirmWindowSeconds()));
			return true;
		}

		MessageDispatcher.reply(Messages.marketSafetyWarning(
			reasonLabel(risk.reason()),
			primaryRatio(risk),
			risk.offeredPriceCoins(),
			risk.fairPriceCoins(),
			risk.lowConfidence()
		));
		return true;
	}

	public void reset() {
		confirmStore.clear();
		lastHighlightAtMs.clear();
		marketRiskAnalyzer.clearHotCache();
	}

	public void appendMarketTooltipStatus(ItemStack stack, List<Component> lines) {
		if (!ScamRules.marketSafetyEnabled() || !ScamRules.marketTooltipHighlightEnabled()) {
			return;
		}
		if (stack == null || stack.isEmpty() || lines == null) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.getConnection() == null || client.screen == null) {
			return;
		}
		if (!(client.screen instanceof AbstractContainerScreen<?> containerScreen)) {
			return;
		}

		removeExistingSsTooltip(lines);

		String title = safeLower(containerScreen.getTitle().getString());
		MarketRiskAnalyzer.Action action = detectAction(title);
		if (action == MarketRiskAnalyzer.Action.NONE || action == MarketRiskAnalyzer.Action.TRADE) {
			return;
		}

		List<String> tooltipLines = toPlainLines(lines);
		String itemName = stack.getHoverName().getString();
		String itemKey = resolveRiskItemKey(action, stack, tooltipLines);
		long offeredPrice = extractLikelyCoins(itemName, tooltipLines);
		if (offeredPrice <= 0L) {
			return;
		}

		RareItemClassifier.Match rare = rareItemClassifier.classify(stack, itemKey, tooltipLines);
		MarketRiskAnalyzer.RiskDecision risk = marketRiskAnalyzer.evaluate(new MarketRiskAnalyzer.EvaluationInput(
			action,
			itemKey,
			offeredPrice,
			rare.rare(),
			System.currentTimeMillis()
		));
		if (risk.fairPriceCoins() <= 0L) {
			return;
		}

		double ratio = offeredPrice / (double) risk.fairPriceCoins();
		MutableComponent statusLine = statusLineFor(action, risk, ratio);
		if (statusLine == null) {
			return;
		}
		lines.add(statusLine);
	}

	private static String resolveRiskItemKey(MarketRiskAnalyzer.Action action, ItemStack clickedStack, List<String> tooltipLines) {
		String clickedName = clickedStack == null || clickedStack.isEmpty() ? "" : clickedStack.getHoverName().getString();
		String defaultKey = MarketItemKey.fromDisplayNameAndLore(clickedName, tooltipLines);
		if (action != MarketRiskAnalyzer.Action.AH_LIST && action != MarketRiskAnalyzer.Action.BZ_LIST) {
			return defaultKey;
		}

		String listingName = extractListingTargetName(tooltipLines);
		if (listingName.isBlank()) {
			return defaultKey;
		}
		String listingKey = MarketItemKey.fromDisplayNameAndLore(listingName, List.of());
		return listingKey.isBlank() ? defaultKey : listingKey;
	}

	private static MarketRiskAnalyzer.Action detectAction(String normalizedTitle) {
		if (normalizedTitle == null || normalizedTitle.isBlank()) {
			return MarketRiskAnalyzer.Action.NONE;
		}
		String title = normalizedTitle.toLowerCase(Locale.ROOT);
		if (title.contains("trade")) {
			return MarketRiskAnalyzer.Action.TRADE;
		}
		if (title.contains("create buy order")
			|| title.contains("create sell offer")
			|| title.contains("confirm offer")
			|| title.contains("confirm order")
			|| title.contains("buy order")
			|| title.contains("sell offer")) {
			return MarketRiskAnalyzer.Action.BZ_LIST;
		}
		if (title.contains("bazaar")) {
			return MarketRiskAnalyzer.Action.BZ_BUY;
		}
		if (!title.contains("auction") && !title.contains("bin")) {
			return MarketRiskAnalyzer.Action.NONE;
		}
		if (title.contains("create bin auction")
			|| title.contains("create auction")
			|| title.contains("auction duration")
			|| title.contains("confirm auction")
			|| title.contains("manage auctions")) {
			return MarketRiskAnalyzer.Action.AH_LIST;
		}
		return MarketRiskAnalyzer.Action.AH_BUY;
	}

	private static List<String> readTooltipLines(Minecraft client, ItemStack stack) {
		List<String> out = new ArrayList<>();
		if (client == null || stack == null || stack.isEmpty()) {
			return out;
		}
		try {
			for (Component line : Screen.getTooltipFromItem(client, stack)) {
				if (line == null) {
					continue;
				}
				String text = ChatFormatting.stripFormatting(line.getString());
				if (text != null && !text.isBlank()) {
					out.add(text.trim());
				}
			}
		} catch (Exception ignored) {
		}
		return out;
	}

	private static long extractLikelyCoins(String itemName, List<String> tooltipLines) {
		long best = parseCoinsFromText(itemName);
		if (tooltipLines != null) {
			for (String line : tooltipLines) {
				if (line == null || line.isBlank()) {
					continue;
				}
				String lower = line.toLowerCase(Locale.ROOT);
				boolean priceLine = lower.contains("coin")
					|| lower.contains("price")
					|| lower.contains("cost")
					|| lower.contains("bid")
					|| lower.contains("buy it now")
					|| lower.contains("starting");
				if (!priceLine) {
					continue;
				}
				best = Math.max(best, parseCoinsFromText(line));
			}
		}
		return best;
	}

	private static String extractListingTargetName(List<String> tooltipLines) {
		if (tooltipLines == null || tooltipLines.isEmpty()) {
			return "";
		}
		for (String line : tooltipLines) {
			String raw = line == null ? "" : line.trim();
			if (raw.isBlank()) {
				continue;
			}
			Matcher direct = ITEM_LINE_PATTERN.matcher(raw);
			if (direct.matches()) {
				String candidate = normalizeItemNameCandidate(direct.group(1));
				if (!candidate.isBlank()) {
					return candidate;
				}
			}

			String lower = raw.toLowerCase(Locale.ROOT);
			if (lower.contains("item") || lower.contains("product")) {
				Matcher inline = ITEM_VALUE_PATTERN.matcher(raw);
				if (inline.matches()) {
					String candidate = normalizeItemNameCandidate(inline.group(1));
					if (!candidate.isBlank()) {
						return candidate;
					}
				}
			}
		}
		return "";
	}

	private static String normalizeItemNameCandidate(String raw) {
		if (raw == null || raw.isBlank()) {
			return "";
		}
		String clean = raw;
		int coinsIndex = clean.toLowerCase(Locale.ROOT).indexOf("coins");
		if (coinsIndex > 0) {
			clean = clean.substring(0, coinsIndex);
		}
		clean = clean.replace("(", " ").replace(")", " ").trim();
		return clean;
	}

	private static long parseCoinsFromText(String raw) {
		if (raw == null || raw.isBlank()) {
			return 0L;
		}
		long best = 0L;
		Matcher matcher = NUMBER_PATTERN.matcher(raw);
		while (matcher.find()) {
			String baseRaw = matcher.group(1);
			String suffixRaw = matcher.group(2);
			if (baseRaw == null || baseRaw.isBlank()) {
				continue;
			}
			long parsed = toCoins(baseRaw, suffixRaw);
			best = Math.max(best, parsed);
		}
		return best;
	}

	private static long toCoins(String baseRaw, String suffixRaw) {
		String compact = baseRaw == null ? "" : baseRaw.replace(" ", "").trim();
		if (compact.isBlank()) {
			return 0L;
		}
		String suffix = suffixRaw == null ? "" : suffixRaw.trim().toLowerCase(Locale.ROOT);
		try {
			if (!suffix.isBlank()) {
				double value = Double.parseDouble(compact.replace(',', '.'));
				double multiplier = switch (suffix) {
					case "k" -> 1_000.0;
					case "m" -> 1_000_000.0;
					case "b" -> 1_000_000_000.0;
					default -> 1.0;
				};
				return Math.max(0L, Math.round(value * multiplier));
			}
			String digits = compact.replaceAll("[^0-9]", "");
			if (digits.isBlank()) {
				return 0L;
			}
			return Math.max(0L, Long.parseLong(digits));
		} catch (Exception ignored) {
			return 0L;
		}
	}

	private boolean shouldSendHighlight(String itemKey, long nowMs) {
		String key = MarketItemKey.normalize(itemKey);
		if (key.isBlank()) {
			return true;
		}
		Long last = lastHighlightAtMs.get(key);
		if (last != null && (nowMs - last) < HIGHLIGHT_COOLDOWN_MS) {
			return false;
		}
		lastHighlightAtMs.put(key, nowMs);
		while (lastHighlightAtMs.size() > 128) {
			String first = lastHighlightAtMs.keySet().iterator().next();
			lastHighlightAtMs.remove(first);
		}
		return true;
	}

	private static String fingerprint(MarketRiskAnalyzer.Action action, int slotId, String itemKey, long offeredPrice, String title) {
		return action.name() + "|" + Math.max(0, slotId) + "|" + MarketItemKey.normalize(itemKey) + "|" + Math.max(0L, offeredPrice) + "|" + safeLower(title);
	}

	private static boolean requiresConfirm(MarketRiskAnalyzer.Action action, MarketRiskAnalyzer.RiskDecision risk) {
		if (risk == null || action == null) {
			return false;
		}
		if (risk.block()) {
			return true;
		}
		if (action == MarketRiskAnalyzer.Action.AH_LIST || action == MarketRiskAnalyzer.Action.BZ_LIST) {
			return risk.reason() == MarketRiskAnalyzer.Reason.OVERBID
				|| risk.reason() == MarketRiskAnalyzer.Reason.UNDERBID
				|| risk.reason() == MarketRiskAnalyzer.Reason.RARE_UNDERPRICE;
		}
		return false;
	}

	private static MutableComponent statusLineFor(MarketRiskAnalyzer.Action action, MarketRiskAnalyzer.RiskDecision risk, double ratio) {
		if (risk == null) {
			return null;
		}
		if (risk.reason() == MarketRiskAnalyzer.Reason.OVERBID) {
			return Messages.marketTooltipOverbidding();
		}
		if (risk.reason() == MarketRiskAnalyzer.Reason.UNDERBID || risk.reason() == MarketRiskAnalyzer.Reason.RARE_UNDERPRICE) {
			return Messages.marketTooltipUnderbidding();
		}

		double safeRatio = Math.max(0.0, ratio);
		if (action == MarketRiskAnalyzer.Action.AH_BUY || action == MarketRiskAnalyzer.Action.BZ_BUY) {
			if (safeRatio >= ScamRules.marketAhOverbidWarnMultiple()) {
				return Messages.marketTooltipOverbidding();
			}
			return Messages.marketTooltipCompetetive();
		}
		if (action == MarketRiskAnalyzer.Action.AH_LIST || action == MarketRiskAnalyzer.Action.BZ_LIST) {
			double underWarn = 1.0 / Math.max(1.0, ScamRules.marketAhOverbidWarnMultiple());
			if (safeRatio <= underWarn) {
				return Messages.marketTooltipUnderbidding();
			}
			if (safeRatio >= ScamRules.marketAhOverbidWarnMultiple()) {
				return Messages.marketTooltipOverbidding();
			}
			return Messages.marketTooltipCompetetive();
		}
		return null;
	}

	private static void removeExistingSsTooltip(List<Component> lines) {
		if (lines == null || lines.isEmpty()) {
			return;
		}
		lines.removeIf(line -> {
			if (line == null) {
				return false;
			}
			String text = line.getString();
			return text != null && text.startsWith("[SS] ");
		});
	}

	private static List<String> toPlainLines(List<Component> lines) {
		if (lines == null || lines.isEmpty()) {
			return List.of();
		}
		List<String> out = new ArrayList<>(lines.size());
		for (Component line : lines) {
			if (line == null) {
				continue;
			}
			String stripped = ChatFormatting.stripFormatting(line.getString());
			String safe = stripped == null ? line.getString() : stripped;
			if (safe == null || safe.isBlank()) {
				continue;
			}
			out.add(safe.trim());
		}
		return out;
	}

	private static String reasonLabel(MarketRiskAnalyzer.Reason reason) {
		if (reason == null) {
			return "market risk";
		}
		return switch (reason) {
			case OVERBID -> "overbid";
			case UNDERBID -> "underbid";
			case RARE_UNDERPRICE -> "rare underprice";
			case RARE_TRADE_PROTECTION -> "rare trade protection";
			case ONLY_HIGHLIGHT, NONE -> "market risk";
		};
	}

	private static double primaryRatio(MarketRiskAnalyzer.RiskDecision risk) {
		if (risk == null) {
			return 0.0;
		}
		if (risk.overbidMultiple() > 0.0) {
			return risk.overbidMultiple();
		}
		if (risk.rareUnderpriceRatio() > 0.0) {
			return risk.rareUnderpriceRatio();
		}
		if (risk.inflatedMultiple30d() > 0.0) {
			return risk.inflatedMultiple30d();
		}
		return risk.npcMultiple();
	}

	private void debug(MarketRiskAnalyzer.Action action, int slotId, String itemKey, long offeredPrice, MarketRiskAnalyzer.RiskDecision risk) {
		if (debugReporter == null || risk == null) {
			return;
		}
		debugReporter.debugMarket(
			"click action=" + action
				+ " slot=" + slotId
				+ " item=" + MarketItemKey.normalize(itemKey)
				+ " price=" + offeredPrice
				+ " block=" + risk.block()
				+ " warn=" + risk.warn()
				+ " reason=" + risk.reason().name().toLowerCase(Locale.ROOT)
				+ " ratio=" + String.format(Locale.ROOT, "%.2f", primaryRatio(risk))
				+ " lowConfidence=" + risk.lowConfidence()
		);
	}

	private static String safeLower(String text) {
		if (text == null || text.isBlank()) {
			return "";
		}
		return text.toLowerCase(Locale.ROOT).trim();
	}
}
