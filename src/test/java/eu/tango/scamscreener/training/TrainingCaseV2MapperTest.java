package eu.tango.scamscreener.training;

import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.review.ReviewCaseMessage;
import eu.tango.scamscreener.review.ReviewCaseRole;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingCaseV2MapperTest {
    @Test
    void normalizesLegacyAdvancedMappingSelectionIds() {
        assertEquals(
            "stage.rule::rule.external_platform",
            TrainingCaseMappings.normalizeSelectionId("RuleStage - External platform push: \"discord\"")
        );
        assertEquals(
            "stage.behavior::behavior.repeated_message",
            TrainingCaseMappings.normalizeSelectionId("BehaviorStage - Repeated contact message x4")
        );
        assertEquals(
            "stage.rule::rule.external_platform",
            TrainingCaseMappings.normalizeSelectionId("stage.rule::rule.external_platform")
        );
    }

    @Test
    void buildsCanonicalTrainingCaseFromReviewedEntry() {
        ReviewEntry entry = new ReviewEntry(
            "review-7",
            null,
            "Alpha",
            "[12] Alpha: add me on discord",
            43,
            "RuleStage",
            12_345L,
            List.of("RULE_MATCH"),
            List.of(
                StageResult.score("RuleStage", 20, "External platform push: \"discord\""),
                StageResult.score("BehaviorStage", 1, "Repeated contact message x4; Behavior combo: repeated burst contact")
            ),
            List.of(
                new ReviewCaseMessage(
                    0,
                    "other",
                    "player",
                    "hello",
                    false,
                    ReviewCaseRole.CONTEXT,
                    List.of(),
                    List.of()
                ),
                new ReviewCaseMessage(
                    1,
                    "other",
                    "player",
                    "Alpha add me on discord",
                    true,
                    ReviewCaseRole.SIGNAL,
                    List.of("external_platform"),
                    List.of(
                        "RuleStage - External platform push: \"discord\"",
                        "BehaviorStage - Repeated contact message x4"
                    )
                )
            )
        );
        entry.setVerdict(ReviewVerdict.RISK);

        TrainingCaseV2 trainingCase = TrainingCaseV2Mapper.fromReviewEntry(entry);

        assertEquals("training_case_v2", trainingCase.format());
        assertEquals(2, trainingCase.schemaVersion());
        assertEquals("case.review-7", trainingCase.caseId());
        assertEquals("risk", trainingCase.caseData().label());
        assertEquals(2, trainingCase.caseData().messages().size());
        assertEquals("Alpha add me on discord", trainingCase.caseData().messages().get(1).text());
        assertEquals(
            List.of("stage.rule::rule.external_platform", "stage.behavior::behavior.repeated_message"),
            trainingCase.caseData().messages().get(1).mappingIds()
        );
        assertEquals(List.of("external_platform"), trainingCase.caseData().caseSignalTagIds());

        assertEquals("review", trainingCase.observedPipeline().outcomeAtCapture());
        assertEquals("stage.rule", trainingCase.observedPipeline().decidedByStageId());
        assertEquals(
            List.of("rule.external_platform"),
            trainingCase.observedPipeline().stageResults().get(0).reasonIds()
        );
        assertEquals(
            List.of("behavior.repeated_message", "behavior.combo_repeated_burst"),
            trainingCase.observedPipeline().stageResults().get(1).reasonIds()
        );

        assertEquals(List.of(1), trainingCase.supervision().contextStage().signalMessageIndices());
        assertEquals(List.of(0), trainingCase.supervision().contextStage().contextMessageIndices());
        assertEquals(List.of(), trainingCase.supervision().contextStage().excludedMessageIndices());
        assertEquals(List.of("external_platform"), trainingCase.supervision().contextStage().targetSignalTagIds());

        assertEquals(2, trainingCase.supervision().fixedStageCalibrations().size());
        TrainingCaseV2.FixedStageCalibration ruleCalibration = trainingCase.supervision().fixedStageCalibrations().stream()
            .filter(calibration -> "stage.rule::rule.external_platform".equals(calibration.mappingId()))
            .findFirst()
            .orElseThrow();
        assertEquals("stage.rule", ruleCalibration.stageId());
        assertEquals("rule.external_platform", ruleCalibration.reasonId());
        assertEquals("increase", ruleCalibration.action());
        assertEquals("strong", ruleCalibration.strength());
        assertEquals(2, ruleCalibration.weightDeltaHint());
        assertEquals(List.of(1), ruleCalibration.becauseMessageIndices());

        assertTrue(trainingCase.supervision().fixedStageCalibrations().stream()
            .anyMatch(calibration -> "stage.behavior::behavior.repeated_message".equals(calibration.mappingId())));
    }
}
