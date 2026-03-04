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
 * Exports reviewed cases into one canonical training file.
 */
public final class TrainingCaseExportService {
    private static final Gson GSON = new GsonBuilder()
        .disableHtmlEscaping()
        .create();

    /**
     * Exports the provided review entries into the default training export file.
     *
     * @param entries the review entries to export
     * @return the export result summary
     */
    public TrainingCaseExportResult exportReviewedCases(Iterable<ReviewEntry> entries) {
        return exportReviewedCases(entries, ConfigPaths.trainingCasesV2File());
    }

    /**
     * Exports the provided review entries into explicit target files.
     *
     * @param entries the review entries to export
     * @param trainingCasesFile the canonical training-case output path
     * @return the export result summary
     */
    public TrainingCaseExportResult exportReviewedCases(
        Iterable<ReviewEntry> entries,
        Path trainingCasesFile
    ) {
        List<ReviewEntry> exportableEntries = exportableEntries(entries);
        List<TrainingCaseV2> trainingCases = new ArrayList<>(exportableEntries.size());

        for (int index = 0; index < exportableEntries.size(); index++) {
            ReviewEntry entry = exportableEntries.get(index);
            String caseId = String.format(Locale.ROOT, "case_%06d", index + 1);
            TrainingCaseV2 trainingCase = TrainingCaseV2Mapper.fromReviewEntry(entry, caseId);
            if (trainingCase == null) {
                continue;
            }

            trainingCases.add(trainingCase);
        }

        writeJsonLines(trainingCasesFile, trainingCases);
        return new TrainingCaseExportResult(trainingCases.size(), trainingCasesFile);
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
     * @param trainingCasesFile the canonical export path
     */
    public record TrainingCaseExportResult(
        int exportedCaseCount,
        Path trainingCasesFile
    ) {
    }
}
