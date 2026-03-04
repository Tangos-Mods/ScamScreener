package eu.tango.scamscreener.training;

import java.util.List;

/**
 * Canonical anonymous training case used for context-stage training and fixed-stage calibration.
 */
public record TrainingCaseV2(
    String format,
    int schemaVersion,
    String caseId,
    CaseData caseData,
    ObservedPipeline observedPipeline,
    Supervision supervision
) {
    /**
     * One canonical case payload.
     *
     * @param label final reviewed case label
     * @param messages ordered anonymized case messages
     * @param caseSignalTagIds deduplicated signal tags present in the case
     */
    public record CaseData(
        String label,
        List<MessageData> messages,
        List<String> caseSignalTagIds
    ) {
    }

    /**
     * One canonical case message.
     *
     * @param index stable message order inside the case
     * @param text anonymized message text
     * @param sourceType message source type id
     * @param speakerRole message speaker role id
     * @param trigger indicates whether the message triggered the review
     * @param caseRole reviewed case role
     * @param signalTagIds explicit reviewed signal tags
     * @param mappingIds stable fixed-stage mapping ids attached by the reviewer
     */
    public record MessageData(
        int index,
        String text,
        String sourceType,
        String speakerRole,
        boolean trigger,
        String caseRole,
        List<String> signalTagIds,
        List<String> mappingIds
    ) {
    }

    /**
     * Runtime pipeline snapshot observed when the case was captured.
     *
     * @param scoreAtCapture score visible at capture time
     * @param outcomeAtCapture observed capture outcome
     * @param decidedByStageId stable stage id that reached the outcome
     * @param stageResults ordered normalized stage results
     */
    public record ObservedPipeline(
        int scoreAtCapture,
        String outcomeAtCapture,
        String decidedByStageId,
        List<ObservedStageResult> stageResults
    ) {
    }

    /**
     * One normalized observed stage result.
     *
     * @param stageId stable stage id
     * @param decision normalized pipeline decision
     * @param scoreDelta captured score contribution
     * @param reasonIds stable reason ids derived from the captured stage reason
     */
    public record ObservedStageResult(
        String stageId,
        String decision,
        int scoreDelta,
        List<String> reasonIds
    ) {
    }

    /**
     * Canonical supervision attached to the case.
     *
     * @param contextStage target data for the future context stage
     * @param fixedStageCalibrations weight hints for deterministic stages and mappings
     */
    public record Supervision(
        ContextStageTarget contextStage,
        List<FixedStageCalibration> fixedStageCalibrations
    ) {
    }

    /**
     * Context-stage supervision derived from reviewed message roles.
     *
     * @param targetLabel final reviewed target label
     * @param signalMessageIndices message indices labeled as signals
     * @param contextMessageIndices message indices labeled as context
     * @param excludedMessageIndices message indices explicitly excluded
     * @param targetSignalTagIds deduplicated signal tags for the target
     */
    public record ContextStageTarget(
        String targetLabel,
        List<Integer> signalMessageIndices,
        List<Integer> contextMessageIndices,
        List<Integer> excludedMessageIndices,
        List<String> targetSignalTagIds
    ) {
    }

    /**
     * One deterministic-stage calibration hint derived from reviewed mapping selections.
     *
     * @param mappingId stable combined mapping id
     * @param stageId stable stage id
     * @param reasonId stable reason id
     * @param action calibration direction
     * @param strength coarse calibration strength
     * @param weightDeltaHint signed numeric weight hint
     * @param becauseMessageIndices message indices that justify the hint
     */
    public record FixedStageCalibration(
        String mappingId,
        String stageId,
        String reasonId,
        String action,
        String strength,
        int weightDeltaHint,
        List<Integer> becauseMessageIndices
    ) {
    }
}
