package eu.tango.scamscreener.training;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.tango.scamscreener.config.store.ConfigPaths;
import eu.tango.scamscreener.review.ReviewEntry;
import eu.tango.scamscreener.review.ReviewVerdict;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Exports reviewed cases into canonical and derived training files.
 */
public final class TrainingCaseExportService {
    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .create();

    /**
     * Exports the provided review entries into the default training export files.
     *
     * @param entries the review entries to export
     * @return the export result summary
     */
    public TrainingCaseExportResult exportReviewedCases(Iterable<ReviewEntry> entries) {
        return exportReviewedCases(
            entries,
            ConfigPaths.trainingCasesV2File(),
            ConfigPaths.contextStageCasesV2File(),
            ConfigPaths.fixedStageCalibrationsV2File()
        );
    }

    /**
     * Exports the provided review entries into explicit target files.
     *
     * @param entries the review entries to export
     * @param trainingCasesFile the canonical training-case output path
     * @param contextStageFile the derived context-stage output path
     * @param fixedStageFile the derived fixed-stage output path
     * @return the export result summary
     */
    public TrainingCaseExportResult exportReviewedCases(
        Iterable<ReviewEntry> entries,
        Path trainingCasesFile,
        Path contextStageFile,
        Path fixedStageFile
    ) {
        List<ReviewEntry> exportableEntries = exportableEntries(entries);
        List<TrainingCaseV2> trainingCases = new ArrayList<>(exportableEntries.size());
        List<ContextStageExportRow> contextRows = new ArrayList<>(exportableEntries.size());
        List<FixedStageCalibrationExportRow> fixedRows = new ArrayList<>();

        for (int index = 0; index < exportableEntries.size(); index++) {
            ReviewEntry entry = exportableEntries.get(index);
            String caseId = String.format(Locale.ROOT, "case_%06d", index + 1);
            TrainingCaseV2 trainingCase = TrainingCaseV2Mapper.fromReviewEntry(entry, caseId);
            if (trainingCase == null) {
                continue;
            }

            trainingCases.add(trainingCase);
            contextRows.add(new ContextStageExportRow(
                "context_stage_case_v2",
                2,
                trainingCase.caseId(),
                trainingCase.caseData().label(),
                trainingCase.caseData().messages(),
                trainingCase.supervision().contextStage()
            ));
            for (TrainingCaseV2.FixedStageCalibration calibration : trainingCase.supervision().fixedStageCalibrations()) {
                fixedRows.add(new FixedStageCalibrationExportRow(
                    "fixed_stage_calibration_v2",
                    2,
                    trainingCase.caseId(),
                    trainingCase.caseData().label(),
                    calibration
                ));
            }
        }

        writeJsonLines(trainingCasesFile, trainingCases);
        writeJsonLines(contextStageFile, contextRows);
        writeJsonLines(fixedStageFile, fixedRows);
        return new TrainingCaseExportResult(
            trainingCases.size(),
            contextRows.size(),
            fixedRows.size(),
            trainingCasesFile,
            contextStageFile,
            fixedStageFile
        );
    }

    private static List<ReviewEntry> exportableEntries(Iterable<ReviewEntry> entries) {
        List<ReviewEntry> exportableEntries = new ArrayList<>();
        if (entries == null) {
            return exportableEntries;
        }

        for (ReviewEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            ReviewVerdict verdict = entry.getVerdict();
            if (verdict != ReviewVerdict.RISK && verdict != ReviewVerdict.SAFE) {
                continue;
            }

            exportableEntries.add(entry);
        }

        exportableEntries.sort(Comparator
            .comparingLong(ReviewEntry::getCapturedAtMs)
            .thenComparing(ReviewEntry::getId));
        return exportableEntries;
    }

    private static void writeJsonLines(Path path, List<?> rows) {
        if (path == null) {
            return;
        }

        StringBuilder content = new StringBuilder();
        if (rows != null) {
            for (Object row : rows) {
                if (row == null) {
                    continue;
                }
                if (content.length() > 0) {
                    content.append(System.lineSeparator());
                }
                content.append(GSON.toJson(row));
            }
        }

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content.toString(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to export training cases to " + path, exception);
        }
    }

    /**
     * One export summary.
     *
     * @param exportedCaseCount number of canonical training cases written
     * @param exportedContextCaseCount number of context-stage lines written
     * @param exportedCalibrationCount number of calibration lines written
     * @param trainingCasesFile the canonical export path
     * @param contextStageFile the context-stage export path
     * @param fixedStageFile the fixed-stage export path
     */
    public record TrainingCaseExportResult(
        int exportedCaseCount,
        int exportedContextCaseCount,
        int exportedCalibrationCount,
        Path trainingCasesFile,
        Path contextStageFile,
        Path fixedStageFile
    ) {
    }

    private record ContextStageExportRow(
        String format,
        int schemaVersion,
        String caseId,
        String label,
        List<TrainingCaseV2.MessageData> messages,
        TrainingCaseV2.ContextStageTarget target
    ) {
    }

    private record FixedStageCalibrationExportRow(
        String format,
        int schemaVersion,
        String caseId,
        String label,
        TrainingCaseV2.FixedStageCalibration calibration
    ) {
    }
}
