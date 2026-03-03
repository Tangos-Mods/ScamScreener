package eu.tango.scamscreener.message;

import eu.tango.scamscreener.ScamScreenerRuntime;
import eu.tango.scamscreener.chat.ChatLineClassifier;
import eu.tango.scamscreener.pipeline.data.ChatEvent;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import eu.tango.scamscreener.pipeline.data.PipelineDecision;
import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.pipeline.state.BehaviorStore;
import eu.tango.scamscreener.review.ReviewCaseMessage;
import eu.tango.scamscreener.review.ReviewEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Small in-memory registry for recent alert contexts referenced by chat action tags.
 */
public final class AlertContextRegistry {
    private static final int MAX_ENTRIES = 120;
    private static final Map<String, AlertContext> RECENT = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, AlertContext> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private AlertContextRegistry() {
    }

    /**
     * Registers one alert context and returns its stable action id.
     *
     * @param chatEvent the evaluated chat event
     * @param decision the final pipeline decision
     * @return the generated alert id
     */
    public static synchronized String register(ChatEvent chatEvent, PipelineDecision decision) {
        ChatEvent safeEvent = chatEvent == null ? ChatEvent.messageOnly("") : chatEvent;
        PipelineDecision safeDecision = decision == null
            ? new PipelineDecision(PipelineDecision.Outcome.IGNORE, 0, "", List.of(), List.of())
            : decision;

        List<String> capturedMessages = collectCapturedMessages(safeEvent);
        List<RuleDetail> ruleDetails = collectRuleDetails(safeDecision);
        List<ReviewCaseMessage> caseMessages = buildCaseMessages(capturedMessages, safeEvent.getRawMessage(), safeEvent.getSourceType());

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertContext context = new AlertContext(
            id,
            safeEvent.getSenderUuid(),
            safeEvent.getSenderName(),
            safeEvent.getRawMessage(),
            capturedMessages,
            caseMessages,
            safeDecision.getTotalScore(),
            safeDecision.getOutcome(),
            safeDecision.getDecidedByStage(),
            safeEvent.getTimestampMs(),
            safeDecision.getReasons(),
            ruleDetails,
            safeDecision.getStageResults(),
            findLinkedReviewEntryId(safeEvent, safeDecision)
        );
        RECENT.put(id, context);
        return id;
    }

    /**
     * Looks up one recent alert context.
     *
     * @param id the alert id
     * @return the matching alert context, when present
     */
    public static synchronized Optional<AlertContext> find(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(RECENT.get(id.trim()));
    }

    /**
     * Looks up the most recent alert context for one player name.
     *
     * @param playerName the target player name
     * @return the newest matching alert context, when present
     */
    public static synchronized Optional<AlertContext> findMostRecentForPlayer(String playerName) {
        String normalizedPlayerName = normalizePlayerName(playerName);
        if (normalizedPlayerName.isBlank()) {
            return Optional.empty();
        }

        AlertContext bestMatch = null;
        for (AlertContext context : RECENT.values()) {
            if (context == null) {
                continue;
            }
            if (!normalizedPlayerName.equals(normalizePlayerName(context.senderName()))) {
                continue;
            }
            if (bestMatch == null || context.capturedAtMs() > bestMatch.capturedAtMs()) {
                bestMatch = context;
            }
        }

        return Optional.ofNullable(bestMatch);
    }

    /**
     * Returns recent player-name suggestions from the alert registry.
     *
     * @return the deduplicated recent player names
     */
    public static synchronized List<String> recentPlayerNames() {
        Set<String> seen = new LinkedHashSet<>();
        List<String> names = new ArrayList<>();
        for (AlertContext context : RECENT.values()) {
            if (context == null) {
                continue;
            }

            String playerName = context.displayPlayerName();
            String normalizedPlayerName = normalizePlayerName(playerName);
            if (normalizedPlayerName.isBlank() || "unknown".equals(normalizedPlayerName)) {
                continue;
            }
            if (seen.add(normalizedPlayerName)) {
                names.add(playerName);
            }
        }

        return List.copyOf(names);
    }

    /**
     * Creates or reuses one alert-style info context for an exact review entry.
     *
     * @param reviewEntry the review entry to inspect
     * @return the matching or synthesized alert context
     */
    public static synchronized Optional<AlertContext> createReviewContext(ReviewEntry reviewEntry) {
        if (reviewEntry == null) {
            return Optional.empty();
        }

        List<RuleDetail> ruleDetails = collectRuleDetails(new PipelineDecision(
            PipelineDecision.Outcome.REVIEW,
            reviewEntry.getScore(),
            reviewEntry.getDecidedByStage(),
            reviewEntry.getStageResults(),
            reviewEntry.getReasons()
        ));

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertContext context = new AlertContext(
            id,
            reviewEntry.getSenderUuid(),
            reviewEntry.getSenderName(),
            reviewEntry.getMessage(),
            collectCapturedMessagesForPlayer(reviewEntry.getSenderUuid(), reviewEntry.getSenderName(), reviewEntry.getMessage()),
            reviewEntry.getCaseMessages(),
            reviewEntry.getScore(),
            PipelineDecision.Outcome.REVIEW,
            reviewEntry.getDecidedByStage(),
            reviewEntry.getCapturedAtMs(),
            reviewEntry.getReasons(),
            ruleDetails,
            reviewEntry.getStageResults(),
            reviewEntry.getId()
        );
        RECENT.put(id, context);
        return Optional.of(context);
    }

    /**
     * Creates a synthetic player review context when no live alert context exists.
     *
     * @param playerName the player name to review
     * @return the created context, when a non-blank player name was provided
     */
    public static synchronized Optional<AlertContext> createPlayerReviewContext(String playerName) {
        String safePlayerName = sanitizePlayerName(playerName);
        if (safePlayerName.isBlank()) {
            return Optional.empty();
        }

        Optional<AlertContext> recentMatch = findMostRecentForPlayer(safePlayerName);
        if (recentMatch.isPresent()) {
            return recentMatch;
        }

        ScamScreenerRuntime runtime = safeRuntime();
        List<ReviewEntry> reviewEntries = runtime == null ? List.of() : runtime.reviewStore().entries();
        ReviewEntry latestEntry = latestReviewEntryForPlayer(reviewEntries, safePlayerName);
        if (latestEntry != null) {
            return createReviewContext(latestEntry);
        }

        String id = UUID.randomUUID().toString().replace("-", "");
        AlertContext context = new AlertContext(
            id,
            null,
            safePlayerName,
            "",
            collectCapturedMessagesForPlayer(null, safePlayerName),
            List.of(),
            0,
            PipelineDecision.Outcome.IGNORE,
            "",
            System.currentTimeMillis(),
            List.of(),
            List.of(),
            List.of(),
            ""
        );
        RECENT.put(id, context);
        return Optional.of(context);
    }

    static synchronized void clear() {
        RECENT.clear();
    }

    private static String findLinkedReviewEntryId(ChatEvent chatEvent, PipelineDecision decision) {
        if (decision == null || decision.getOutcome() != PipelineDecision.Outcome.REVIEW) {
            return "";
        }

        ScamScreenerRuntime runtime = safeRuntime();
        if (runtime == null) {
            return "";
        }

        for (ReviewEntry entry : runtime.reviewStore().entries()) {
            if (entry == null) {
                continue;
            }
            if (entry.getCapturedAtMs() != chatEvent.getTimestampMs()) {
                continue;
            }
            if (!entry.getMessage().equals(chatEvent.getRawMessage())) {
                continue;
            }
            if (!entry.getSenderName().equals(chatEvent.getSenderName())) {
                continue;
            }
            if (entry.getScore() != Math.max(0, decision.getTotalScore())) {
                continue;
            }

            return entry.getId();
        }

        return "";
    }

    private static List<String> collectCapturedMessages(ChatEvent chatEvent) {
        if (chatEvent == null || !chatEvent.hasSender()) {
            return compactMessages(List.of(chatEvent == null ? "" : chatEvent.getRawMessage()));
        }

        return collectCapturedMessagesForPlayer(chatEvent.getSenderUuid(), chatEvent.getSenderName(), chatEvent.getRawMessage());
    }

    private static List<String> collectCapturedMessagesForPlayer(UUID senderUuid, String senderName) {
        return collectCapturedMessagesForPlayer(senderUuid, senderName, "");
    }

    private static List<String> collectCapturedMessagesForPlayer(UUID senderUuid, String senderName, String preferredMessage) {
        ScamScreenerRuntime runtime = safeRuntime();
        if (runtime == null) {
            return compactMessages(List.of(preferredMessage));
        }

        List<String> mergedMessages = new ArrayList<>();

        ChatEvent snapshotEvent = new ChatEvent(
            preferredMessage,
            senderUuid,
            sanitizePlayerName(senderName),
            System.currentTimeMillis(),
            ChatSourceType.PLAYER
        );
        BehaviorStore.BehaviorSnapshot behaviorSnapshot = runtime.behaviorStore().snapshotFor(snapshotEvent);
        mergedMessages.addAll(behaviorSnapshot.recentMessages());

        List<ReviewEntry> matchingReviewEntries = matchingReviewEntries(runtime.reviewStore().entries(), senderUuid, senderName);
        for (int index = matchingReviewEntries.size() - 1; index >= 0; index--) {
            mergedMessages.add(matchingReviewEntries.get(index).getMessage());
        }

        if (preferredMessage != null && !preferredMessage.isBlank()) {
            mergedMessages.add(preferredMessage);
        }

        return compactMessages(mergedMessages);
    }

    private static List<String> compactMessages(List<String> rawMessages) {
        if (rawMessages == null || rawMessages.isEmpty()) {
            return List.of();
        }

        Set<String> uniqueMessages = new LinkedHashSet<>();
        for (String rawMessage : rawMessages) {
            if (rawMessage == null) {
                continue;
            }

            String normalizedMessage = rawMessage.replace('\n', ' ').replace('\r', ' ').trim();
            if (!normalizedMessage.isBlank()) {
                uniqueMessages.add(normalizedMessage);
            }
        }

        return List.copyOf(uniqueMessages);
    }

    private static List<ReviewCaseMessage> buildCaseMessages(List<String> capturedMessages, String triggerMessage, ChatSourceType sourceType) {
        return ReviewCaseMessage.fromCapturedMessages(capturedMessages, triggerMessage, sourceType);
    }

    private static List<RuleDetail> collectRuleDetails(PipelineDecision decision) {
        if (decision == null) {
            return List.of();
        }

        List<RuleDetail> details = new ArrayList<>();
        if (!decision.getStageResults().isEmpty()) {
            for (StageResult stageResult : decision.getStageResults()) {
                if (stageResult == null || !stageResult.hasReason()) {
                    continue;
                }

                for (String detail : splitReasonParts(stageResult.getReason())) {
                    details.add(new RuleDetail(stageLabel(stageResult), detail));
                }
            }
        }

        if (details.isEmpty()) {
            for (String reason : decision.getReasons()) {
                for (String detail : splitReasonParts(reason)) {
                    details.add(new RuleDetail("Pipeline", detail));
                }
            }
        }

        return List.copyOf(details);
    }

    private static List<String> splitReasonParts(String reason) {
        if (reason == null || reason.isBlank()) {
            return List.of();
        }

        String[] parts = reason.split(";\\s*");
        List<String> normalizedParts = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part == null) {
                continue;
            }

            String normalizedPart = part.trim();
            if (!normalizedPart.isBlank()) {
                normalizedParts.add(normalizedPart);
            }
        }

        return List.copyOf(normalizedParts);
    }

    private static String stageLabel(StageResult stageResult) {
        if (stageResult == null || stageResult.getStageName() == null || stageResult.getStageName().isBlank()) {
            return "Pipeline";
        }

        return stageResult.getStageName().trim();
    }

    private static List<ReviewEntry> matchingReviewEntries(List<ReviewEntry> entries, UUID senderUuid, String senderName) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        List<ReviewEntry> matches = new ArrayList<>();
        for (ReviewEntry entry : entries) {
            if (entry == null || !matchesPlayer(entry, senderUuid, senderName)) {
                continue;
            }

            matches.add(entry);
        }

        return List.copyOf(matches);
    }

    private static ReviewEntry latestReviewEntryForPlayer(List<ReviewEntry> entries, String playerName) {
        ReviewEntry latestEntry = null;
        for (ReviewEntry entry : entries) {
            if (entry == null || !matchesPlayer(entry, null, playerName)) {
                continue;
            }
            if (latestEntry == null || entry.getCapturedAtMs() > latestEntry.getCapturedAtMs()) {
                latestEntry = entry;
            }
        }

        return latestEntry;
    }

    private static boolean matchesPlayer(ReviewEntry entry, UUID senderUuid, String senderName) {
        if (entry == null) {
            return false;
        }
        if (senderUuid != null && senderUuid.equals(entry.getSenderUuid())) {
            return true;
        }

        String normalizedSenderName = normalizePlayerName(senderName);
        if (normalizedSenderName.isBlank()) {
            return false;
        }

        return normalizedSenderName.equals(normalizePlayerName(entry.getSenderName()));
    }

    private static String sanitizePlayerName(String playerName) {
        return playerName == null ? "" : playerName.trim();
    }

    private static String normalizePlayerName(String playerName) {
        return sanitizePlayerName(playerName).toLowerCase(Locale.ROOT);
    }

    private static Optional<AlertContext> findByLinkedReviewEntryId(String reviewEntryId) {
        if (reviewEntryId == null || reviewEntryId.isBlank()) {
            return Optional.empty();
        }

        String matchingContextId = null;
        long latestCapturedAtMs = Long.MIN_VALUE;
        for (Map.Entry<String, AlertContext> entry : RECENT.entrySet()) {
            AlertContext context = entry.getValue();
            if (context == null || !reviewEntryId.equals(context.linkedReviewEntryId())) {
                continue;
            }
            if (matchingContextId == null || context.capturedAtMs() >= latestCapturedAtMs) {
                matchingContextId = entry.getKey();
                latestCapturedAtMs = context.capturedAtMs();
            }
        }

        if (matchingContextId == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(RECENT.get(matchingContextId));
    }

    private static ScamScreenerRuntime safeRuntime() {
        try {
            return ScamScreenerRuntime.getInstance();
        } catch (LinkageError | RuntimeException ignored) {
            return null;
        }
    }

    /**
     * One stored alert snapshot used by the warning action buttons.
     *
     * @param id the stable alert id
     * @param senderUuid the sender UUID, when available
     * @param senderName the sender name, when available
     * @param rawMessage the captured chat message
     * @param capturedMessages the merged multi-line message context
     * @param caseMessages the review-ready case-level messages
     * @param score the total pipeline score
     * @param outcome the final pipeline outcome
     * @param decidedByStage the stage that finalized the decision
     * @param capturedAtMs the captured timestamp in epoch milliseconds
     * @param reasons the collected pipeline reasons
     * @param ruleDetails the split rule-like details shown in the v1 info flow
     * @param stageResults the ordered pipeline stage trace
     * @param linkedReviewEntryId the linked review entry id, when one exists
     */
    public record AlertContext(
        String id,
        UUID senderUuid,
        String senderName,
        String rawMessage,
        List<String> capturedMessages,
        List<ReviewCaseMessage> caseMessages,
        int score,
        PipelineDecision.Outcome outcome,
        String decidedByStage,
        long capturedAtMs,
        List<String> reasons,
        List<RuleDetail> ruleDetails,
        List<StageResult> stageResults,
        String linkedReviewEntryId
    ) {
        public AlertContext {
            id = id == null ? "" : id.trim();
            senderName = senderName == null ? "" : senderName.trim();
            rawMessage = rawMessage == null ? "" : rawMessage.trim();
            capturedMessages = capturedMessages == null ? List.of() : List.copyOf(capturedMessages);
            caseMessages = caseMessages == null ? List.of() : List.copyOf(caseMessages);
            score = Math.max(0, score);
            outcome = outcome == null ? PipelineDecision.Outcome.IGNORE : outcome;
            decidedByStage = decidedByStage == null ? "" : decidedByStage.trim();
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
            ruleDetails = ruleDetails == null ? List.of() : List.copyOf(ruleDetails);
            stageResults = stageResults == null ? List.of() : List.copyOf(stageResults);
            linkedReviewEntryId = linkedReviewEntryId == null ? "" : linkedReviewEntryId.trim();
        }

        /**
         * Returns the preferred display name for this alert target.
         *
         * @return a non-blank player label
         */
        public String displayPlayerName() {
            if (!senderName.isBlank()) {
                return senderName;
            }
            ChatLineClassifier.ParsedPlayerLine parsedPlayerLine = ChatLineClassifier.parsePlayerMessage(rawMessage).orElse(null);
            if (parsedPlayerLine != null && !parsedPlayerLine.senderName().isBlank()) {
                return parsedPlayerLine.senderName();
            }

            return "unknown";
        }

        /**
         * Indicates whether the alert points to a player target.
         *
         * @return {@code true} when UUID or player name is present
         */
        public boolean hasPlayerTarget() {
            return senderUuid != null || !senderName.isBlank();
        }

        /**
         * Indicates whether the alert is linked to one review entry.
         *
         * @return {@code true} when a linked review entry id is present
         */
        public boolean hasLinkedReviewEntry() {
            return !linkedReviewEntryId.isBlank();
        }
    }

    /**
     * One concrete rule-like detail line shown in the classic info view.
     *
     * @param source the stage or source that produced the detail
     * @param detail the concrete detail text
     */
    public record RuleDetail(String source, String detail) {
        public RuleDetail {
            source = source == null ? "" : source.trim();
            detail = detail == null ? "" : detail.trim();
        }
    }
}
