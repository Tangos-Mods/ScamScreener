package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.rules.ScamRules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class MarketSafetySettingsScreen extends ScamScreenerGUI {
	private static final String[] PROFILES = {"CONSERVATIVE", "BALANCED", "AGGRESSIVE"};
	private static final int[] CONFIRM_CLICKS = {1, 2, 3, 4, 5};
	private static final int[] CONFIRM_WINDOWS = {4, 6, 8, 10, 12};
	private static final double[] AH_WARN = {2.0, 2.5, 3.0, 3.5, 4.0};
	private static final double[] AH_BLOCK = {3.0, 4.0, 5.0, 6.0, 8.0};
	private static final double[] INFLATED_WARN = {2.0, 2.5, 3.0, 4.0, 5.0};
	private static final double[] INFLATED_SEVERE = {4.0, 6.0, 8.0, 10.0, 12.0};
	private static final double[] NPC_WARN = {10.0, 15.0, 25.0, 35.0, 50.0};
	private static final double[] NPC_BLOCK = {40.0, 60.0, 100.0, 150.0, 200.0};
	private static final double[] RARE_WARN = {0.80, 0.70, 0.65, 0.60, 0.55};
	private static final double[] RARE_BLOCK = {0.60, 0.50, 0.45, 0.40, 0.35};

	private Button enabledButton;
	private Button profileButton;
	private Button confirmClicksButton;
	private Button confirmWindowButton;
	private Button ahWarnButton;
	private Button ahBlockButton;
	private Button inflatedWarnButton;
	private Button inflatedSevereButton;
	private Button npcWarnButton;
	private Button npcBlockButton;
	private Button rareWarnButton;
	private Button rareBlockButton;
	private Button rareTradeButton;
	private Button tooltipHighlightButton;

	MarketSafetySettingsScreen(Screen parent) {
		super(Component.literal("ScamScreener Market Safety"), parent);
	}

	@Override
	protected void init() {
		ColumnState column = defaultColumnState();
		int fullWidth = column.buttonWidth();
		int x = column.x();
		int y = column.y();
		int spacing = DEFAULT_SPLIT_SPACING;
		int half = splitWidth(fullWidth, 2, spacing);
		int rightX = x + half + spacing;

		enabledButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketSafetyEnabled(!ScamRules.marketSafetyEnabled());
			refreshButtons();
		}).bounds(x, y, half, 20).build());
		profileButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketSafetyProfile(cycle(PROFILES, ScamRules.marketSafetyProfile()));
			refreshButtons();
		}).bounds(rightX, y, half, 20).build());
		y += ROW_HEIGHT;

		confirmClicksButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketConfirmClicksRequired(cycleInt(CONFIRM_CLICKS, ScamRules.marketConfirmClicksRequired()));
			refreshButtons();
		}).bounds(x, y, half, 20).build());
		confirmWindowButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketConfirmWindowSeconds(cycleInt(CONFIRM_WINDOWS, ScamRules.marketConfirmWindowSeconds()));
			refreshButtons();
		}).bounds(rightX, y, half, 20).build());
		y += ROW_HEIGHT;

		ahWarnButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketAhOverbidWarnMultiple(cycleDouble(AH_WARN, ScamRules.marketAhOverbidWarnMultiple()));
			refreshButtons();
		}).bounds(x, y, half, 20).build());
		ahBlockButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketAhOverbidBlockMultiple(cycleDouble(AH_BLOCK, ScamRules.marketAhOverbidBlockMultiple()));
			refreshButtons();
		}).bounds(rightX, y, half, 20).build());
		y += ROW_HEIGHT;

		inflatedWarnButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketInflatedWarnMultiple30d(cycleDouble(INFLATED_WARN, ScamRules.marketInflatedWarnMultiple30d()));
			refreshButtons();
		}).bounds(x, y, half, 20).build());
		inflatedSevereButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketInflatedSevereMultiple30d(cycleDouble(INFLATED_SEVERE, ScamRules.marketInflatedSevereMultiple30d()));
			refreshButtons();
		}).bounds(rightX, y, half, 20).build());
		y += ROW_HEIGHT;

		npcWarnButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketNpcWarnMultiple(cycleDouble(NPC_WARN, ScamRules.marketNpcWarnMultiple()));
			refreshButtons();
		}).bounds(x, y, half, 20).build());
		npcBlockButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketNpcBlockMultiple(cycleDouble(NPC_BLOCK, ScamRules.marketNpcBlockMultiple()));
			refreshButtons();
		}).bounds(rightX, y, half, 20).build());
		y += ROW_HEIGHT;

		rareWarnButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketRareUnderpriceWarnRatio(cycleDouble(RARE_WARN, ScamRules.marketRareUnderpriceWarnRatio()));
			refreshButtons();
		}).bounds(x, y, half, 20).build());
		rareBlockButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketRareUnderpriceBlockRatio(cycleDouble(RARE_BLOCK, ScamRules.marketRareUnderpriceBlockRatio()));
			refreshButtons();
		}).bounds(rightX, y, half, 20).build());
		y += ROW_HEIGHT;

		rareTradeButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketRareTradeProtectionEnabled(!ScamRules.marketRareTradeProtectionEnabled());
			refreshButtons();
		}).bounds(x, y, half, 20).build());
		tooltipHighlightButton = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
			ScamRules.setMarketTooltipHighlightEnabled(!ScamRules.marketTooltipHighlightEnabled());
			refreshButtons();
		}).bounds(rightX, y, half, 20).build());

		addBackButton(fullWidth);
		refreshButtons();
	}

	private void refreshButtons() {
		if (enabledButton != null) {
			enabledButton.setMessage(onOffLine("Enabled: ", ScamRules.marketSafetyEnabled()));
		}
		if (profileButton != null) {
			profileButton.setMessage(Component.literal("Profile: " + ScamRules.marketSafetyProfile()));
		}
		if (confirmClicksButton != null) {
			confirmClicksButton.setMessage(Component.literal("Confirm Clicks: " + ScamRules.marketConfirmClicksRequired()));
		}
		if (confirmWindowButton != null) {
			confirmWindowButton.setMessage(Component.literal("Confirm Window: " + ScamRules.marketConfirmWindowSeconds() + "s"));
		}
		if (ahWarnButton != null) {
			ahWarnButton.setMessage(Component.literal("AH Overbid Warn: " + formatMultiplier(ScamRules.marketAhOverbidWarnMultiple())));
		}
		if (ahBlockButton != null) {
			ahBlockButton.setMessage(Component.literal("AH Overbid Block: " + formatMultiplier(ScamRules.marketAhOverbidBlockMultiple())));
		}
		if (inflatedWarnButton != null) {
			inflatedWarnButton.setMessage(Component.literal("Inflated Warn: " + formatMultiplier(ScamRules.marketInflatedWarnMultiple30d())));
		}
		if (inflatedSevereButton != null) {
			inflatedSevereButton.setMessage(Component.literal("Inflated Severe: " + formatMultiplier(ScamRules.marketInflatedSevereMultiple30d())));
		}
		if (npcWarnButton != null) {
			npcWarnButton.setMessage(Component.literal("NPC Warn: " + formatMultiplier(ScamRules.marketNpcWarnMultiple())));
		}
		if (npcBlockButton != null) {
			npcBlockButton.setMessage(Component.literal("NPC Severe: " + formatMultiplier(ScamRules.marketNpcBlockMultiple())));
		}
		if (rareWarnButton != null) {
			rareWarnButton.setMessage(Component.literal("Rare Underprice Warn: " + formatRatio(ScamRules.marketRareUnderpriceWarnRatio())));
		}
		if (rareBlockButton != null) {
			rareBlockButton.setMessage(Component.literal("Rare Underprice Block: " + formatRatio(ScamRules.marketRareUnderpriceBlockRatio())));
		}
		if (rareTradeButton != null) {
			rareTradeButton.setMessage(onOffLine("Rare Trade Protect: ", ScamRules.marketRareTradeProtectionEnabled()));
		}
		if (tooltipHighlightButton != null) {
			tooltipHighlightButton.setMessage(onOffLine("Tooltip Highlight: ", ScamRules.marketTooltipHighlightEnabled()));
		}
	}

	private static String cycle(String[] options, String value) {
		if (options == null || options.length == 0) {
			return "";
		}
		int index = indexOf(options, value);
		return options[(index + 1) % options.length];
	}

	private static int cycleInt(int[] options, int value) {
		if (options == null || options.length == 0) {
			return value;
		}
		int index = indexOf(options, value);
		return options[(index + 1) % options.length];
	}

	private static double cycleDouble(double[] options, double value) {
		if (options == null || options.length == 0) {
			return value;
		}
		int index = indexClosest(options, value);
		return options[(index + 1) % options.length];
	}

	private static int indexOf(String[] options, String value) {
		String target = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
		for (int i = 0; i < options.length; i++) {
			if (options[i].equalsIgnoreCase(target)) {
				return i;
			}
		}
		return 0;
	}

	private static int indexOf(int[] options, int value) {
		for (int i = 0; i < options.length; i++) {
			if (options[i] == value) {
				return i;
			}
		}
		return 0;
	}

	private static int indexClosest(double[] options, double value) {
		int bestIndex = 0;
		double bestDiff = Double.MAX_VALUE;
		for (int i = 0; i < options.length; i++) {
			double diff = Math.abs(options[i] - value);
			if (diff < bestDiff) {
				bestDiff = diff;
				bestIndex = i;
			}
		}
		return bestIndex;
	}

	private static String formatMultiplier(double value) {
		return String.format(Locale.ROOT, "%.2fx", value);
	}

	private static String formatRatio(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}
}

