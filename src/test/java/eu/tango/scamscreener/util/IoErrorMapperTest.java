package eu.tango.scamscreener.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IoErrorMapperTest {
	@Test
	void trainingErrorDetailReturnsUnknownForNullError() {
		assertEquals("unknown error", IoErrorMapper.trainingErrorDetail(null, Path.of("training.csv")));
	}

	@Test
	void trainingErrorDetailMapsMissingFile() {
		IOException error = new NoSuchFileException("training.csv");

		assertEquals(
			"Training data file not found: training.csv",
			IoErrorMapper.trainingErrorDetail(error, null)
		);
	}

	@Test
	void trainingErrorDetailMapsAccessDenied() {
		IOException error = new AccessDeniedException("training.csv");

		assertEquals(
			"Access denied while reading training data: training.csv",
			IoErrorMapper.trainingErrorDetail(error, null)
		);
	}

	@Test
	void trainingErrorDetailTreatsPathOnlyMessageAsMissingFile() {
		Path trainingPath = Path.of("training.csv");
		IOException error = new IOException(trainingPath.toString());

		assertEquals(
			"Training data file not found: training.csv",
			IoErrorMapper.trainingErrorDetail(error, trainingPath)
		);
	}

	@Test
	void trainingErrorDetailFallsBackToSimpleClassNameForBlankMessage() {
		IOException error = new IOException(" ");

		assertEquals("IOException", IoErrorMapper.trainingErrorDetail(error, null));
	}
}
