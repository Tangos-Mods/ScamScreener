package eu.tango.scamscreener.review;

import eu.tango.scamscreener.chat.ChatLineClassifier;
import eu.tango.scamscreener.pipeline.data.ChatSourceType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * One anonymized message inside a case-oriented review.
 */
@Getter
public final class ReviewCaseMessage {
    private final int messageIndex;
    private final String speakerRole;
    private final String messageSourceType;
    private final String cleanText;
    private final boolean triggerMessage;
    private ReviewCaseRole caseRole;
    private final List<String> signalTagIds;
    private final List<String> advancedRuleSelections;

    public ReviewCaseMessage(
        int messageIndex,
        String speakerRole,
        String messageSourceType,
        String cleanText,
        boolean triggerMessage,
        ReviewCaseRole caseRole,
        List<String> signalTagIds,
        List<String> advancedRuleSelections
    ) {
        this.messageIndex = Math.max(0, messageIndex);
        this.speakerRole = normalizeValue(speakerRole, "other");
        this.messageSourceType = normalizeValue(messageSourceType, "player");
        this.cleanText = normalizeText(cleanText);
        this.triggerMessage = triggerMessage;
        this.caseRole = caseRole == null ? defaultRole(triggerMessage) : caseRole;
        this.signalTagIds = normalizeList(signalTagIds);
        this.advancedRuleSelections = normalizeList(advancedRuleSelections);
    }

    public void setCaseRole(ReviewCaseRole caseRole) {
        this.caseRole = caseRole == null ? ReviewCaseRole.EXCLUDED : caseRole;
    }

    public void toggleSignalTag(String signalTagId) {
        toggleValue(signalTagIds, signalTagId);
    }

    public void toggleAdvancedRuleSelection(String ruleSelection) {
        toggleValue(advancedRuleSelections, ruleSelection);
    }

    public boolean hasSignalTag(String signalTagId) {
        return contains(signalTagIds, signalTagId);
    }

    public boolean hasAdvancedRuleSelection(String ruleSelection) {
        return contains(advancedRuleSelections, ruleSelection);
    }

    public boolean isIncludedInCase() {
        return caseRole != ReviewCaseRole.EXCLUDED;
    }

    public boolean isSignalMessage() {
        return caseRole == ReviewCaseRole.SIGNAL;
    }

    public int signalCount() {
        return signalTagIds.size();
    }

    public void clearSignalAnnotations() {
        signalTagIds.clear();
        advancedRuleSelections.clear();
    }

    public static List<ReviewCaseMessage> fromCapturedMessages(
        List<String> rawMessages,
        String triggerMessage,
        ChatSourceType sourceType
    ) {
        if (rawMessages == null || rawMessages.isEmpty()) {
            return List.of(new ReviewCaseMessage(
                0,
                defaultSpeakerRole(sourceType),
                defaultMessageSourceType(sourceType),
                triggerMessage,
                true,
                ReviewCaseRole.SIGNAL,
                List.of(),
                List.of()
            ));
        }

        List<String> normalizedMessages = new ArrayList<>();
        for (String rawMessage : rawMessages) {
            String cleanText = normalizeText(rawMessage);
            if (!cleanText.isBlank()) {
                normalizedMessages.add(cleanText);
            }
        }
        if (normalizedMessages.isEmpty()) {
            normalizedMessages.add(normalizeText(triggerMessage));
        }

        String normalizedTrigger = normalizeText(triggerMessage);
        int triggerIndex = normalizedMessages.isEmpty() ? -1 : normalizedMessages.size() - 1;
        if (!normalizedTrigger.isBlank()) {
            for (int index = normalizedMessages.size() - 1; index >= 0; index--) {
                if (normalizedTrigger.equals(normalizedMessages.get(index))) {
                    triggerIndex = index;
                    break;
                }
            }
        }

        List<ReviewCaseMessage> messages = new ArrayList<>(normalizedMessages.size());
        for (int index = 0; index < normalizedMessages.size(); index++) {
            boolean isTriggerMessage = index == triggerIndex;
            messages.add(new ReviewCaseMessage(
                index,
                defaultSpeakerRole(sourceType),
                defaultMessageSourceType(sourceType),
                normalizedMessages.get(index),
                isTriggerMessage,
                isTriggerMessage ? ReviewCaseRole.SIGNAL : ReviewCaseRole.CONTEXT,
                List.of(),
                List.of()
            ));
        }

        return List.copyOf(messages);
    }

    private static ReviewCaseRole defaultRole(boolean triggerMessage) {
        return triggerMessage ? ReviewCaseRole.SIGNAL : ReviewCaseRole.CONTEXT;
    }

    private static String defaultSpeakerRole(ChatSourceType sourceType) {
        return switch (sourceType == null ? ChatSourceType.UNKNOWN : sourceType) {
            case SYSTEM -> "system";
            case UNKNOWN -> "unknown";
            default -> "other";
        };
    }

    private static String defaultMessageSourceType(ChatSourceType sourceType) {
        return switch (sourceType == null ? ChatSourceType.UNKNOWN : sourceType) {
            case SYSTEM -> "system";
            case UNKNOWN -> "unknown";
            default -> "player";
        };
    }

    private static void toggleValue(List<String> values, String rawValue) {
        String normalizedValue = normalizeValue(rawValue, "");
        if (normalizedValue.isBlank()) {
            return;
        }
        if (values.contains(normalizedValue)) {
            values.remove(normalizedValue);
        } else {
            values.add(normalizedValue);
        }
    }

    private static boolean contains(List<String> values, String rawValue) {
        String normalizedValue = normalizeValue(rawValue, "");
        if (normalizedValue.isBlank()) {
            return false;
        }

        return values.contains(normalizedValue);
    }

    private static List<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> deduplicated = new LinkedHashSet<>();
        for (String value : values) {
            String normalizedValue = normalizeValue(value, "");
            if (!normalizedValue.isBlank()) {
                deduplicated.add(normalizedValue);
            }
        }

        return new ArrayList<>(deduplicated);
    }

    private static String normalizeValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }

    private static String normalizeText(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            return "";
        }

        return ChatLineClassifier.displayMessageOnly(rawMessage.replace('\n', ' ').replace('\r', ' ').trim());
    }
}
