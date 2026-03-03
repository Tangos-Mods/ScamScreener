package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.pipeline.rule.FunnelRules;
import eu.tango.scamscreener.pipeline.rule.RuleCatalog;
import eu.tango.scamscreener.pipeline.state.FunnelStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple sender-local sequence heuristics for multi-step risk progression.
 */
public final class FunnelStage extends Stage {
    private final FunnelStore funnelStore;
    private final RuleCatalog rules;

    /**
     * Creates the funnel stage with a default in-memory store.
     */
    public FunnelStage() {
        this(new FunnelStore(), new RulesConfig());
    }

    /**
     * Creates the funnel stage with an explicit shared store and default rules config.
     *
     * @param funnelStore the shared sender-local funnel store
     */
    public FunnelStage(FunnelStore funnelStore) {
        this(funnelStore, new RulesConfig());
    }

    /**
     * Creates the funnel stage with a default in-memory store and explicit rules config.
     *
     * @param rulesConfig the config backing funnel scores
     */
    public FunnelStage(RulesConfig rulesConfig) {
        this(new FunnelStore(), rulesConfig);
    }

    /**
     * Creates the funnel stage with an explicit shared store and rules config.
     *
     * @param funnelStore the shared sender-local funnel store
     * @param rulesConfig the config backing funnel scores
     */
    public FunnelStage(FunnelStore funnelStore, RulesConfig rulesConfig) {
        this(funnelStore, new RuleCatalog(rulesConfig));
    }

    /**
     * Creates the funnel stage with an explicit shared store and compiled rule catalog.
     *
     * @param funnelStore the shared sender-local funnel store
     * @param ruleCatalog the shared deterministic rule catalog
     */
    public FunnelStage(FunnelStore funnelStore, RuleCatalog ruleCatalog) {
        rules = ruleCatalog == null ? new RuleCatalog(new RulesConfig()) : ruleCatalog;
        this.funnelStore = funnelStore == null ? new FunnelStore() : funnelStore;
        this.funnelStore.configure(rules.funnel().windowMs(), rules.funnel().maxHistory());
    }

    /**
     * Evaluates the current message as one funnel step within the sender-local sequence.
     *
     * @param chatEvent the chat event received from the client
     * @return a score-only result when the sequence indicates elevated risk
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        if (!rules.funnelStageEnabled()) {
            funnelStore.recordStep(chatEvent, FunnelStore.FunnelStep.MESSAGE, chatEvent.getNormalizedMessage());
            return pass();
        }

        FunnelStore.FunnelSnapshot snapshot = funnelStore.snapshotFor(chatEvent);
        if (!snapshot.hasSender()) {
            return pass();
        }

        FunnelStore.FunnelStep currentStep = classifyStep(chatEvent.getNormalizedMessage());
        List<FunnelStore.FunnelStep> previousSteps = snapshot.recentSteps();
        boolean hasPriorContact = !previousSteps.isEmpty();
        boolean hasTrust = previousSteps.contains(FunnelStore.FunnelStep.TRUST);
        boolean hasExternal = previousSteps.contains(FunnelStore.FunnelStep.EXTERNAL_PLATFORM);

        int totalScore = 0;
        List<String> reasonParts = new ArrayList<>();
        FunnelRules funnel = rules.funnel();

        if (currentStep == FunnelStore.FunnelStep.EXTERNAL_PLATFORM && hasPriorContact) {
            totalScore += funnel.externalAfterContactScore();
            reasonParts.add(funnel.externalAfterContactReason());

            if (hasTrust) {
                totalScore += funnel.trustBridgeBonus();
                reasonParts.add(funnel.externalAfterTrustReason());
            }
        }

        if (currentStep == FunnelStore.FunnelStep.PAYMENT) {
            if (hasExternal) {
                totalScore += funnel.paymentAfterExternalScore();
                reasonParts.add(funnel.paymentAfterExternalReason());
            } else if (hasTrust) {
                totalScore += funnel.paymentAfterTrustScore();
                reasonParts.add(funnel.paymentAfterTrustReason());
            }
        }

        if (currentStep == FunnelStore.FunnelStep.ACCOUNT_DATA) {
            if (hasExternal) {
                totalScore += funnel.accountAfterExternalScore();
                reasonParts.add(funnel.accountAfterExternalReason());
            } else if (hasTrust) {
                totalScore += funnel.accountAfterTrustScore();
                reasonParts.add(funnel.accountAfterTrustReason());
            }
        }

        if ((currentStep == FunnelStore.FunnelStep.PAYMENT || currentStep == FunnelStore.FunnelStep.ACCOUNT_DATA)
            && hasTrust && hasExternal) {
            totalScore += funnel.fullChainBonusScore();
            reasonParts.add(funnel.fullChainReason());
        }

        // Record the classified step after evaluation so the snapshot remains pre-message.
        funnelStore.recordStep(chatEvent, currentStep, chatEvent.getNormalizedMessage());

        if (totalScore <= 0) {
            return pass();
        }

        return score(totalScore, String.join("; ", reasonParts));
    }

    private FunnelStore.FunnelStep classifyStep(String message) {
        String safeMessage = message == null ? "" : message;
        if (safeMessage.isBlank()) {
            return FunnelStore.FunnelStep.MESSAGE;
        }

        if (rules.accountData().patternMatches(safeMessage)) {
            return FunnelStore.FunnelStep.ACCOUNT_DATA;
        }
        if (rules.upfrontPayment().patternMatches(safeMessage)) {
            return FunnelStore.FunnelStep.PAYMENT;
        }
        if (rules.externalPlatform().patternMatches(safeMessage)) {
            return FunnelStore.FunnelStep.EXTERNAL_PLATFORM;
        }
        if (rules.trustSignal().patternMatches(safeMessage)) {
            return FunnelStore.FunnelStep.TRUST;
        }

        return FunnelStore.FunnelStep.MESSAGE;
    }
}
