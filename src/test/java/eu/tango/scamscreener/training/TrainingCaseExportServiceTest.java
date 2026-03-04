package eu.tango.scamscreener.training;

import eu.tango.scamscreener.pipeline.data.StageResult;
import eu.tango.scamscreener.review.ReviewCaseMessage;
import eu.tango.scamscreener.review.ReviewCaseRole;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingCaseExportServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void exportsCanonicalAndDerivedTrainingFiles() throws IOException {
        ReviewEntry pending = reviewedEntry("review-1", ReviewVerdict.PENDING, 1_000L);
        ReviewEntry risk = reviewedEntry("review-2", ReviewVerdict.RISK, 2_000L);
        ReviewEntry safe = reviewedEntry("review-3", ReviewVerdict.SAFE, 3_000L);

        Path trainingCasesFile = tempDir.resolve("training-cases-v2.jsonl");
        Path contextStageFile = tempDir.resolve("context-stage-cases-v2.jsonl");
        Path fixedStageFile = tempDir.resolve("fixed-stage-calibrations-v2.jsonl");

        TrainingCaseExportService.TrainingCaseExportResult result = new TrainingCaseExportService().exportReviewedCases(
            List.of(pending, risk, safe),
            trainingCasesFile,
            contextStageFile,
            fixedStageFile
        );

        assertEquals(2, result.exportedCaseCount());
        assertEquals(2, result.exportedContextCaseCount());
        assertEquals(2, result.exportedCalibrationCount());

        List<String> trainingLines = Files.readAllLines(trainingCasesFile);
        List<String> contextLines = Files.readAllLines(contextStageFile);
        List<String> fixedLines = Files.readAllLines(fixedStageFile);

        assertEquals(2, trainingLines.size());
        assertEquals(2, contextLines.size());
        assertEquals(2, fixedLines.size());
        assertTrue(trainingLines.getFirst().contains("\"caseId\":\"case_000001\""));
        assertTrue(trainingLines.get(1).contains("\"caseId\":\"case_000002\""));
        assertTrue(contextLines.getFirst().contains("\"format\":\"context_stage_case_v2\""));
        assertTrue(fixedLines.getFirst().contains("\"format\":\"fixed_stage_calibration_v2\""));
        assertTrue(fixedLines.getFirst().contains("\"mappingId\":\"stage.rule::rule.external_platform\""));
    }

    private static ReviewEntry reviewedEntry(String id, ReviewVerdict verdict, long capturedAtMs) {
        ReviewEntry entry = new ReviewEntry(
            id,
            null,
            "Alpha",
            "[12] Alpha: add me on discord",
            25,
            "RuleStage",
            capturedAtMs,
            List.of("RULE_MATCH"),
            List.of(StageResult.score("RuleStage", 20, "External platform push: \"discord\"")),
            List.of(new ReviewCaseMessage(
                0,
                "other",
                "player",
                "Alpha add me on discord",
                true,
                ReviewCaseRole.SIGNAL,
                List.of("external_platform"),
                List.of("RuleStage - External platform push: \"discord\"")
            ))
        );
        entry.setVerdict(verdict);
        return entry;
    }
}
