package eu.tango.scamscreener.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalAiTrainerUploadFileTest {
	@Test
	void buildNormalizedUploadFileUsesOnlySelectedSourceFile() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-upload-source-");
		try {
			Path active = root.resolve("scam-screener-training-data.csv");
			Path archiveDir = root.resolve("old").resolve("training-data");
			Path archived = archiveDir.resolve("scam-screener-training-data.csv.old.1");

			writeTrainingCsv(active, "active");
			writeTrainingCsv(archived, "archived");

			LocalAiTrainer trainer = new LocalAiTrainer();
			Path normalized = trainer.buildNormalizedUploadFile(active);

			List<String> lines = Files.readAllLines(normalized, StandardCharsets.UTF_8);
			assertEquals("message,label", lines.get(0));
			assertTrue(lines.stream().anyMatch(line -> line.contains("active message")));
			assertFalse(lines.stream().anyMatch(line -> line.contains("archived message")));
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void buildNormalizedUploadFileFromArchivedSourceUsesArchiveDirectory() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-upload-archived-");
		try {
			Path archiveDir = root.resolve("old").resolve("training-data");
			Path archived = archiveDir.resolve("scam-screener-training-data.csv.old.7");
			writeTrainingCsv(archived, "archived");

			LocalAiTrainer trainer = new LocalAiTrainer();
			Path normalized = trainer.buildNormalizedUploadFile(archived);

			assertEquals(archiveDir.toAbsolutePath().normalize(), normalized.getParent().toAbsolutePath().normalize());
			assertFalse(Files.exists(root.resolve("old").resolve("old")));
			assertTrue(normalized.getFileName().toString().startsWith("scam-screener-training-data.csv.upload.tmp."));
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void archiveTrainingDataOnlyMovesActiveCsvIntoArchiveFolder() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-archive-move-");
		try {
			Path active = root.resolve("scam-screener-training-data.csv");
			writeTrainingCsv(active, "active");

			LocalAiTrainer trainer = new LocalAiTrainer();
			Path archived = trainer.archiveTrainingDataOnly(active);

			assertFalse(Files.exists(active));
			assertTrue(Files.exists(archived));
			assertTrue(archived.toString().contains("old"));
			assertTrue(archived.getFileName().toString().endsWith(".old.1"));
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void archiveTrainingDataOnlyIncrementsArchiveSuffix() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-archive-increment-");
		try {
			Path active = root.resolve("scam-screener-training-data.csv");
			Path archiveDir = root.resolve("old").resolve("training-data");
			writeTrainingCsv(active, "active");
			writeTrainingCsv(archiveDir.resolve("scam-screener-training-data.csv.old.1"), "old1");

			LocalAiTrainer trainer = new LocalAiTrainer();
			Path archived = trainer.archiveTrainingDataOnly(active);

			assertTrue(archived.getFileName().toString().endsWith(".old.2"));
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void buildNormalizedUploadFileFailsForMissingFile() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-upload-missing-");
		try {
			LocalAiTrainer trainer = new LocalAiTrainer();
			IOException error = assertThrows(IOException.class, () -> trainer.buildNormalizedUploadFile(root.resolve("missing.csv")));
			assertTrue(error.getMessage().contains("Training file not found"));
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void buildNormalizedUploadFileFailsWhenHeaderIsInvalid() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-upload-header-");
		try {
			Path active = root.resolve("scam-screener-training-data.csv");
			Files.write(active, List.of("text,label", "\"hello world\",1"), StandardCharsets.UTF_8);

			LocalAiTrainer trainer = new LocalAiTrainer();
			IOException error = assertThrows(IOException.class, () -> trainer.buildNormalizedUploadFile(active));
			assertTrue(error.getMessage().contains("message,label"));
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void buildNormalizedUploadFileFailsWhenOnlyOneLabelExists() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-upload-labels-");
		try {
			Path active = root.resolve("scam-screener-training-data.csv");
			List<String> lines = new ArrayList<>();
			lines.add("message,label");
			for (int i = 0; i < 12; i++) {
				lines.add("\"valid message " + i + "\",1");
			}
			Files.write(active, lines, StandardCharsets.UTF_8);

			LocalAiTrainer trainer = new LocalAiTrainer();
			IOException error = assertThrows(IOException.class, () -> trainer.buildNormalizedUploadFile(active));
			assertTrue(error.getMessage().contains("Need both labels 0 and 1"));
		} finally {
			deleteTree(root);
		}
	}

	private static void writeTrainingCsv(Path file, String prefix) throws IOException {
		List<String> lines = new ArrayList<>();
		lines.add("message,label");
		for (int i = 0; i < 12; i++) {
			lines.add("\"" + prefix + " message " + i + "\"," + (i % 2));
		}
		Files.createDirectories(file.getParent());
		Files.write(file, lines, StandardCharsets.UTF_8);
	}

	private static void deleteTree(Path root) throws IOException {
		if (root == null || !Files.exists(root)) {
			return;
		}
		try (var stream = Files.walk(root)) {
			stream.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ignored) {
				}
			});
		}
	}
}
