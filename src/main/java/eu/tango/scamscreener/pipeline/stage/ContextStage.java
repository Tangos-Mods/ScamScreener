package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.chat.RecentChatCache;
import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.pipeline.rule.Rule;
import eu.tango.scamscreener.pipeline.rule.RuleCatalog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lightweight context-aware stage that scores signal blends across recent messages from the same sender.
 */
public final class ContextStage extends Stage {
    private final RecentChatCache recentChatCache;
    private final RuleCatalog rules;

    /**
     * Creates the context stage with a fresh cache handle and default rules config.
     *
     * @param recentChatCache the shared recent-chat cache
     */
    public ContextStage(RecentChatCache recentChatCache) {
        this(recentChatCache, new RulesConfig());
    }

    /**
     * Creates the context stage with a shared recent-chat cache and explicit rules config.
     *
     * @param recentChatCache the shared recent-chat cache
     * @param rulesConfig the rules config backing signal detection
     */
    public ContextStage(RecentChatCache recentChatCache, RulesConfig rulesConfig) {
        this(recentChatCache, new RuleCatalog(rulesConfig));
    }

    /**
     * Creates the context stage with a shared recent-chat cache and compiled rule catalog.
     *
     * @param recentChatCache the shared recent-chat cache
     * @param ruleCatalog the shared rule catalog
     */
    public ContextStage(RecentChatCache recentChatCache, RuleCatalog ruleCatalog) {
        this.recentChatCache = recentChatCache == null ? new RecentChatCache() : recentChatCache;
        this.rules = ruleCatalog == null ? new RuleCatalog(new RulesConfig()) : ruleCatalog;
    }

    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        if (!rules.contextStageEnabled()) {
            return pass();
        }
        if (chatEvent == null || !chatEvent.isPlayerSource()) {
            return pass();
        }

        String senderName = normalizeSenderName(chatEvent.getSenderName());
        if (senderName.isBlank()) {
            return pass();
        }

        RulesConfig.ContextStageSettings contextSettings = rules.context();
        List<String> senderMessages = recentMessagesForSender(senderName, contextSettings.getMaxContextMessages());
        if (senderMessages.size() < Math.max(1, contextSettings.getMinSenderMessages())) {
            return pass();
        }

        Set<String> signalKinds = new LinkedHashSet<>();
        int signalMessages = 0;
        for (String message : senderMessages) {
            Set<String> localSignals = classifySignals(message);
            if (!localSignals.isEmpty()) {
                signalKinds.addAll(localSignals);
                signalMessages++;
            }
        }

        if (signalMessages < Math.max(1, contextSettings.getMinSignalMessages())
            || signalKinds.size() < Math.max(1, contextSettings.getMinSignalKinds())) {
            return pass();
        }

        int score = Math.max(0, contextSettings.getSignalBlendScore());
        List<String> reasons = new ArrayList<>();
        List<String> reasonIds = new ArrayList<>();
        reasons.add("Context signal blend: " + String.join(" + ", signalKinds));
        reasonIds.add("context.signal_blend");
        if (signalKinds.size() >= Math.max(1, contextSettings.getEscalationMinSignalKinds())
            || signalMessages >= Math.max(1, contextSettings.getEscalationMinSignalMessages())) {
            score += Math.max(0, contextSettings.getEscalationBonusScore());
            reasons.add("Context escalation: " + signalKinds.size() + " signals across " + signalMessages + " messages");
            reasonIds.add("context.escalation");
        }
        return score(score, reasonIds, String.join("; ", reasons));
    }

    private List<String> recentMessagesForSender(String normalizedSenderName, int maxContextMessages) {
        int boundedMaxContextMessages = Math.max(1, maxContextMessages);
        List<String> messages = new ArrayList<>(boundedMaxContextMessages);
        for (RecentChatCache.CachedChatMessage entry : recentChatCache.entriesForSender(normalizedSenderName, boundedMaxContextMessages)) {
            messages.add(entry.cleanText().toLowerCase(Locale.ROOT));
            if (messages.size() >= boundedMaxContextMessages) {
                break;
            }
        }

        return List.copyOf(messages);
    }

    private Set<String> classifySignals(String message) {
        Set<String> signals = new LinkedHashSet<>();
        if (message == null || message.isBlank()) {
            return signals;
        }

        if (matchesPhraseRule(rules.trust(), message)) {
            signals.add("trust");
        }
        if (matchesPhraseRule(rules.urgency(), message)) {
            signals.add("urgency");
        }
        if (rules.externalPlatform().patternMatches(message)) {
            signals.add("external platform");
        }
        if (rules.upfrontPayment().patternMatches(message)) {
            signals.add("payment");
        }
        if (rules.accountData().patternMatches(message)) {
            signals.add("account data");
        }
        if (rules.coercionThreat().patternMatches(message)) {
            signals.add("threat");
        }
        if (rules.suspiciousLink().patternMatches(message)) {
            signals.add("suspicious link");
        }

        return signals;
    }

    private static boolean matchesPhraseRule(Rule rule, String message) {
        if (rule == null || message == null || message.isBlank()) {
            return false;
        }

        Rule.PhraseScore phraseScore = rule.phraseMatch(message);
        return phraseScore.score() >= rule.threshold();
    }

    private static String normalizeSenderName(String senderName) {
        if (senderName == null || senderName.isBlank()) {
            return "";
        }

        return senderName.trim().toLowerCase(Locale.ROOT);
    }
}
