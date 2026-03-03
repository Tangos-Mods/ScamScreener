package eu.tango.scamscreener.gui.screen;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.gui.base.BaseScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Safe public rules screen with only simple enable/disable toggles.
 *
 * <p>The raw score and threshold tuning lives in the gated advanced screen.
 */
public final class RulesSettingsScreen extends BaseScreen {
    private static final int BUTTON_ROW_STEP = DEFAULT_BUTTON_HEIGHT + 4;

    private final Screen navigationParent;
    private final Section section;

    public RulesSettingsScreen(Screen parent) {
        this(parent, Section.STAGES);
    }

    private RulesSettingsScreen(Screen parent, Section section) {
        super(Text.literal("ScamScreener Rules"), parent);
        navigationParent = parent;
        this.section = section == null ? Section.STAGES : section;
    }

    @Override
    protected void init() {
        int contentWidth = rulesContentWidth();
        int x = centeredX(contentWidth);
        int columnWidth = splitWidth(contentWidth, 2, DEFAULT_SPLIT_GAP);
        int leftX = columnX(x, columnWidth, DEFAULT_SPLIT_GAP, 0);
        int rightX = columnX(x, columnWidth, DEFAULT_SPLIT_GAP, 1);
        int y = CONTENT_TOP + 20;

        List<ToggleAction> actions = buildActions();
        for (int index = 0; index < actions.size(); index++) {
            ToggleAction action = actions.get(index);
            int buttonX = (index % 2 == 0) ? leftX : rightX;
            int buttonY = y + ((index / 2) * BUTTON_ROW_STEP);
            addToggleButton(buttonX, buttonY, columnWidth, action);
        }

        int footerWidth = splitWidth(contentWidth, 4, DEFAULT_SPLIT_GAP);
        addFooterButton(
            columnX(x, footerWidth, DEFAULT_SPLIT_GAP, 0),
            footerWidth,
            Text.literal("< " + section.previous().label()),
            button -> openSection(section.previous())
        );
        addFooterButton(
            columnX(x, footerWidth, DEFAULT_SPLIT_GAP, 1),
            footerWidth,
            Text.literal(section.next().label() + " >"),
            button -> openSection(section.next())
        );
        addFooterButton(
            columnX(x, footerWidth, DEFAULT_SPLIT_GAP, 2),
            footerWidth,
            Text.literal("Advanced..."),
            button -> {
                if (this.client != null) {
                    this.client.setScreen(new AdvancedRulesWarningScreen(this));
                }
            }
        );
        addFooterButton(
            columnX(x, footerWidth, DEFAULT_SPLIT_GAP, 3),
            footerWidth,
            Text.literal("Back"),
            button -> close()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        super.render(context, mouseX, mouseY, deltaTicks);

        int left = centeredX(rulesContentWidth());
        int y = CONTENT_TOP - 18;

        drawSectionTitle(context, left, y, section.label());
        y += 12;
        drawLine(context, left, y, "This screen only enables or disables rules and triggers.");
        y += 12;
        drawLine(context, left, y, section.summary());
        y += 12;
        drawLine(context, left, y, "Use Advanced only if you are intentionally changing the pipeline internals.");
    }

    private List<ToggleAction> buildActions() {
        List<ToggleAction> actions = new ArrayList<>();
        switch (section) {
            case STAGES -> buildStageActions(actions);
            case CORE_TRIGGERS -> buildCoreTriggerActions(actions);
            case SOCIAL_TRIGGERS -> buildSocialTriggerActions(actions);
            case COMBOS -> buildComboActions(actions);
        }

        return List.copyOf(actions);
    }

    private void buildStageActions(List<ToggleAction> actions) {
        RulesConfig rules = rules();
        addToggleAction(actions, "Mute Stage: ", rules.isMuteStageEnabled(), () -> rules.setMuteStageEnabled(!rules.isMuteStageEnabled()));
        addToggleAction(actions, "Rule Stage: ", rules.isRuleStageEnabled(), () -> rules.setRuleStageEnabled(!rules.isRuleStageEnabled()));
        addToggleAction(actions, "Similarity Stage: ", rules.isSimilarityStageEnabled(), () -> rules.setSimilarityStageEnabled(!rules.isSimilarityStageEnabled()));
        addToggleAction(actions, "Behavior Stage: ", rules.isBehaviorStageEnabled(), () -> rules.setBehaviorStageEnabled(!rules.isBehaviorStageEnabled()));
        addToggleAction(actions, "Trend Stage: ", rules.isTrendStageEnabled(), () -> rules.setTrendStageEnabled(!rules.isTrendStageEnabled()));
        addToggleAction(actions, "Funnel Stage: ", rules.isFunnelStageEnabled(), () -> rules.setFunnelStageEnabled(!rules.isFunnelStageEnabled()));
    }

    private void buildCoreTriggerActions(List<ToggleAction> actions) {
        RulesConfig.RuleStageSettings settings = rules().ruleStage();
        addToggleAction(actions, "Suspicious Links: ", settings.isSuspiciousLinkEnabled(),
            () -> settings.setSuspiciousLinkEnabled(!settings.isSuspiciousLinkEnabled()));
        addToggleAction(actions, "External Platform: ", settings.isExternalPlatformEnabled(),
            () -> settings.setExternalPlatformEnabled(!settings.isExternalPlatformEnabled()));
        addToggleAction(actions, "Upfront Payment: ", settings.isUpfrontPaymentEnabled(),
            () -> settings.setUpfrontPaymentEnabled(!settings.isUpfrontPaymentEnabled()));
        addToggleAction(actions, "Account Data: ", settings.isAccountDataEnabled(),
            () -> settings.setAccountDataEnabled(!settings.isAccountDataEnabled()));
        addToggleAction(actions, "Too Good To Be True: ", settings.isTooGoodEnabled(),
            () -> settings.setTooGoodEnabled(!settings.isTooGoodEnabled()));
        addToggleAction(actions, "Coercion Threats: ", settings.isCoercionThreatEnabled(),
            () -> settings.setCoercionThreatEnabled(!settings.isCoercionThreatEnabled()));
    }

    private void buildSocialTriggerActions(List<ToggleAction> actions) {
        RulesConfig.RuleStageSettings settings = rules().ruleStage();
        addToggleAction(actions, "Middleman Trigger: ", settings.isMiddlemanTriggerEnabled(),
            () -> settings.setMiddlemanTriggerEnabled(!settings.isMiddlemanTriggerEnabled()));
        addToggleAction(actions, "Proof Bait: ", settings.isProofBaitEnabled(),
            () -> settings.setProofBaitEnabled(!settings.isProofBaitEnabled()));
        addToggleAction(actions, "Urgency Wording: ", settings.isUrgencyEnabled(),
            () -> settings.setUrgencyEnabled(!settings.isUrgencyEnabled()));
        addToggleAction(actions, "Trust Wording: ", settings.isTrustEnabled(),
            () -> settings.setTrustEnabled(!settings.isTrustEnabled()));
        addToggleAction(actions, "Discord Handle: ", settings.isDiscordHandleEnabled(),
            () -> settings.setDiscordHandleEnabled(!settings.isDiscordHandleEnabled()));
    }

    private void buildComboActions(List<ToggleAction> actions) {
        RulesConfig.RuleStageSettings settings = rules().ruleStage();
        addToggleAction(actions, "Link + Redirect Combo: ", settings.isLinkRedirectComboEnabled(),
            () -> settings.setLinkRedirectComboEnabled(!settings.isLinkRedirectComboEnabled()));
        addToggleAction(actions, "Trust + Payment Combo: ", settings.isTrustPaymentComboEnabled(),
            () -> settings.setTrustPaymentComboEnabled(!settings.isTrustPaymentComboEnabled()));
        addToggleAction(actions, "Urgency + Account Combo: ", settings.isUrgencyAccountComboEnabled(),
            () -> settings.setUrgencyAccountComboEnabled(!settings.isUrgencyAccountComboEnabled()));
        addToggleAction(actions, "Middleman + Proof Combo: ", settings.isMiddlemanProofComboEnabled(),
            () -> settings.setMiddlemanProofComboEnabled(!settings.isMiddlemanProofComboEnabled()));
    }

    private void addToggleAction(List<ToggleAction> actions, String labelPrefix, boolean enabled, Runnable onPress) {
        actions.add(new ToggleAction(labelPrefix, enabled, onPress));
    }

    private ButtonWidget addToggleButton(int x, int y, int width, ToggleAction action) {
        return addDrawableChild(
            ButtonWidget.builder(toggleText(action.labelPrefix(), action.enabled()), button -> applyRulesChange(action.onPress()))
                .dimensions(x, y, width, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
    }

    private void applyRulesChange(Runnable changeAction) {
        changeAction.run();
        ScamScreenerRuntime.getInstance().saveRules();
        openSection(section);
    }

    private void openSection(Section nextSection) {
        if (this.client != null) {
            this.client.setScreen(new RulesSettingsScreen(navigationParent, nextSection));
        }
    }

    private RulesConfig rules() {
        return ScamScreenerRuntime.getInstance().rules();
    }

    private int rulesContentWidth() {
        return Math.min(560, Math.max(320, this.width - 40));
    }

    private record ToggleAction(String labelPrefix, boolean enabled, Runnable onPress) {
    }

    private enum Section {
        STAGES("Stages", "Turn whole pipeline stages on or off without changing their tuning."),
        CORE_TRIGGERS("Core Triggers", "Basic high-signal trigger families for links, payment and account theft."),
        SOCIAL_TRIGGERS("Social Triggers", "Trust, middleman and social-engineering trigger families."),
        COMBOS("Combos", "Compound rule bonuses that only fire when multiple triggers align.");

        private static final Section[] VALUES = values();

        private final String label;
        private final String summary;

        Section(String label, String summary) {
            this.label = label;
            this.summary = summary;
        }

        public String label() {
            return label;
        }

        public String summary() {
            return summary;
        }

        public Section next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }

        public Section previous() {
            int previousIndex = ordinal() - 1;
            if (previousIndex < 0) {
                previousIndex = VALUES.length - 1;
            }

            return VALUES[previousIndex];
        }
    }
}
