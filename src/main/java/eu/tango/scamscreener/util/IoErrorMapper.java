package eu.tango.scamscreener.util;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public final class IoErrorMapper {
	private IoErrorMapper() {
	}

	public static String trainingErrorDetail(IOException error, Path trainingPath) {
		return fileErrorDetail(error, trainingPath, "Training data file not found: ", "Access denied while reading training data: ");
	}

	public static String fileErrorDetail(Exception error, Path expectedPath, String notFoundPrefix, String accessDeniedPrefix) {
		if (error == null) {
			return "unknown error";
		}
		if (error instanceof NoSuchFileException missing) {
			String file = missing.getFile();
			return (notFoundPrefix == null ? "File not found: " : notFoundPrefix) + (file == null ? "unknown" : file);
		}
		if (error instanceof AccessDeniedException denied) {
			String file = denied.getFile();
			return (accessDeniedPrefix == null ? "Access denied: " : accessDeniedPrefix) + (file == null ? "unknown" : file);
		}
		String message = error.getMessage();
		if (message == null || message.isBlank()) {
			return error.getClass().getSimpleName();
		}
		String trimmed = message.trim();
		if (expectedPath != null && trimmed.equals(expectedPath.toString())) {
			return (notFoundPrefix == null ? "File not found: " : notFoundPrefix) + trimmed;
		}
		return trimmed;
	}
}
