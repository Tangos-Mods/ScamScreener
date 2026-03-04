package eu.tango.scamscreener.config.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule-specific configuration loaded from {@code rules.json}.
 *
 * <p>This keeps the current deterministic and similarity phrase data outside
 * the stage classes so rule tuning can evolve without hardcoding everything in
 * the pipeline implementation.
 */
@Getter
@Setter
@NoArgsConstructor
public final class RulesConfig {
    private boolean muteStageEnabled = true;
    private boolean ruleStageEnabled = true;
    private boolean similarityStageEnabled = true;
    private boolean behaviorStageEnabled = true;
    private boolean trendStageEnabled = true;
    private boolean funnelStageEnabled = true;
    private boolean contextStageEnabled = true;
    private MuteStageSettings muteStage = new MuteStageSettings();
    private RuleStageSettings ruleStage = new RuleStageSettings();
    private SimilarityStageSettings similarityStage = new SimilarityStageSettings();
    private BehaviorStageSettings behaviorStage = new BehaviorStageSettings();
    private TrendStageSettings trendStage = new TrendStageSettings();
    private FunnelStageSettings funnelStage = new FunnelStageSettings();
    private ContextStageSettings contextStage = new ContextStageSettings();

    /**
     * Returns the normalized mute stage settings.
     *
     * @return non-null mute stage settings
     */
    public MuteStageSettings muteStage() {
        if (muteStage == null) {
            muteStage = new MuteStageSettings();
        }

        return muteStage;
    }

    /**
     * Returns the normalized deterministic rule settings.
     *
     * @return non-null rule stage settings
     */
    public RuleStageSettings ruleStage() {
        if (ruleStage == null) {
            ruleStage = new RuleStageSettings();
        }

        return ruleStage;
    }

    /**
     * Returns the normalized similarity rule settings.
     *
     * @return non-null similarity stage settings
     */
    public SimilarityStageSettings similarityStage() {
        if (similarityStage == null) {
            similarityStage = new SimilarityStageSettings();
        }

        return similarityStage;
    }

    /**
     * Returns the normalized behavior stage settings.
     *
     * @return non-null behavior stage settings
     */
    public BehaviorStageSettings behaviorStage() {
        if (behaviorStage == null) {
            behaviorStage = new BehaviorStageSettings();
        }

        return behaviorStage;
    }

    /**
     * Returns the normalized trend stage settings.
     *
     * @return non-null trend stage settings
     */
    public TrendStageSettings trendStage() {
        if (trendStage == null) {
            trendStage = new TrendStageSettings();
        }

        return trendStage;
    }

    /**
     * Returns the normalized funnel stage settings.
     *
     * @return non-null funnel stage settings
     */
    public FunnelStageSettings funnelStage() {
        if (funnelStage == null) {
            funnelStage = new FunnelStageSettings();
        }

        return funnelStage;
    }

    /**
     * Returns the normalized context stage settings.
     *
     * @return non-null context stage settings
     */
    public ContextStageSettings contextStage() {
        if (contextStage == null) {
            contextStage = new ContextStageSettings();
        }

        return contextStage;
    }

    /**
     * Deterministic rule configuration for {@code RuleStage}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class RuleStageSettings {
        private boolean suspiciousLinkEnabled = true;
        private boolean externalPlatformEnabled = true;
        private boolean upfrontPaymentEnabled = true;
        private boolean accountDataEnabled = true;
        private boolean tooGoodEnabled = true;
        private boolean coercionThreatEnabled = true;
        private boolean middlemanTriggerEnabled = true;
        private boolean proofBaitEnabled = true;
        private boolean urgencyEnabled = true;
        private boolean trustEnabled = true;
        private boolean discordHandleEnabled = true;
        private boolean linkRedirectComboEnabled = true;
        private boolean trustPaymentComboEnabled = true;
        private boolean urgencyAccountComboEnabled = true;
        private boolean middlemanProofComboEnabled = true;

        private int suspiciousLinkScore = 20;
        private int externalPlatformScore = 15;
        private int upfrontPaymentScore = 25;
        private int accountDataScore = 35;
        private int tooGoodScore = 15;
        private int coercionThreatScore = 20;
        private int middlemanClaimScore = 15;
        private int proofBaitScore = 10;
        private int urgencyScore = 10;
        private int trustScore = 10;
        private int discordHandleScore = 50;
        private int linkRedirectComboScore = 10;
        private int trustPaymentComboScore = 15;
        private int urgencyAccountComboScore = 15;
        private int middlemanProofComboScore = 10;
        private int urgencyThreshold = 2;
        private int trustThreshold = 2;

        private String suspiciousLinkPattern =
            "\\b(?:https?://\\S+|(?:discord\\.gg|dsc\\.gg|discord(?:app)?\\.com/invite|t\\.me|bit\\.ly|tinyurl\\.com|cutt\\.ly|lnk\\.bio|bio\\.link|grabify\\.link)/\\S+)";
        private String externalPlatformPattern =
            "\\b(?:discord|telegram|whatsapp|instagram|snap(?:chat)?|t\\.me|dm me|dm me on discord|direct message me|add me on discord|join my discord|message me on discord|contact me on discord|discord server|server invite|join vc|vc|voice chat|voice channel|call)\\b";
        private String upfrontPaymentPattern =
            "\\b(?:pay first|send first|payment first|upfront payment|pay upfront|vorkasse|send coins first|coins first|before i trade|before we trade)\\b";
        private String accountDataPattern =
            "\\b(?:password|passwort|2fa|verification code|auth code|login details|email login|microsoft account|backup code|security code|otp|one time code|account email)\\b";
        private String tooGoodPattern =
            "\\b(?:free rank|free coins|100% safe|100 percent safe|guaranteed|garantiert|dupe|duped items?|risk free|free skyblock coins|cheap coins)\\b";
        private String coercionThreatPattern =
            "\\b(?:you\\s+will\\s+not\\s+get\\s+(?:your|ur)\\s+(?:stuff|items?|armor|gear)\\s+back"
                + "|you\\s+won\\s+t\\s+get\\s+(?:your|ur)\\s+(?:stuff|items?|armor|gear)\\s+back"
                + "|well\\s+then\\s+you\\s+will\\s+not\\s+get\\s+(?:your|ur)\\s+(?:stuff|items?|armor|gear)\\s+back"
                + "|unless\\s+you\\s+(?:join|come)\\s+(?:vc|voice\\s+chat|voice\\s+channel|call)"
                + "|if\\s+you\\s+don\\s+t\\s+(?:join|come)\\s+(?:vc|voice\\s+chat|voice\\s+channel|call)"
                + "|or\\s+i\\s+keep\\s+(?:your|ur)\\s+(?:stuff|items?|armor|gear))\\b";
        private String urgencyAllowlistPattern = "\\b(?:auction|ah|flip|bin|bid|bidding)\\b";
        private String tradeContextAllowlistPattern = "\\b(?:sell|selling|buy|buying|trade|trading|price|coins?|payment|pay|lf|lb)\\b";
        private String trustAllowlistPattern = "\\b(?:guild|party|partying|coop|co-op|friend|friends|teammate|member|clan)\\b";
        private String middlemanPattern = "\\b(?:trusted middleman|legit middleman|middleman)\\b";
        private String middlemanClaimPattern =
            "\\b(?:(?:trusted|legit|safe|verified)\\s+middleman|middleman\\s+(?:here|available)|(?:i\\s+can|can)\\s+(?:be\\s+)?(?:your\\s+)?middleman|using\\s+(?:a\\s+)?middleman)\\b";
        private String proofBaitPattern =
            "\\b(?:i\\s+have\\s+proof|can\\s+show\\s+proof|can\\s+provide\\s+proof|have\\s+vouches?|lots?\\s+of\\s+vouches?|mm\\s+proof|rep\\s+proof)\\b";
        private String trustSignalPattern =
            "\\b(?:trust me|i am legit|its legit|it's legit|safe trade|trusted middleman|legit middleman|trusted seller|verified seller|you can trust me|i can be trusted)\\b";
        private String discordHandlePattern =
            "(?:\\b[a-z0-9._]{2,32}#\\d{4}\\b|@[a-z0-9._]{2,32}\\b)";

        private List<String> urgencyKeywords = new ArrayList<>(List.of(
            "now", "quick", "fast", "urgent", "asap", "immediately", "right", "sofort", "jetzt", "hurry", "soon"
        ));
        private List<String> urgencyPhrases = new ArrayList<>(List.of(
            "right now", "right away", "as soon as possible", "need it now", "need this now", "need this right now",
            "fast fast", "quick payment", "need it asap", "need asap", "hurry up", "dont wait"
        ));
        private List<String> trustKeywords = new ArrayList<>(List.of(
            "trust", "trusted", "legit", "safe", "verified", "vouched", "reputable", "middleman"
        ));
        private List<String> trustPhrases = new ArrayList<>(List.of(
            "trust me", "i am legit", "its legit", "it's legit", "safe trade", "trusted middleman", "legit middleman",
            "trusted seller", "verified seller", "you can trust me", "i can be trusted"
        ));

        /**
         * Returns a non-null urgency keyword list.
         *
         * @return normalized urgency keywords
         */
        public List<String> urgencyKeywords() {
            if (urgencyKeywords == null) {
                urgencyKeywords = new ArrayList<>();
            }

            return urgencyKeywords;
        }

        /**
         * Returns a non-null urgency phrase list.
         *
         * @return normalized urgency phrases
         */
        public List<String> urgencyPhrases() {
            if (urgencyPhrases == null) {
                urgencyPhrases = new ArrayList<>();
            }

            return urgencyPhrases;
        }

        /**
         * Returns a non-null trust keyword list.
         *
         * @return normalized trust keywords
         */
        public List<String> trustKeywords() {
            if (trustKeywords == null) {
                trustKeywords = new ArrayList<>();
            }

            return trustKeywords;
        }

        /**
         * Returns a non-null trust phrase list.
         *
         * @return normalized trust phrases
         */
        public List<String> trustPhrases() {
            if (trustPhrases == null) {
                trustPhrases = new ArrayList<>();
            }

            return trustPhrases;
        }
    }

    /**
     * Early bypass configuration for {@code MuteStage}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class MuteStageSettings {
        private int harmlessMessageMaxLength = 12;
        private long duplicateWindowMs = 5_000L;
        private int duplicateCacheSize = 128;
        private int duplicateMaxMessageLength = 96;
        private String harmlessMessagePattern =
            "^(?:gg|gf|gl|hf|wp|ty|thx|thanks|ok|okay|nice|lol|lmao|brb|afk|np|nope|yep|nah)$";
    }

    /**
     * Similarity configuration for {@code LevenshteinStage}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class SimilarityStageSettings {
        private int minCompareLength = 6;
        private List<SimilarityPhrase> phrases = new ArrayList<>(List.of(
            new SimilarityPhrase("SIM_EXTERNAL_PLATFORM", "add me on discord", 10, 0.72),
            new SimilarityPhrase("SIM_EXTERNAL_PLATFORM", "join my discord", 10, 0.72),
            new SimilarityPhrase("SIM_EXTERNAL_PLATFORM", "add me on telegram", 10, 0.74),
            new SimilarityPhrase("SIM_EXTERNAL_PLATFORM", "dm me on discord", 10, 0.74),
            new SimilarityPhrase("SIM_EXTERNAL_PLATFORM", "join vc", 8, 0.76),
            new SimilarityPhrase("SIM_EXTERNAL_PLATFORM", "voice chat", 8, 0.78),
            new SimilarityPhrase("SIM_UPFRONT_PAYMENT", "pay first", 12, 0.78),
            new SimilarityPhrase("SIM_UPFRONT_PAYMENT", "send first", 12, 0.78),
            new SimilarityPhrase("SIM_UPFRONT_PAYMENT", "pay upfront", 12, 0.76),
            new SimilarityPhrase("SIM_UPFRONT_PAYMENT", "send payment first", 12, 0.74),
            new SimilarityPhrase("SIM_ACCOUNT_DATA", "verification code", 14, 0.80),
            new SimilarityPhrase("SIM_ACCOUNT_DATA", "login details", 14, 0.80),
            new SimilarityPhrase("SIM_ACCOUNT_DATA", "microsoft account", 14, 0.78),
            new SimilarityPhrase("SIM_ACCOUNT_DATA", "auth code", 14, 0.80),
            new SimilarityPhrase("SIM_ACCOUNT_DATA", "email login", 14, 0.80),
            new SimilarityPhrase("SIM_TRUST_MANIPULATION", "trust me", 8, 0.82),
            new SimilarityPhrase("SIM_TRUST_MANIPULATION", "trusted middleman", 10, 0.76),
            new SimilarityPhrase("SIM_TRUST_MANIPULATION", "legit middleman", 10, 0.76),
            new SimilarityPhrase("SIM_TRUST_MANIPULATION", "trusted seller", 8, 0.78),
            new SimilarityPhrase("SIM_TOO_GOOD", "free rank", 8, 0.82),
            new SimilarityPhrase("SIM_TOO_GOOD", "free coins", 8, 0.82),
            new SimilarityPhrase("SIM_TOO_GOOD", "free skyblock coins", 10, 0.78),
            new SimilarityPhrase("SIM_TOO_GOOD", "100 safe", 8, 0.74),
            new SimilarityPhrase("SIM_URGENCY", "right now", 6, 0.82),
            new SimilarityPhrase("SIM_URGENCY", "right away", 6, 0.78),
            new SimilarityPhrase("SIM_URGENCY", "as soon as possible", 6, 0.74),
            new SimilarityPhrase("SIM_URGENCY", "need it asap", 6, 0.76)
        ));

        /**
         * Returns a non-null phrase list.
         *
         * @return normalized similarity phrases
         */
        public List<SimilarityPhrase> phrases() {
            if (phrases == null) {
                phrases = new ArrayList<>();
            }

            return phrases;
        }
    }

    /**
     * One configurable fuzzy phrase entry.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class SimilarityPhrase {
        private String category;
        private String phrase;
        private int score;
        private double threshold;

        /**
         * Creates one similarity phrase entry.
         *
         * @param category the logical category used in reason output
         * @param phrase the comparison phrase
         * @param score the score contributed by this phrase
         * @param threshold the minimum similarity threshold
         */
        public SimilarityPhrase(String category, String phrase, int score, double threshold) {
            this.category = category;
            this.phrase = phrase;
            this.score = score;
            this.threshold = threshold;
        }
    }

    /**
     * Behavior configuration for {@code BehaviorStage}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class BehaviorStageSettings {
        private int minRepeatMessageLength = 8;
        private int minBurstMessageLength = 4;
        private int repeatedMessageThreshold = 1;
        private int repeatedMessageScore = 1;
        private int burstContactThreshold = 3;
        private int burstContactScore = 1;
        private int comboBonusMinimum = 0;
        private int comboBonusDivisor = 2;
        private long windowMs = 90_000L;
        private int maxHistory = 8;
    }

    /**
     * Trend configuration for {@code TrendStage}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class TrendStageSettings {
        private int minTrendMessageLength = 6;
        private int singleSenderRepeatScore = 10;
        private int multiSenderWaveScore = 20;
        private int multiSenderWaveThreshold = 2;
        private int escalationBonusMinimum = 1;
        private int escalationBonusDivisor = 2;
        private long windowMs = 120_000L;
        private int maxHistory = 200;
    }

    /**
     * Funnel configuration for {@code FunnelStage}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class FunnelStageSettings {
        private int externalAfterContactScore = 8;
        private int paymentAfterExternalScore = 18;
        private int paymentAfterTrustScore = 10;
        private int accountAfterExternalScore = 22;
        private int accountAfterTrustScore = 14;
        private int fullChainBonusScore = 10;
        private int trustBridgeBonusMinimum = 1;
        private int trustBridgeBonusDivisor = 2;
        private long windowMs = 300_000L;
        private int maxHistory = 8;
    }

    /**
     * Context configuration for {@code ContextStage}.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static final class ContextStageSettings {
        private int signalBlendScore = 2;
        private int escalationBonusScore = 1;
        private int maxContextMessages = 6;
        private int minSenderMessages = 2;
        private int minSignalMessages = 2;
        private int minSignalKinds = 2;
        private int escalationMinSignalKinds = 3;
        private int escalationMinSignalMessages = 3;
    }
}
