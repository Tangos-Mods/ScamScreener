package eu.tango.scamscreener.pipeline.stage;

import eu.tango.scamscreener.config.data.RulesConfig;
import eu.tango.scamscreener.pipeline.core.Stage;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.pipeline.rule.MuteRules;
import eu.tango.scamscreener.pipeline.rule.RuleCatalog;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * First pipeline stage reserved for mute and suppression logic.
 */
public final class MuteStage extends Stage {
    private final RuleCatalog rules;
    private final Map<String, Long> recentDuplicateKeys = new LinkedHashMap<>();

    /**
     * Creates the mute stage with the built-in default rules config.
     */
    public MuteStage() {
        this(new RulesConfig());
    }

    /**
     * Creates the mute stage with an explicit rules config.
     *
     * @param rulesConfig the config backing early bypass rules
     */
    public MuteStage(RulesConfig rulesConfig) {
        this(new RuleCatalog(rulesConfig));
    }

    /**
     * Creates the mute stage with an explicit compiled rule catalog.
     *
     * @param ruleCatalog the shared rule catalog
     */
    public MuteStage(RuleCatalog ruleCatalog) {
        rules = ruleCatalog == null ? new RuleCatalog(new RulesConfig()) : ruleCatalog;
    }

    /**
     * Evaluates mute-specific rules.
     *
     * @param chatEvent the chat event received from the client
     * @return the stage result for this event
     */
    @Override
    protected StageResult evaluate(ChatEvent chatEvent) {
        if (!rules.muteStageEnabled()) {
            return pass();
        }

        MuteRules mute = rules.mute();
        if (chatEvent.isSystemSource()) {
            // System and NPC messages should bypass risk checks, not be treated as hidden chat.
            return allow(mute.systemBypassReason(), "mute.system_bypass");
        }

        String normalizedMessage = chatEvent.getNormalizedMessage();
        if (chatEvent.isPlayerSource() && mute.matchesHarmlessMessage(normalizedMessage)) {
            return allow(mute.harmlessBypassReason(), "mute.noise_bypass");
        }

        if (shouldBypassAsDuplicate(chatEvent, mute, normalizedMessage)) {
            return allow(mute.duplicateBypassReason(), "mute.duplicate_bypass");
        }

        // Player and unknown messages continue through the normal pipeline.
        return pass();
    }

    private synchronized boolean shouldBypassAsDuplicate(ChatEvent chatEvent, MuteRules mute, String normalizedMessage) {
        if (chatEvent == null || chatEvent.isPlayerSource() || !mute.isDuplicateCandidate(normalizedMessage)) {
            return false;
        }

        long nowMs = Math.max(0L, chatEvent.getTimestampMs());
        pruneExpiredDuplicateKeys(nowMs, mute);
        String duplicateKey = duplicateKey(chatEvent, normalizedMessage);
        if (duplicateKey.isBlank()) {
            return false;
        }

        Long previousTimestamp = recentDuplicateKeys.get(duplicateKey);
        recentDuplicateKeys.put(duplicateKey, nowMs);
        trimDuplicateCache(mute);
        return previousTimestamp != null && (nowMs - previousTimestamp) <= mute.duplicateWindowMs();
    }

    private void pruneExpiredDuplicateKeys(long nowMs, MuteRules mute) {
        if (recentDuplicateKeys.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<String, Long>> iterator = recentDuplicateKeys.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (nowMs - entry.getValue() > mute.duplicateWindowMs()) {
                iterator.remove();
            }
        }
    }

    private void trimDuplicateCache(MuteRules mute) {
        while (recentDuplicateKeys.size() > mute.duplicateCacheSize()) {
            Iterator<String> iterator = recentDuplicateKeys.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private static String duplicateKey(ChatEvent chatEvent, String normalizedMessage) {
        if (normalizedMessage == null || normalizedMessage.isBlank()) {
            return "";
        }

        UUID senderUuid = chatEvent.getSenderUuid();
        if (senderUuid != null) {
            return senderUuid + "|" + normalizedMessage;
        }

        String senderName = chatEvent.getSenderName();
        if (senderName != null && !senderName.isBlank()) {
            return senderName.trim().toLowerCase(Locale.ROOT) + "|" + normalizedMessage;
        }

        return "ANON|" + normalizedMessage;
    }
}
