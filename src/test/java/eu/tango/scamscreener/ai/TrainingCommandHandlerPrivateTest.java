package eu.tango.scamscreener.ai;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingCommandHandlerPrivateTest {
	private static Method resolveOldFolderMethod;
	private static Method loadUploadTosTextMethod;

	@BeforeAll
	static void setUpReflection() throws Exception {
		resolveOldFolderMethod = TrainingCommandHandler.class.getDeclaredMethod("resolveOldFolder", Path.class);
		resolveOldFolderMethod.setAccessible(true);
		loadUploadTosTextMethod = TrainingCommandHandler.class.getDeclaredMethod("loadUploadTosText");
		loadUploadTosTextMethod.setAccessible(true);
	}

	@Test
	void resolveOldFolderReturnsOldAncestorOrParentFallback() throws Exception {
		Path root = Files.createTempDirectory("scamscreener-handler-resolve-old-");
		try {
			Path archived = root.resolve("old").resolve("training-data").resolve("file.csv.old.2");
			Files.createDirectories(archived.getParent());
			Files.writeString(archived, "x");
			Path resolvedOld = resolveOldFolder(archived);
			assertEquals("old", resolvedOld.getFileName().toString().toLowerCase());

			Path plain = root.resolve("file.csv");
			Files.writeString(plain, "x");
			Path fallback = resolveOldFolder(plain);
			assertEquals(root.toAbsolutePath().normalize(), fallback.toAbsolutePath().normalize());
		} finally {
			deleteTree(root);
		}
	}

	@Test
	void loadUploadTosTextReturnsBundledContent() throws Exception {
		String text = loadUploadTosText();
		assertNotNull(text);
		assertTrue(text.trim().length() > 50);
	}

	private static Path resolveOldFolder(Path archivePath) throws Exception {
		return (Path) resolveOldFolderMethod.invoke(null, archivePath);
	}

	private static String loadUploadTosText() throws Exception {
		return (String) loadUploadTosTextMethod.invoke(null);
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
