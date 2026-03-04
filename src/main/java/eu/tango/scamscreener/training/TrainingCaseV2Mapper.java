package eu.tango.scamscreener.training;

import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.review.ReviewCaseMessage;
import eu.tango.scamscreener.review.ReviewCaseRole;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewPersistenceSanitizer;
import eu.tango.scamscreener.review.ReviewVerdict;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Converts one reviewed case into the canonical {@link TrainingCaseV2} format.
 */
public final class TrainingCaseV2Mapper {
    private static final String FORMAT = "training_case_v2";
    private static final int SCHEMA_VERSION = 2;

    private TrainingCaseV2Mapper() {
    }

    /**
     * Converts one review entry into the canonical training-case payload.
     *
     * @param entry the reviewed entry to convert
     * @return the canonical training case, or {@code null} when the entry is invalid
     */
    public static TrainingCaseV2 fromReviewEntry(ReviewEntry entry) {
        return fromReviewEntry(entry, null);
    }

    /**
     * Converts one review entry into the canonical training-case payload using an explicit case id.
     *
     * @param entry the reviewed entry to convert
     * @param caseId the explicit canonical case id, when provided
     * @return the canonical training case, or {@code null} when the entry is invalid
     */
    public static TrainingCaseV2 fromReviewEntry(ReviewEntry entry, String caseId) {
        if (entry == null || entry.getId() == null || entry.getId().isBlank()) {
            return null;
        }

        String label = label(entry.getVerdict());
        List<TrainingCaseV2.MessageData> messages = buildMessages(entry);
        List<String> caseSignalTagIds = collectCaseSignalTagIds(entry.getCaseMessages());
        List<TrainingCaseV2.ObservedStageResult> observedStageResults = buildObservedStageResults(entry.getStageResults());

        return new TrainingCaseV2(
            FORMAT,
            SCHEMA_VERSION,
            normalizeCaseId(caseId == null || caseId.isBlank() ? entry.getId() : caseId),
            new TrainingCaseV2.CaseData(label, messages, caseSignalTagIds),
            new TrainingCaseV2.ObservedPipeline(
                Math.max(0, entry.getScore()),
                "review",
                TrainingCaseMappings.stageId(entry.getDecidedByStage()),
                observedStageResults
            ),
            new TrainingCaseV2.Supervision(
                buildContextStageTarget(label, entry.getCaseMessages(), caseSignalTagIds),
                buildFixedStageCalibrations(entry.getVerdict(), entry.getCaseMessages())
            )
        );
    }

    private static List<TrainingCaseV2.MessageData> buildMessages(ReviewEntry entry) {
        List<TrainingCaseV2.MessageData> messages = new ArrayList<>();
        for (ReviewCaseMessage message : entry.getCaseMessages()) {
            if (message == null || message.getCleanText().isBlank()) {
                continue;
            }

            messages.add(new TrainingCaseV2.MessageData(
                Math.max(0, message.getMessageIndex()),
                ReviewPersistenceSanitizer.sanitizePersistedText(message.getCleanText(), entry.getSenderName()),
                normalizeValue(message.getMessageSourceType()),
                normalizeValue(message.getSpeakerRole()),
                message.isTriggerMessage(),
                caseRoleId(message.getCaseRole()),
                List.copyOf(message.getSignalTagIds()),
                normalizeSelectionIds(message.getAdvancedRuleSelections())
            ));
        }

        return List.copyOf(messages);
    }

    private static List<String> collectCaseSignalTagIds(List<ReviewCaseMessage> caseMessages) {
        Set<String> caseSignalTagIds = new LinkedHashSet<>();
        if (caseMessages != null) {
            for (ReviewCaseMessage message : caseMessages) {
                if (message == null || !message.isSignalMessage()) {
                    continue;
                }
                caseSignalTagIds.addAll(message.getSignalTagIds());
            }
        }

        return List.copyOf(caseSignalTagIds);
    }

    private static List<TrainingCaseV2.ObservedStageResult> buildObservedStageResults(List<StageResult> stageResults) {
        List<TrainingCaseV2.ObservedStageResult> observedStageResults = new ArrayList<>();
        if (stageResults != null) {
            for (StageResult stageResult : stageResults) {
                if (stageResult == null) {
                    continue;
                }

                observedStageResults.add(new TrainingCaseV2.ObservedStageResult(
                    stageResult.getStageId(),
                    decisionId(stageResult.getDecision()),
                    stageResult.getScoreDelta(),
                    stageResult.getReasonIds()
                ));
            }
        }

        return List.copyOf(observedStageResults);
    }

    private static TrainingCaseV2.ContextStageTarget buildContextStageTarget(
        String label,
        List<ReviewCaseMessage> caseMessages,
        List<String> caseSignalTagIds
    ) {
        List<Integer> signalMessageIndices = new ArrayList<>();
        List<Integer> contextMessageIndices = new ArrayList<>();
        List<Integer> excludedMessageIndices = new ArrayList<>();

        if (caseMessages != null) {
            for (ReviewCaseMessage message : caseMessages) {
                if (message == null) {
                    continue;
                }

                int messageIndex = Math.max(0, message.getMessageIndex());
                if (message.getCaseRole() == ReviewCaseRole.SIGNAL) {
                    signalMessageIndices.add(messageIndex);
                } else if (message.getCaseRole() == ReviewCaseRole.CONTEXT) {
                    contextMessageIndices.add(messageIndex);
                } else {
                    excludedMessageIndices.add(messageIndex);
                }
            }
        }

        return new TrainingCaseV2.ContextStageTarget(
            label,
            List.copyOf(signalMessageIndices),
            List.copyOf(contextMessageIndices),
            List.copyOf(excludedMessageIndices),
            List.copyOf(caseSignalTagIds)
        );
    }

    private static List<TrainingCaseV2.FixedStageCalibration> buildFixedStageCalibrations(
        ReviewVerdict verdict,
        List<ReviewCaseMessage> caseMessages
    ) {
        Map<String, CalibrationBuilder> builders = new LinkedHashMap<>();
        if (caseMessages != null) {
            for (ReviewCaseMessage message : caseMessages) {
                if (message == null || message.getAdvancedRuleSelections().isEmpty()) {
                    continue;
                }

                int messageIndex = Math.max(0, message.getMessageIndex());
                for (String selection : message.getAdvancedRuleSelections()) {
                    TrainingCaseMappings.ParsedSelection parsedSelection = TrainingCaseMappings.parseSelection(selection);
                    if (parsedSelection.id().isBlank()) {
                        continue;
                    }

                    CalibrationBuilder builder = builders.computeIfAbsent(parsedSelection.id(), ignored ->
                        new CalibrationBuilder(parsedSelection.id(), parsedSelection.stageId(), parsedSelection.reasonId())
                    );
                    builder.messageIndices.add(messageIndex);
                    builder.seenOnTrigger |= message.isTriggerMessage();
                }
            }
        }

        List<TrainingCaseV2.FixedStageCalibration> calibrations = new ArrayList<>(builders.size());
        for (CalibrationBuilder builder : builders.values()) {
            calibrations.add(builder.build(verdict));
        }

        return List.copyOf(calibrations);
    }

    private static String normalizeCaseId(String reviewEntryId) {
        String normalized = reviewEntryId == null ? "" : reviewEntryId.trim();
        if (normalized.isEmpty()) {
            return "case.unknown";
        }
        if (normalized.startsWith("case_")) {
            return normalized.toLowerCase(Locale.ROOT);
        }

        return "case." + normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
    }

    private static String label(ReviewVerdict verdict) {
        if (verdict == null) {
            return "pending";
        }

        return switch (verdict) {
            case RISK -> "risk";
            case SAFE -> "safe";
            case IGNORED -> "ignored";
            case PENDING -> "pending";
        };
    }

    private static String caseRoleId(ReviewCaseRole caseRole) {
        if (caseRole == null) {
            return "excluded";
        }

        return switch (caseRole) {
            case EXCLUDED -> "excluded";
            case CONTEXT -> "context";
            case SIGNAL -> "signal";
        };
    }

    private static String decisionId(Enum<?> decision) {
        if (decision == null) {
            return "pass";
        }

        return decision.name().trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> normalizeSelectionIds(List<String> rawSelections) {
        if (rawSelections == null || rawSelections.isEmpty()) {
            return List.of();
        }

        Set<String> normalizedSelections = new LinkedHashSet<>();
        for (String rawSelection : rawSelections) {
            String normalizedSelection = TrainingCaseMappings.normalizeSelectionId(rawSelection);
            if (!normalizedSelection.isBlank()) {
                normalizedSelections.add(normalizedSelection);
            }
        }

        return List.copyOf(normalizedSelections);
    }

    private static final class CalibrationBuilder {
        private final String mappingId;
        private final String stageId;
        private final String reasonId;
        private final Set<Integer> messageIndices = new LinkedHashSet<>();
        private boolean seenOnTrigger;

        private CalibrationBuilder(String mappingId, String stageId, String reasonId) {
            this.mappingId = mappingId;
            this.stageId = stageId;
            this.reasonId = reasonId;
        }

        private TrainingCaseV2.FixedStageCalibration build(ReviewVerdict verdict) {
            int absoluteWeight = seenOnTrigger || messageIndices.size() > 1 ? 2 : 1;
            String action = switch (verdict == null ? ReviewVerdict.PENDING : verdict) {
                case RISK -> "increase";
                case SAFE -> "decrease";
                case IGNORED, PENDING -> "keep";
            };
            int weightDeltaHint = switch (action) {
                case "increase" -> absoluteWeight;
                case "decrease" -> -absoluteWeight;
                default -> 0;
            };
            String strength = switch (Math.abs(weightDeltaHint)) {
                case 0 -> "none";
                case 1 -> "normal";
                default -> "strong";
            };

            return new TrainingCaseV2.FixedStageCalibration(
                mappingId,
                stageId,
                reasonId,
                action,
                strength,
                weightDeltaHint,
                List.copyOf(messageIndices)
            );
        }
    }
}
