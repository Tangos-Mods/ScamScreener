package eu.tango.scamscreener.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ArchiveFileUtil {
	private ArchiveFileUtil() {
	}

	public static Path nextArchiveTarget(Path baseFile, Path archiveDir) throws IOException {
		Files.createDirectories(archiveDir);
		int index = 1;
		Path target = archiveDir.resolve(baseFile.getFileName() + ".old." + index);
		while (Files.exists(target)) {
			index++;
			target = archiveDir.resolve(baseFile.getFileName() + ".old." + index);
		}
		return target;
	}
}
