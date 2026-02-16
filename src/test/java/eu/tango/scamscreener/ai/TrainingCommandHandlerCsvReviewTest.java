package eu.tango.scamscreener.ai;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TrainingCommandHandlerCsvReviewTest {
	@Test
	void parseTrainingCsvRowsKeepsDuplicateRowsAndLineBasedIds() throws Exception {
		List<String> lines = List.of(
			"message,label",
			"\"trade with middleman\",0",
			"\"trade with middleman\",1",
			"\"safe market offer\",0"
		);

		List<TrainingCommandHandler.TrainingCsvReviewRow> rows = TrainingCommandHandler.parseTrainingCsvRows(lines);
		assertEquals(3, rows.size());
		assertEquals("trade with middleman", rows.get(0).message());
		assertEquals("trade with middleman", rows.get(1).message());
		assertNotEquals(rows.get(0).rowId(), rows.get(1).rowId());
		assertEquals("2", rows.get(0).rowId());
		assertEquals("3", rows.get(1).rowId());
		assertEquals(0, rows.get(0).currentLabel());
		assertEquals(1, rows.get(1).currentLabel());
	}

	@Test
	void applyCsvReviewUpdatesChangesOnlySelectedLine() throws Exception {
		List<String> lines = List.of(
			"message,label",
			"\"trade with middleman\",0",
			"\"trade with middleman\",1",
			"\"safe market offer\",0"
		);
		Map<Integer, Integer> requested = new LinkedHashMap<>();
		requested.put(2, 1);

		TrainingCommandHandler.CsvUpdateResult result = TrainingCommandHandler.applyCsvReviewUpdates(lines, requested);
		assertEquals(1, result.changedRows());
		assertEquals("message,label", result.lines().get(0));
		assertEquals("\"trade with middleman\",1", result.lines().get(1));
		assertEquals("\"trade with middleman\",1", result.lines().get(2));
		assertEquals("\"safe market offer\",0", result.lines().get(3));
	}

	@Test
	void applyCsvReviewUpdatesDeletesRowsMarkedIgnored() throws Exception {
		List<String> lines = List.of(
			"message,label",
			"\"trade with middleman\",0",
			"\"trade with middleman\",1",
			"\"safe market offer\",0"
		);
		Map<Integer, Integer> requested = new LinkedHashMap<>();
		requested.put(3, -1);

		TrainingCommandHandler.CsvUpdateResult result = TrainingCommandHandler.applyCsvReviewUpdates(lines, requested);
		assertEquals(1, result.changedRows());
		assertEquals(3, result.lines().size());
		assertEquals("message,label", result.lines().get(0));
		assertEquals("\"trade with middleman\",0", result.lines().get(1));
		assertEquals("\"safe market offer\",0", result.lines().get(2));
	}
}
