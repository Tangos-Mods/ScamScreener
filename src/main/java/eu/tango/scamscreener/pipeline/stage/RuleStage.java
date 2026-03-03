package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.pipeline.rule.Rule;
import eu.tango.scamscreener.pipeline.rule.RuleCatalog;

import java.util.ArrayList;
import java.util.List;

/**
 * Core rule-based detection stage for exact and regex-style matches.
 *
 * <p>This is the first v2 port of the old v1 rule logic. The current version
 * uses an injected rules config so the pattern set and scores can be tuned
 * without hardcoding everything directly inside the stage class.
 */
public final class RuleStage extends Stage {
    private final RuleCatalog rules;

    /**
     * Creates the rule stage with the built-in default rules config.
     */
    public RuleStage() {
        this(new RulesConfig());
    }

    /**
     * Creates the rule stage with an explicit rules config.
     *
     * @param rulesConfig the config backing deterministic rule checks
     */
    public RuleStage(RulesConfig rulesConfig) {
        this(new RuleCatalog(rulesConfig));
    }

    /**
     * Creates the rule stage with an explicit compiled rule catalog.
     *
     * @param ruleCatalog the shared deterministic rule catalog
     */
    public RuleStage(RuleCatalog ruleCatalog) {
        rules = ruleCatalog == null ? new RuleCatalog(new RulesConfig()) : ruleCatalog;
    }

    /**
     * Evaluates deterministic rule checks.
     *
     * @param chatEvent the chat event received from the client
     * @return a score-only result when one or more rules matched
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        if (!rules.ruleStageEnabled()) {
            return pass();
        }

        String message = chatEvent.getNormalizedMessage();
        if (message.isBlank()) {
            return pass();
        }

        int totalScore = 0;
        List<String> reasonParts = new ArrayList<>();

        String linkMatch = rules.suspiciousLink().firstPatternMatch(message);
        if (linkMatch != null) {
            totalScore += rules.suspiciousLink().score();
            reasonParts.add(rules.suspiciousLink().reason(linkMatch));
        }

        String externalPlatformMatch = rules.externalPlatform().firstPatternMatch(message);
        if (externalPlatformMatch != null) {
            totalScore += rules.externalPlatform().score();
            reasonParts.add(rules.externalPlatform().reason(externalPlatformMatch));
        }

        String paymentMatch = rules.upfrontPayment().firstPatternMatch(message);
        if (paymentMatch != null) {
            totalScore += rules.upfrontPayment().score();
            reasonParts.add(rules.upfrontPayment().reason(paymentMatch));
        }

        String accountMatch = rules.accountData().firstPatternMatch(message);
        if (accountMatch != null) {
            totalScore += rules.accountData().score();
            reasonParts.add(rules.accountData().reason(accountMatch));
        }

        String tooGoodMatch = rules.tooGood().firstPatternMatch(message);
        if (tooGoodMatch != null) {
            totalScore += rules.tooGood().score();
            reasonParts.add(rules.tooGood().reason(tooGoodMatch));
        }

        String coercionThreatMatch = rules.coercionThreat().firstPatternMatch(message);
        if (coercionThreatMatch != null) {
            totalScore += rules.coercionThreat().score();
            reasonParts.add(rules.coercionThreat().reason(coercionThreatMatch));
        }

        String middlemanMatch = rules.middleman().firstPatternMatch(message);
        String middlemanClaimMatch = rules.middlemanClaim().firstPatternMatch(message);
        if (middlemanClaimMatch != null) {
            totalScore += rules.middlemanClaim().score();
            reasonParts.add(rules.middlemanClaim().reason(middlemanClaimMatch));
        }

        String proofBaitMatch = rules.proofBait().firstPatternMatch(message);
        if (proofBaitMatch != null) {
            totalScore += rules.proofBait().score();
            reasonParts.add(rules.proofBait().reason(proofBaitMatch));
        }

        boolean riskContext = linkMatch != null
            || externalPlatformMatch != null
            || paymentMatch != null
            || accountMatch != null
            || tooGoodMatch != null
            || coercionThreatMatch != null
            || middlemanMatch != null
            || middlemanClaimMatch != null
            || proofBaitMatch != null;

        Rule.PhraseScore urgency = rules.urgency().phraseMatch(message);
        boolean urgencyAllowlisted = rules.urgencyAllowlist().patternMatches(message);
        boolean tradeContextAllowlisted = rules.tradeContextAllowlist().patternMatches(message);
        boolean urgencyTriggered = false;
        if (coercionThreatMatch == null
            && urgency.score() >= rules.urgency().threshold()
            && !(urgencyAllowlisted && !riskContext)
            && !(tradeContextAllowlisted && !riskContext)) {
            totalScore += rules.urgency().score();
            reasonParts.add(rules.urgency().reason(urgency.match()));
            urgencyTriggered = true;
        }

        Rule.PhraseScore trust = rules.trust().phraseMatch(message);
        boolean trustAllowlisted = rules.trustAllowlist().patternMatches(message);
        boolean trustTriggered = false;
        if (trust.score() >= rules.trust().threshold()
            && !(trustAllowlisted && !riskContext)) {
            totalScore += rules.trust().score();
            reasonParts.add(rules.trust().reason(trust.match()));
            trustTriggered = true;
        }

        String discordHandleMatch = rules.discordHandle().firstPatternMatch(message);
        if (externalPlatformMatch != null && discordHandleMatch != null) {
            totalScore += rules.discordHandle().score();
            reasonParts.add(rules.discordHandle().reason(discordHandleMatch));
        }

        if (linkMatch != null && externalPlatformMatch != null) {
            totalScore += rules.linkRedirectCombo().score();
            reasonParts.add(rules.linkRedirectCombo().reason(null));
        }

        if (trustTriggered && paymentMatch != null) {
            totalScore += rules.trustPaymentCombo().score();
            reasonParts.add(rules.trustPaymentCombo().reason(null));
        }

        if (urgencyTriggered && accountMatch != null) {
            totalScore += rules.urgencyAccountCombo().score();
            reasonParts.add(rules.urgencyAccountCombo().reason(null));
        }

        if ((middlemanMatch != null || middlemanClaimMatch != null) && proofBaitMatch != null) {
            totalScore += rules.middlemanProofCombo().score();
            reasonParts.add(rules.middlemanProofCombo().reason(null));
        }

        if (totalScore <= 0 || reasonParts.isEmpty()) {
            return pass();
        }

        return score(totalScore, String.join("; ", reasonParts));
    }
}
