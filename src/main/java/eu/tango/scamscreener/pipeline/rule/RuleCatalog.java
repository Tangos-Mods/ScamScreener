package eu.tango.scamscreener.pipeline.rule;

import eu.tango.scamscreener.chat.TextNormalization;
import eu.tango.scamscreener.config.data.RulesConfig;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Central compiled rule catalog shared by deterministic rule-driven stages.
 *
 * <p>This keeps the v1-style rule families defined once from {@link RulesConfig}
 * and lets multiple stages reuse the same initialized rule objects.
 */
@Getter
@Accessors(fluent = true)
public final class RuleCatalog {
    private final MuteRules mute;
    private final Rule suspiciousLink;
    private final Rule externalPlatform;
    private final Rule upfrontPayment;
    private final Rule accountData;
    private final Rule tooGood;
    private final Rule coercionThreat;
    private final Rule urgencyAllowlist;
    private final Rule tradeContextAllowlist;
    private final Rule trustAllowlist;
    private final Rule middleman;
    private final Rule middlemanClaim;
    private final Rule proofBait;
    private final Rule urgency;
    private final Rule trust;
    private final Rule trustSignal;
    private final Rule discordHandle;
    private final Rule linkRedirectCombo;
    private final Rule trustPaymentCombo;
    private final Rule urgencyAccountCombo;
    private final Rule middlemanProofCombo;
    private final int minCompareLength;
    private final List<SimilarityRule> similarityRules;
    private final BehaviorRules behavior;
    private final TrendRules trend;
    private final FunnelRules funnel;

    /**
     * Builds the shared compiled rule catalog from one rules config.
     *
     * @param rulesConfig the config backing deterministic rule definitions
     */
    public RuleCatalog(RulesConfig rulesConfig) {
        RulesConfig safeConfig = rulesConfig == null ? new RulesConfig() : rulesConfig;
        RulesConfig.MuteStageSettings muteSettings = safeConfig.muteStage();
        mute = new MuteRules(
            compileOptionalPattern(muteSettings.getHarmlessMessagePattern()),
            Math.max(0, muteSettings.getHarmlessMessageMaxLength()),
            Math.max(0L, muteSettings.getDuplicateWindowMs()),
            Math.max(0, muteSettings.getDuplicateCacheSize()),
            Math.max(0, muteSettings.getDuplicateMaxMessageLength())
        );

        RulesConfig.RuleStageSettings settings = safeConfig.ruleStage();

        suspiciousLink = Rule.pattern(
            "SUSPICIOUS_LINK",
            settings.getSuspiciousLinkPattern(),
            settings.getSuspiciousLinkScore(),
            "Suspicious link: \"%s\""
        );
        externalPlatform = Rule.pattern(
            "EXTERNAL_PLATFORM",
            settings.getExternalPlatformPattern(),
            settings.getExternalPlatformScore(),
            "External platform push: \"%s\""
        );
        upfrontPayment = Rule.pattern(
            "UPFRONT_PAYMENT",
            settings.getUpfrontPaymentPattern(),
            settings.getUpfrontPaymentScore(),
            "Upfront payment wording: \"%s\""
        );
        accountData = Rule.pattern(
            "ACCOUNT_DATA",
            settings.getAccountDataPattern(),
            settings.getAccountDataScore(),
            "Sensitive account wording: \"%s\""
        );
        tooGood = Rule.pattern(
            "TOO_GOOD",
            settings.getTooGoodPattern(),
            settings.getTooGoodScore(),
            "Too-good-to-be-true wording: \"%s\""
        );
        coercionThreat = Rule.pattern(
            "COERCION_THREAT",
            settings.getCoercionThreatPattern(),
            settings.getCoercionThreatScore(),
            "Coercion or extortion wording: \"%s\""
        );
        urgencyAllowlist = Rule.pattern("URGENCY_ALLOWLIST", settings.getUrgencyAllowlistPattern(), 0);
        tradeContextAllowlist = Rule.pattern("TRADE_CONTEXT_ALLOWLIST", settings.getTradeContextAllowlistPattern(), 0);
        trustAllowlist = Rule.pattern("TRUST_ALLOWLIST", settings.getTrustAllowlistPattern(), 0);
        middleman = Rule.pattern("MIDDLEMAN", settings.getMiddlemanPattern(), 0);
        middlemanClaim = Rule.pattern(
            "MIDDLEMAN_CLAIM",
            settings.getMiddlemanClaimPattern(),
            settings.getMiddlemanClaimScore(),
            "Middleman claim: \"%s\""
        );
        proofBait = Rule.pattern(
            "PROOF_BAIT",
            settings.getProofBaitPattern(),
            settings.getProofBaitScore(),
            "Proof or vouch bait: \"%s\""
        );
        urgency = Rule.phrase(
            "URGENCY",
            settings.urgencyKeywords(),
            settings.urgencyPhrases(),
            settings.getUrgencyScore(),
            settings.getUrgencyThreshold(),
            "Urgency wording: \"%s\""
        );
        trust = Rule.phrase(
            "TRUST",
            settings.trustKeywords(),
            settings.trustPhrases(),
            settings.getTrustScore(),
            settings.getTrustThreshold(),
            "Trust manipulation wording: \"%s\""
        );
        trustSignal = Rule.pattern("TRUST_SIGNAL", settings.getTrustSignalPattern(), 0);
        discordHandle = Rule.pattern(
            "DISCORD_HANDLE",
            settings.getDiscordHandlePattern(),
            settings.getDiscordHandleScore(),
            "Discord handle with platform mention: \"%s\""
        );
        linkRedirectCombo = Rule.fixed(
            "LINK_REDIRECT_COMBO",
            settings.getLinkRedirectComboScore(),
            "Link plus off-platform redirect"
        );
        trustPaymentCombo = Rule.fixed(
            "TRUST_PAYMENT_COMBO",
            settings.getTrustPaymentComboScore(),
            "Trust framing plus upfront payment"
        );
        urgencyAccountCombo = Rule.fixed(
            "URGENCY_ACCOUNT_COMBO",
            settings.getUrgencyAccountComboScore(),
            "Urgency paired with sensitive account request"
        );
        middlemanProofCombo = Rule.fixed(
            "MIDDLEMAN_PROOF_COMBO",
            settings.getMiddlemanProofComboScore(),
            "Middleman claim plus proof bait"
        );

        RulesConfig.SimilarityStageSettings similaritySettings = safeConfig.similarityStage();
        minCompareLength = Math.max(1, similaritySettings.getMinCompareLength());
        similarityRules = buildSimilarityRules(similaritySettings.phrases());

        RulesConfig.BehaviorStageSettings behaviorSettings = safeConfig.behaviorStage();
        behavior = new BehaviorRules(
            Math.max(0, behaviorSettings.getMinRepeatMessageLength()),
            Math.max(0, behaviorSettings.getMinBurstMessageLength()),
            Math.max(0, behaviorSettings.getRepeatedMessageThreshold()),
            Math.max(0, behaviorSettings.getRepeatedMessageScore()),
            Math.max(0, behaviorSettings.getBurstContactThreshold()),
            Math.max(0, behaviorSettings.getBurstContactScore()),
            Math.max(1, behaviorSettings.getComboBonusMinimum()),
            Math.max(1, behaviorSettings.getComboBonusDivisor()),
            Math.max(1L, behaviorSettings.getWindowMs()),
            Math.max(1, behaviorSettings.getMaxHistory())
        );

        RulesConfig.TrendStageSettings trendSettings = safeConfig.trendStage();
        trend = new TrendRules(
            Math.max(0, trendSettings.getMinTrendMessageLength()),
            Math.max(0, trendSettings.getSingleSenderRepeatScore()),
            Math.max(0, trendSettings.getMultiSenderWaveScore()),
            Math.max(1, trendSettings.getMultiSenderWaveThreshold()),
            Math.max(1, trendSettings.getEscalationBonusMinimum()),
            Math.max(1, trendSettings.getEscalationBonusDivisor()),
            Math.max(1L, trendSettings.getWindowMs()),
            Math.max(1, trendSettings.getMaxHistory())
        );

        RulesConfig.FunnelStageSettings funnelSettings = safeConfig.funnelStage();
        funnel = new FunnelRules(
            Math.max(0, funnelSettings.getExternalAfterContactScore()),
            Math.max(0, funnelSettings.getPaymentAfterExternalScore()),
            Math.max(0, funnelSettings.getPaymentAfterTrustScore()),
            Math.max(0, funnelSettings.getAccountAfterExternalScore()),
            Math.max(0, funnelSettings.getAccountAfterTrustScore()),
            Math.max(0, funnelSettings.getFullChainBonusScore()),
            Math.max(1, funnelSettings.getTrustBridgeBonusMinimum()),
            Math.max(1, funnelSettings.getTrustBridgeBonusDivisor()),
            Math.max(1L, funnelSettings.getWindowMs()),
            Math.max(1, funnelSettings.getMaxHistory())
        );
    }

    private static List<SimilarityRule> buildSimilarityRules(List<RulesConfig.SimilarityPhrase> configuredPhrases) {
        List<SimilarityRule> entries = new ArrayList<>();
        if (configuredPhrases == null || configuredPhrases.isEmpty()) {
            return List.of();
        }

        for (RulesConfig.SimilarityPhrase configuredPhrase : configuredPhrases) {
            if (configuredPhrase == null) {
                continue;
            }

            String category = sanitizeCategory(configuredPhrase.getCategory());
            String rawPhrase = configuredPhrase.getPhrase() == null ? "" : configuredPhrase.getPhrase().trim();
            if (category.isBlank() || rawPhrase.isBlank()) {
                continue;
            }

            String normalizedPhrase = TextNormalization.normalizeForSimilarity(rawPhrase);
            if (normalizedPhrase.isBlank()) {
                continue;
            }

            int score = Math.max(0, configuredPhrase.getScore());
            double threshold = Math.max(0.0, Math.min(1.0, configuredPhrase.getThreshold()));
            int tokenCount = tokenize(normalizedPhrase).size();
            entries.add(new SimilarityRule(category, rawPhrase, normalizedPhrase, score, threshold, tokenCount));
        }

        return List.copyOf(entries);
    }

    private static String sanitizeCategory(String rawCategory) {
        if (rawCategory == null) {
            return "";
        }

        return rawCategory.trim();
    }

    private static List<String> tokenize(String normalizedMessage) {
        if (normalizedMessage.isBlank()) {
            return List.of();
        }

        String[] rawTokens = normalizedMessage.split("\\s+");
        List<String> tokens = new ArrayList<>(rawTokens.length);
        for (String token : rawTokens) {
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    private static Pattern compileOptionalPattern(String rawPattern) {
        if (rawPattern == null || rawPattern.isBlank()) {
            return null;
        }

        return Pattern.compile(rawPattern);
    }
}
