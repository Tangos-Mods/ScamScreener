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
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingCaseExportServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void exportsSingleCanonicalTrainingFile() throws IOException {
        ReviewEntry pending = reviewedEntry("review-1", ReviewVerdict.PENDING, 1_000L);
        ReviewEntry risk = reviewedEntry("review-2", ReviewVerdict.RISK, 2_000L);
        ReviewEntry safe = reviewedEntry("review-3", ReviewVerdict.SAFE, 3_000L);

        Path trainingCasesFile = tempDir.resolve("training-cases-v2.jsonl");

        TrainingCaseExportService.TrainingCaseExportResult result = new TrainingCaseExportService("client-abc").exportReviewedCases(
            List.of(pending, risk, safe),
            trainingCasesFile
        );

        assertEquals(2, result.exportedCaseCount());

        List<String> trainingLines = Files.readAllLines(trainingCasesFile);
        String rawContent = Files.readString(trainingCasesFile);

        assertEquals(2, trainingLines.size());
        assertTrue(trainingLines.getFirst().contains("\"caseId\":\"case.client-abc.review-2\""));
        assertTrue(trainingLines.get(1).contains("\"caseId\":\"case.client-abc.review-3\""));
        assertTrue(trainingLines.getFirst().contains("\"format\":\"training_case_v2\""));
        assertTrue(trainingLines.getFirst().contains("\"role\":\"other\""));
        assertTrue(trainingLines.getFirst().contains("\"source\":\"player\""));
        assertTrue(trainingLines.getFirst().contains("\"outcome\":\"pass\""));
        assertTrue(trainingLines.getFirst().contains("\"score\":20"));
        assertTrue(trainingLines.getFirst().contains("\"reason\":\"External platform push: \\\"discord\\\"\""));
        assertTrue(trainingLines.getFirst().contains("\"fixedStageCalibrations\""));
        assertTrue(trainingLines.getFirst().contains("\"mappingId\":\"stage.rule::rule.external_platform\""));
        assertTrue(rawContent.contains("\n"));
        assertFalse(rawContent.contains("\r\n"));
    }

    @Test
    void exportsSingleCanonicalTrainingFileAsync() throws IOException, ExecutionException, InterruptedException {
        ReviewEntry pending = reviewedEntry("review-1", ReviewVerdict.PENDING, 1_000L);
        ReviewEntry risk = reviewedEntry("review-2", ReviewVerdict.RISK, 2_000L);
        ReviewEntry safe = reviewedEntry("review-3", ReviewVerdict.SAFE, 3_000L);

        Path trainingCasesFile = tempDir.resolve("training-cases-v2-async.jsonl");

        TrainingCaseExportService.TrainingCaseExportResult result = new TrainingCaseExportService("client-async")
            .exportReviewedCasesAsync(List.of(pending, risk, safe), trainingCasesFile)
            .get();

        assertEquals(2, result.exportedCaseCount());

        List<String> trainingLines = Files.readAllLines(trainingCasesFile);

        assertEquals(2, trainingLines.size());
        assertTrue(trainingLines.getFirst().contains("\"caseId\":\"case.client-async.review-2\""));
        assertTrue(trainingLines.get(1).contains("\"caseId\":\"case.client-async.review-3\""));
    }

    @Test
    void writesEachCaseIdOnlyOncePerExport() throws IOException {
        ReviewEntry first = reviewedEntry("review-2", ReviewVerdict.RISK, 2_000L);
        ReviewEntry updated = reviewedEntry("review-2", ReviewVerdict.SAFE, 4_000L);

        Path trainingCasesFile = tempDir.resolve("training-cases-v2-deduplicated.jsonl");

        TrainingCaseExportService.TrainingCaseExportResult result = new TrainingCaseExportService("client-dup").exportReviewedCases(
            List.of(first, updated),
            trainingCasesFile
        );

        List<String> trainingLines = Files.readAllLines(trainingCasesFile);

        assertEquals(1, result.exportedCaseCount());
        assertEquals(1, trainingLines.size());
        assertTrue(trainingLines.getFirst().contains("\"caseId\":\"case.client-dup.review-2\""));
        assertTrue(trainingLines.getFirst().contains("\"label\":\"safe\""));
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
