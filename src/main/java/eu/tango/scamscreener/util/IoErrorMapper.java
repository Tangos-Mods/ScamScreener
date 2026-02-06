package eu.tango.scamscreener.util;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public final class IoErrorMapper {
	private IoErrorMapper() {
	}

	public static String trainingErrorDetail(IOException error, Path trainingPath) {
		if (error == null) {
			return "unknown error";
		}
		if (error instanceof NoSuchFileException missing) {
			String file = missing.getFile();
			return "Training data file not found: " + (file == null ? "unknown" : file);
		}
		if (error instanceof AccessDeniedException denied) {
			String file = denied.getFile();
			return "Access denied while reading training data: " + (file == null ? "unknown" : file);
		}
		String message = error.getMessage();
		if (message == null || message.isBlank()) {
			return error.getClass().getSimpleName();
		}
		String trimmed = message.trim();
		if (trainingPath != null && trimmed.equals(trainingPath.toString())) {
			return "Training data file not found: " + trimmed;
		}
		return trimmed;
	}
}
