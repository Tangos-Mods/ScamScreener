package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.pipeline.rule.BehaviorRules;
import eu.tango.scamscreener.pipeline.rule.RuleCatalog;
import eu.tango.scamscreener.pipeline.state.BehaviorStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Sender-specific behavior heuristics based on short local message history.
 */
public final class BehaviorStage extends Stage {
    private static final int MAX_BEHAVIOR_SPAM_SCORE = 1;

    private final BehaviorStore behaviorStore;
    private final RuleCatalog rules;

    /**
     * Creates the behavior stage with a default in-memory store.
     */
    public BehaviorStage() {
        this(new BehaviorStore(), new RulesConfig());
    }

    /**
     * Creates the behavior stage with an explicit shared store and default rules config.
     *
     * @param behaviorStore the shared sender-local behavior store
     */
    public BehaviorStage(BehaviorStore behaviorStore) {
        this(behaviorStore, new RulesConfig());
    }

    /**
     * Creates the behavior stage with a default in-memory store and explicit rules config.
     *
     * @param rulesConfig the config backing behavior thresholds and scores
     */
    public BehaviorStage(RulesConfig rulesConfig) {
        this(new BehaviorStore(), rulesConfig);
    }

    /**
     * Creates the behavior stage with an explicit shared store and rules config.
     *
     * @param behaviorStore the shared sender-local behavior store
     * @param rulesConfig the config backing behavior thresholds and scores
     */
    public BehaviorStage(BehaviorStore behaviorStore, RulesConfig rulesConfig) {
        this(behaviorStore, new RuleCatalog(rulesConfig));
    }

    /**
     * Creates the behavior stage with an explicit shared store and compiled rule catalog.
     *
     * @param behaviorStore the shared sender-local behavior store
     * @param ruleCatalog the shared rule catalog
     */
    public BehaviorStage(BehaviorStore behaviorStore, RuleCatalog ruleCatalog) {
        rules = ruleCatalog == null ? new RuleCatalog(new RulesConfig()) : ruleCatalog;
        this.behaviorStore = behaviorStore == null ? new BehaviorStore() : behaviorStore;
        this.behaviorStore.configure(rules.behavior().windowMs(), rules.behavior().maxHistory());
    }

    /**
     * Evaluates short sender-local history for repeated-contact patterns.
     *
     * @param chatEvent the chat event received from the client
     * @return a score-only result when repeated or bursty contact is detected
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        if (!rules.behaviorStageEnabled()) {
            behaviorStore.record(chatEvent);
            return pass();
        }

        BehaviorStore.BehaviorSnapshot snapshot = behaviorStore.snapshotFor(chatEvent);
        if (!snapshot.hasSender()) {
            return pass();
        }

        List<String> reasonParts = new ArrayList<>();
        String normalizedMessage = chatEvent.getNormalizedMessage();
        BehaviorRules behavior = rules.behavior();
        boolean repeatedTriggered = false;
        boolean burstTriggered = false;

        if (normalizedMessage.length() >= behavior.minRepeatMessageLength()
            && snapshot.sameMessageCount() >= behavior.repeatedMessageThreshold()) {
            repeatedTriggered = true;
            reasonParts.add(behavior.repeatedMessageReason(snapshot.sameMessageCount() + 1));
        }

        if (normalizedMessage.length() >= behavior.minBurstMessageLength()
            && snapshot.recentMessageCount() >= behavior.burstContactThreshold()) {
            burstTriggered = true;
            reasonParts.add(behavior.burstContactReason(snapshot.recentMessageCount() + 1));
        }

        if (repeatedTriggered && burstTriggered) {
            reasonParts.add(behavior.comboReason());
        }

        // Always record the current message so the next event sees updated sender history.
        behaviorStore.record(chatEvent);

        if (!repeatedTriggered && !burstTriggered) {
            return pass();
        }

        return score(MAX_BEHAVIOR_SPAM_SCORE, String.join("; ", reasonParts));
    }
}
