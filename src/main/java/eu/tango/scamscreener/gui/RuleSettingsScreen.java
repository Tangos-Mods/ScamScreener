package eu.tango.scamscreener.gui;

import eu.tango.scamscreener.rules.ScamRules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

final class RuleSettingsScreen extends ScamScreenerGUI {
	private final Map<ScamRules.ScamRule, Button> ruleButtons = new EnumMap<>(ScamRules.ScamRule.class);

	RuleSettingsScreen(Screen parent) {
		super(Component.literal("ScamScreener Rules"), parent);
	}

	@Override
	protected void init() {
		ruleButtons.clear();
		ScamRules.ScamRule[] rules = ScamRules.ScamRule.values();
		int columns = 2;
		int spacingX = 8;
		int spacingY = ROW_HEIGHT;
		int sidePadding = 20;
		int availableWidth = this.width - sidePadding * 2;
		int buttonWidth = Math.max(100, (availableWidth - spacingX) / columns);
		int startX = (this.width - (buttonWidth * columns + spacingX)) / 2;
		int startY = 34;

		for (int i = 0; i < rules.length; i++) {
			ScamRules.ScamRule rule = rules[i];
			int row = i / columns;
			int col = i % columns;
			int x = startX + col * (buttonWidth + spacingX);
			int y = startY + row * spacingY;
			Button button = this.addRenderableWidget(Button.builder(Component.empty(), clicked -> {
				if (ScamRules.isRuleEnabled(rule)) {
					ScamRules.disableRule(rule);
				} else {
					ScamRules.enableRule(rule);
				}
				refreshRuleButtons();
			}).bounds(x, y, buttonWidth, 20).build());
			ruleButtons.put(rule, button);
		}

		addBackButton(160);
		refreshRuleButtons();
	}

	private void refreshRuleButtons() {
		for (Map.Entry<ScamRules.ScamRule, Button> entry : ruleButtons.entrySet()) {
			ScamRules.ScamRule rule = entry.getKey();
			boolean enabled = ScamRules.isRuleEnabled(rule);
			entry.getValue().setMessage(Component.literal(readableRuleName(rule) + ": ").append(onOffComponent(enabled)));
		}
	}

	private static String readableRuleName(ScamRules.ScamRule rule) {
		return switch (rule) {
			case SUSPICIOUS_LINK -> "Suspicious Link";
			case PRESSURE_AND_URGENCY -> "Pressure/Urgency";
			case UPFRONT_PAYMENT -> "Upfront Payment";
			case ACCOUNT_DATA_REQUEST -> "Account Data";
			case EXTERNAL_PLATFORM_PUSH -> "External Platform";
			case DISCORD_HANDLE -> "Discord Handle";
			case FAKE_MIDDLEMAN_CLAIM -> "Fake Middleman";
			case TOO_GOOD_TO_BE_TRUE -> "Too Good To Be True";
			case TRUST_MANIPULATION -> "Trust Manipulation";
			case SPAMMY_CONTACT_PATTERN -> "Spam Contact";
			case MULTI_MESSAGE_PATTERN -> "Multi Message";
			case FUNNEL_SEQUENCE_PATTERN -> "Funnel Sequence";
			case SIMILARITY_MATCH -> "Similarity Match";
			case LOCAL_AI_RISK_SIGNAL -> "Local AI Signal";
			case LOCAL_AI_FUNNEL_SIGNAL -> "Local AI Funnel Signal";
		};
	}
}
