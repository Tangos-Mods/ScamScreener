package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.pipeline.rule.RuleCatalog;
import eu.tango.scamscreener.pipeline.rule.TrendRules;
import eu.tango.scamscreener.pipeline.state.TrendStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-message trend heuristics based on short global history.
 */
public final class TrendStage extends Stage {
    private static final String[] DIRECTED_EXTERNAL_REDIRECT_PHRASES = {
        "add me",
        "dm me",
        "message me",
        "contact me",
        "join my",
        "join vc",
        "join call",
        "voice chat",
        "voice channel"
    };

    private final TrendStore trendStore;
    private final RuleCatalog rules;

    /**
     * Creates the trend stage with a default in-memory store.
     */
    public TrendStage() {
        this(new TrendStore(), new RulesConfig());
    }

    /**
     * Creates the trend stage with an explicit shared store and default rules config.
     *
     * @param trendStore the shared cross-sender trend store
     */
    public TrendStage(TrendStore trendStore) {
        this(trendStore, new RulesConfig());
    }

    /**
     * Creates the trend stage with a default in-memory store and explicit rules config.
     *
     * @param rulesConfig the config backing trend thresholds and scores
     */
    public TrendStage(RulesConfig rulesConfig) {
        this(new TrendStore(), rulesConfig);
    }

    /**
     * Creates the trend stage with an explicit shared store and rules config.
     *
     * @param trendStore the shared cross-sender trend store
     * @param rulesConfig the config backing trend thresholds and scores
     */
    public TrendStage(TrendStore trendStore, RulesConfig rulesConfig) {
        this(trendStore, new RuleCatalog(rulesConfig));
    }

    /**
     * Creates the trend stage with an explicit shared store and compiled rule catalog.
     *
     * @param trendStore the shared cross-sender trend store
     * @param ruleCatalog the shared rule catalog
     */
    public TrendStage(TrendStore trendStore, RuleCatalog ruleCatalog) {
        rules = ruleCatalog == null ? new RuleCatalog(new RulesConfig()) : ruleCatalog;
        this.trendStore = trendStore == null ? new TrendStore() : trendStore;
        this.trendStore.configure(rules.trend().windowMs(), rules.trend().maxHistory());
    }

    /**
     * Evaluates recent cross-sender repeats for the current message.
     *
     * @param chatEvent the chat event received from the client
     * @return a score-only result when multiple senders repeat the same message
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        if (!rules.trendStageEnabled()) {
            trendStore.record(chatEvent);
            return pass();
        }

        TrendStore.TrendSnapshot snapshot = trendStore.snapshotFor(chatEvent);
        TrendRules trend = rules.trend();
        if (snapshot.normalizedMessage().isBlank() || snapshot.normalizedMessage().length() < trend.minMessageLength()) {
            trendStore.record(chatEvent);
            return pass();
        }
        if (!isTrendEligible(chatEvent)) {
            trendStore.record(chatEvent);
            return pass();
        }

        int totalScore = 0;
        List<String> reasonParts = new ArrayList<>();
        List<String> reasonIds = new ArrayList<>();

        if (snapshot.distinctSenderCount() >= trend.multiSenderWaveThreshold()) {
            totalScore += trend.multiSenderWaveScore();
            reasonParts.add(trend.waveReason(snapshot.distinctSenderCount()));
            reasonIds.add("trend.multi_sender_wave");
        } else if (snapshot.distinctSenderCount() > 0) {
            totalScore += snapshot.distinctSenderCount();
            reasonParts.add(trend.singleRepeatReason(snapshot.distinctSenderCount()));
            reasonIds.add("trend.single_cross_sender_repeat");
        }

        // Always record after evaluating so the snapshot represents the state before the current message.
        trendStore.record(chatEvent);

        if (totalScore <= 0) {
            return pass();
        }

        return score(totalScore, reasonIds, String.join("; ", reasonParts));
    }

    private boolean isTrendEligible(ChatEvent chatEvent) {
        if (chatEvent == null) {
            return false;
        }

        if (rules.suspiciousLink().patternMatches(chatEvent)
            || rules.upfrontPayment().patternMatches(chatEvent)
            || rules.coercionThreat().patternMatches(chatEvent)
            || rules.middlemanClaim().patternMatches(chatEvent)
            || rules.proofBait().patternMatches(chatEvent)
            || rules.trustSignal().patternMatches(chatEvent)) {
            return true;
        }

        if (!rules.externalPlatform().patternMatches(chatEvent)) {
            return false;
        }

        if (rules.discordHandle().patternMatches(chatEvent)) {
            return true;
        }

        String normalizedMessage = chatEvent.getNormalizedMessage();
        for (String phrase : DIRECTED_EXTERNAL_REDIRECT_PHRASES) {
            if (normalizedMessage.contains(phrase)) {
                return true;
            }
        }

        return false;
    }
}
