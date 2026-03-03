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
 * Advanced low-level rule tuning screen.
 *
 * <p>This keeps the raw weight and threshold controls behind an explicit
 * confirmation screen because careless changes can destabilize the pipeline.
 */
public final class AdvancedRulesSettingsScreen extends BaseScreen {
    private static final int MAX_SCORE = 60;
    private static final int SCORE_STEP = 5;
    private static final int MAX_MIN_LENGTH = 32;
    private static final int MAX_THRESHOLD = 8;
    private static final int MAX_STORE_HISTORY = 256;
    private static final long MIN_WINDOW_MS = 30_000L;
    private static final long MAX_WINDOW_MS = 600_000L;
    private static final long WINDOW_STEP_MS = 30_000L;
    private static final int BUTTON_ROW_STEP = DEFAULT_BUTTON_HEIGHT + 2;

    private final Screen navigationParent;
    private final Section section;

    public AdvancedRulesSettingsScreen(Screen parent) {
        this(parent, Section.RULE_STAGE);
    }

    private AdvancedRulesSettingsScreen(Screen parent, Section section) {
        super(Text.literal("ScamScreener Rules (Advanced)"), parent);
        navigationParent = parent;
        this.section = section == null ? Section.RULE_STAGE : section;
    }

    @Override
    protected void init() {
        int contentWidth = rulesContentWidth();
        int x = centeredX(contentWidth);
        int columnWidth = splitWidth(contentWidth, 2, DEFAULT_SPLIT_GAP);
        int leftX = columnX(x, columnWidth, DEFAULT_SPLIT_GAP, 0);
        int rightX = columnX(x, columnWidth, DEFAULT_SPLIT_GAP, 1);
        int y = CONTENT_TOP + 20;

        List<SettingAction> actions = buildActions();
        for (int index = 0; index < actions.size(); index++) {
            SettingAction action = actions.get(index);
            int buttonX = (index % 2 == 0) ? leftX : rightX;
            int buttonY = y + ((index / 2) * BUTTON_ROW_STEP);
            addSettingButton(buttonX, buttonY, columnWidth, action);
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
            Text.literal("Runtime"),
            button -> {
                if (this.client != null) {
                    this.client.setScreen(new RuntimeSettingsScreen(this));
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

        RulesConfig rules = rules();
        int left = centeredX(rulesContentWidth());
        int y = CONTENT_TOP - 18;

        drawSectionTitle(context, left, y, section.label());
        y += 12;
        drawLine(context, left, y, "Advanced tuning changes low-level scores, windows and thresholds.");
        y += 12;
        drawLine(context, left, y, "Bad values can weaken detection or break the pipeline.");
        y += 12;
        drawLine(context, left, y, "Similarity Phrases: " + rules.similarityStage().phrases().size());
    }

    private List<SettingAction> buildActions() {
        List<SettingAction> actions = new ArrayList<>();
        switch (section) {
            case MUTE -> buildMuteActions(actions);
            case RULE_STAGE -> buildRuleStageActions(actions);
            case SIMILARITY -> buildSimilarityActions(actions);
            case BEHAVIOR -> buildBehaviorActions(actions);
            case TREND -> buildTrendActions(actions);
            case FUNNEL -> buildFunnelActions(actions);
        }

        return List.copyOf(actions);
    }

    private void buildMuteActions(List<SettingAction> actions) {
        RulesConfig.MuteStageSettings muteStage = rules().muteStage();
        addAction(actions, "Harmless Max Len: " + muteStage.getHarmlessMessageMaxLength(),
            () -> muteStage.setHarmlessMessageMaxLength(nextCounter(muteStage.getHarmlessMessageMaxLength(), 0, MAX_MIN_LENGTH)));
        addAction(actions, "Duplicate Window: " + formatWindow(muteStage.getDuplicateWindowMs()),
            () -> muteStage.setDuplicateWindowMs(nextMuteWindowMs(muteStage.getDuplicateWindowMs())));
        addAction(actions, "Duplicate Cache: " + muteStage.getDuplicateCacheSize(),
            () -> muteStage.setDuplicateCacheSize(nextCounter(muteStage.getDuplicateCacheSize(), 0, MAX_STORE_HISTORY)));
        addAction(actions, "Duplicate Max Len: " + muteStage.getDuplicateMaxMessageLength(),
            () -> muteStage.setDuplicateMaxMessageLength(nextCounter(muteStage.getDuplicateMaxMessageLength(), 0, 256)));
    }

    private void buildRuleStageActions(List<SettingAction> actions) {
        RulesConfig.RuleStageSettings ruleStage = rules().ruleStage();
        addAction(actions, "Link Score: " + ruleStage.getSuspiciousLinkScore(),
            () -> ruleStage.setSuspiciousLinkScore(nextScore(ruleStage.getSuspiciousLinkScore())));
        addAction(actions, "External Score: " + ruleStage.getExternalPlatformScore(),
            () -> ruleStage.setExternalPlatformScore(nextScore(ruleStage.getExternalPlatformScore())));
        addAction(actions, "Payment Score: " + ruleStage.getUpfrontPaymentScore(),
            () -> ruleStage.setUpfrontPaymentScore(nextScore(ruleStage.getUpfrontPaymentScore())));
        addAction(actions, "Account Score: " + ruleStage.getAccountDataScore(),
            () -> ruleStage.setAccountDataScore(nextScore(ruleStage.getAccountDataScore())));
        addAction(actions, "Too Good Score: " + ruleStage.getTooGoodScore(),
            () -> ruleStage.setTooGoodScore(nextScore(ruleStage.getTooGoodScore())));
        addAction(actions, "Coercion Score: " + ruleStage.getCoercionThreatScore(),
            () -> ruleStage.setCoercionThreatScore(nextScore(ruleStage.getCoercionThreatScore())));
        addAction(actions, "Middleman Score: " + ruleStage.getMiddlemanClaimScore(),
            () -> ruleStage.setMiddlemanClaimScore(nextScore(ruleStage.getMiddlemanClaimScore())));
        addAction(actions, "Proof Bait Score: " + ruleStage.getProofBaitScore(),
            () -> ruleStage.setProofBaitScore(nextScore(ruleStage.getProofBaitScore())));
        addAction(actions, "Urgency Score: " + ruleStage.getUrgencyScore(),
            () -> ruleStage.setUrgencyScore(nextScore(ruleStage.getUrgencyScore())));
        addAction(actions, "Trust Score: " + ruleStage.getTrustScore(),
            () -> ruleStage.setTrustScore(nextScore(ruleStage.getTrustScore())));
        addAction(actions, "Handle Score: " + ruleStage.getDiscordHandleScore(),
            () -> ruleStage.setDiscordHandleScore(nextScore(ruleStage.getDiscordHandleScore())));
        addAction(actions, "Link+Redirect: " + ruleStage.getLinkRedirectComboScore(),
            () -> ruleStage.setLinkRedirectComboScore(nextScore(ruleStage.getLinkRedirectComboScore())));
        addAction(actions, "Trust+Payment: " + ruleStage.getTrustPaymentComboScore(),
            () -> ruleStage.setTrustPaymentComboScore(nextScore(ruleStage.getTrustPaymentComboScore())));
        addAction(actions, "Urgency+Account: " + ruleStage.getUrgencyAccountComboScore(),
            () -> ruleStage.setUrgencyAccountComboScore(nextScore(ruleStage.getUrgencyAccountComboScore())));
        addAction(actions, "MM+Proof: " + ruleStage.getMiddlemanProofComboScore(),
            () -> ruleStage.setMiddlemanProofComboScore(nextScore(ruleStage.getMiddlemanProofComboScore())));
        addAction(actions, "Urgency Hits: " + ruleStage.getUrgencyThreshold(),
            () -> ruleStage.setUrgencyThreshold(nextCounter(ruleStage.getUrgencyThreshold(), 1, MAX_THRESHOLD)));
        addAction(actions, "Trust Hits: " + ruleStage.getTrustThreshold(),
            () -> ruleStage.setTrustThreshold(nextCounter(ruleStage.getTrustThreshold(), 1, MAX_THRESHOLD)));
    }

    private void buildSimilarityActions(List<SettingAction> actions) {
        RulesConfig.SimilarityStageSettings similarityStage = rules().similarityStage();
        addAction(actions, "Min Compare Len: " + similarityStage.getMinCompareLength(),
            () -> similarityStage.setMinCompareLength(nextCounter(similarityStage.getMinCompareLength(), 1, MAX_MIN_LENGTH)));
    }

    private void buildBehaviorActions(List<SettingAction> actions) {
        RulesConfig.BehaviorStageSettings behaviorStage = rules().behaviorStage();
        addAction(actions, "Min Repeat Len: " + behaviorStage.getMinRepeatMessageLength(),
            () -> behaviorStage.setMinRepeatMessageLength(nextCounter(behaviorStage.getMinRepeatMessageLength(), 1, MAX_MIN_LENGTH)));
        addAction(actions, "Min Burst Len: " + behaviorStage.getMinBurstMessageLength(),
            () -> behaviorStage.setMinBurstMessageLength(nextCounter(behaviorStage.getMinBurstMessageLength(), 1, MAX_MIN_LENGTH)));
        addAction(actions, "Repeat Hits: " + behaviorStage.getRepeatedMessageThreshold(),
            () -> behaviorStage.setRepeatedMessageThreshold(nextCounter(behaviorStage.getRepeatedMessageThreshold(), 1, MAX_THRESHOLD)));
        addAction(actions, "Repeat Score: " + behaviorStage.getRepeatedMessageScore(),
            () -> behaviorStage.setRepeatedMessageScore(nextScore(behaviorStage.getRepeatedMessageScore())));
        addAction(actions, "Burst Hits: " + behaviorStage.getBurstContactThreshold(),
            () -> behaviorStage.setBurstContactThreshold(nextCounter(behaviorStage.getBurstContactThreshold(), 1, MAX_THRESHOLD)));
        addAction(actions, "Burst Score: " + behaviorStage.getBurstContactScore(),
            () -> behaviorStage.setBurstContactScore(nextScore(behaviorStage.getBurstContactScore())));
        addAction(actions, "Combo Min: " + behaviorStage.getComboBonusMinimum(),
            () -> behaviorStage.setComboBonusMinimum(nextCounter(behaviorStage.getComboBonusMinimum(), 0, MAX_THRESHOLD)));
        addAction(actions, "Combo Divisor: " + behaviorStage.getComboBonusDivisor(),
            () -> behaviorStage.setComboBonusDivisor(nextCounter(behaviorStage.getComboBonusDivisor(), 1, MAX_THRESHOLD)));
        addAction(actions, "Window: " + formatWindow(behaviorStage.getWindowMs()),
            () -> behaviorStage.setWindowMs(nextWindowMs(behaviorStage.getWindowMs())));
        addAction(actions, "History: " + behaviorStage.getMaxHistory(),
            () -> behaviorStage.setMaxHistory(nextCounter(behaviorStage.getMaxHistory(), 1, MAX_STORE_HISTORY)));
    }

    private void buildTrendActions(List<SettingAction> actions) {
        RulesConfig.TrendStageSettings trendStage = rules().trendStage();
        addAction(actions, "Min Trend Len: " + trendStage.getMinTrendMessageLength(),
            () -> trendStage.setMinTrendMessageLength(nextCounter(trendStage.getMinTrendMessageLength(), 1, MAX_MIN_LENGTH)));
        addAction(actions, "Repeat Score: " + trendStage.getSingleSenderRepeatScore(),
            () -> trendStage.setSingleSenderRepeatScore(nextScore(trendStage.getSingleSenderRepeatScore())));
        addAction(actions, "Wave Score: " + trendStage.getMultiSenderWaveScore(),
            () -> trendStage.setMultiSenderWaveScore(nextScore(trendStage.getMultiSenderWaveScore())));
        addAction(actions, "Wave Hits: " + trendStage.getMultiSenderWaveThreshold(),
            () -> trendStage.setMultiSenderWaveThreshold(nextCounter(trendStage.getMultiSenderWaveThreshold(), 1, MAX_THRESHOLD)));
        addAction(actions, "Escalation Min: " + trendStage.getEscalationBonusMinimum(),
            () -> trendStage.setEscalationBonusMinimum(nextCounter(trendStage.getEscalationBonusMinimum(), 1, MAX_THRESHOLD)));
        addAction(actions, "Escalation Div: " + trendStage.getEscalationBonusDivisor(),
            () -> trendStage.setEscalationBonusDivisor(nextCounter(trendStage.getEscalationBonusDivisor(), 1, MAX_THRESHOLD)));
        addAction(actions, "Window: " + formatWindow(trendStage.getWindowMs()),
            () -> trendStage.setWindowMs(nextWindowMs(trendStage.getWindowMs())));
        addAction(actions, "History: " + trendStage.getMaxHistory(),
            () -> trendStage.setMaxHistory(nextCounter(trendStage.getMaxHistory(), 1, MAX_STORE_HISTORY)));
    }

    private void buildFunnelActions(List<SettingAction> actions) {
        RulesConfig.FunnelStageSettings funnelStage = rules().funnelStage();
        addAction(actions, "Contact->Ext: " + funnelStage.getExternalAfterContactScore(),
            () -> funnelStage.setExternalAfterContactScore(nextScore(funnelStage.getExternalAfterContactScore())));
        addAction(actions, "Ext->Payment: " + funnelStage.getPaymentAfterExternalScore(),
            () -> funnelStage.setPaymentAfterExternalScore(nextScore(funnelStage.getPaymentAfterExternalScore())));
        addAction(actions, "Trust->Payment: " + funnelStage.getPaymentAfterTrustScore(),
            () -> funnelStage.setPaymentAfterTrustScore(nextScore(funnelStage.getPaymentAfterTrustScore())));
        addAction(actions, "Ext->Account: " + funnelStage.getAccountAfterExternalScore(),
            () -> funnelStage.setAccountAfterExternalScore(nextScore(funnelStage.getAccountAfterExternalScore())));
        addAction(actions, "Trust->Account: " + funnelStage.getAccountAfterTrustScore(),
            () -> funnelStage.setAccountAfterTrustScore(nextScore(funnelStage.getAccountAfterTrustScore())));
        addAction(actions, "Full Chain: " + funnelStage.getFullChainBonusScore(),
            () -> funnelStage.setFullChainBonusScore(nextScore(funnelStage.getFullChainBonusScore())));
        addAction(actions, "Bridge Min: " + funnelStage.getTrustBridgeBonusMinimum(),
            () -> funnelStage.setTrustBridgeBonusMinimum(nextCounter(funnelStage.getTrustBridgeBonusMinimum(), 1, MAX_THRESHOLD)));
        addAction(actions, "Bridge Div: " + funnelStage.getTrustBridgeBonusDivisor(),
            () -> funnelStage.setTrustBridgeBonusDivisor(nextCounter(funnelStage.getTrustBridgeBonusDivisor(), 1, MAX_THRESHOLD)));
        addAction(actions, "Window: " + formatWindow(funnelStage.getWindowMs()),
            () -> funnelStage.setWindowMs(nextWindowMs(funnelStage.getWindowMs())));
        addAction(actions, "History: " + funnelStage.getMaxHistory(),
            () -> funnelStage.setMaxHistory(nextCounter(funnelStage.getMaxHistory(), 1, MAX_STORE_HISTORY)));
    }

    private void addAction(List<SettingAction> actions, String label, Runnable onPress) {
        actions.add(new SettingAction(label, onPress));
    }

    private void openSection(Section nextSection) {
        if (this.client != null) {
            this.client.setScreen(new AdvancedRulesSettingsScreen(navigationParent, nextSection));
        }
    }

    private ButtonWidget addSettingButton(int x, int y, int width, SettingAction action) {
        return addDrawableChild(
            ButtonWidget.builder(Text.literal(action.label()), button -> applyRulesChange(action.onPress()))
                .dimensions(x, y, width, DEFAULT_BUTTON_HEIGHT)
                .build()
        );
    }

    private void applyRulesChange(Runnable changeAction) {
        changeAction.run();
        ScamScreenerRuntime.getInstance().saveRules();
        openSection(section);
    }

    private RulesConfig rules() {
        return ScamScreenerRuntime.getInstance().rules();
    }

    private int rulesContentWidth() {
        return Math.min(560, Math.max(320, this.width - 40));
    }

    private static int nextScore(int currentValue) {
        int safeCurrentValue = Math.max(0, currentValue);
        int nextValue = safeCurrentValue + SCORE_STEP;
        if (nextValue > MAX_SCORE) {
            return 0;
        }

        return nextValue;
    }

    private static int nextCounter(int currentValue, int minValue, int maxValue) {
        int safeMinValue = Math.max(0, minValue);
        int safeMaxValue = Math.max(safeMinValue, maxValue);
        int nextValue = Math.max(safeMinValue, currentValue) + 1;
        if (nextValue > safeMaxValue) {
            return safeMinValue;
        }

        return nextValue;
    }

    private static long nextWindowMs(long currentValue) {
        long safeCurrentValue = Math.max(MIN_WINDOW_MS, currentValue);
        long nextValue = safeCurrentValue + WINDOW_STEP_MS;
        if (nextValue > MAX_WINDOW_MS) {
            return MIN_WINDOW_MS;
        }

        return nextValue;
    }

    private static long nextMuteWindowMs(long currentValue) {
        if (currentValue <= 0L) {
            return MIN_WINDOW_MS;
        }

        long nextValue = currentValue + WINDOW_STEP_MS;
        if (nextValue > MAX_WINDOW_MS) {
            return 0L;
        }

        return nextValue;
    }

    private static String formatWindow(long windowMs) {
        return (Math.max(0L, windowMs) / 1000L) + "s";
    }

    private record SettingAction(String label, Runnable onPress) {
    }

    private enum Section {
        MUTE("Mute", "Early bypass rules for harmless chatter and duplicate noise."),
        RULE_STAGE("Rule Stage", "Deterministic rule scores and phrase thresholds."),
        SIMILARITY("Similarity", "Fuzzy phrase matching thresholds and phrase inventory."),
        BEHAVIOR("Behavior", "Sender-local repeat, burst and history-window heuristics."),
        TREND("Trend", "Cross-sender repeat waves and their history window."),
        FUNNEL("Funnel", "Sequence rules, bridge bonuses and sender-local funnel memory.");

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
