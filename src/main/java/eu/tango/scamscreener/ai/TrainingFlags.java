package eu.tango.scamscreener.ai;

import java.util.Locale;
import java.util.StringJoiner;

public final class TrainingFlags {
	public enum Flag {
		PUSHES_EXTERNAL_PLATFORM("pushes_external_platform"),
		DEMANDS_UPFRONT_PAYMENT("demands_upfront_payment"),
		REQUESTS_SENSITIVE_DATA("requests_sensitive_data"),
		CLAIMS_MIDDLEMAN_WITHOUT_PROOF("claims_middleman_without_proof"),
		REPEATED_CONTACT_ATTEMPTS("repeated_contact_attempts"),
		IS_SPAM("is_spam"),
		ASKS_FOR_STUFF("asks_for_stuff"),
		ADVERTISING("advertising");

		private final String csvColumn;

		Flag(String csvColumn) {
			this.csvColumn = csvColumn;
		}

		public String csvColumn() {
			return csvColumn;
		}
	}

	private TrainingFlags() {
	}

	public static String csvColumnsHeader() {
		StringJoiner joiner = new StringJoiner(",");
		for (Flag flag : Flag.values()) {
			joiner.add(flag.csvColumn());
		}
		return joiner.toString();
	}

	public static final class Values {
		private final int[] byIndex = new int[Flag.values().length];

		public Values set(Flag flag, int value) {
			byIndex[flag.ordinal()] = value > 0 ? 1 : 0;
			return this;
		}

		public int get(Flag flag) {
			return byIndex[flag.ordinal()];
		}

		public String toCsvColumns() {
			StringJoiner joiner = new StringJoiner(",");
			for (Flag flag : Flag.values()) {
				joiner.add(String.valueOf(get(flag)));
			}
			return joiner.toString();
		}

		public static Values fromCsv(java.util.List<String> cols, int startIndex) {
			Values values = new Values();
			for (Flag flag : Flag.values()) {
				int idx = startIndex + flag.ordinal();
				int parsed = idx < cols.size() ? parseBinary(cols.get(idx)) : 0;
				values.set(flag, parsed);
			}
			return values;
		}

		private static int parseBinary(String value) {
			if (value == null || value.isBlank()) {
				return 0;
			}
			try {
				return Integer.parseInt(value.trim()) > 0 ? 1 : 0;
			} catch (NumberFormatException ignored) {
				String normalized = value.trim().toLowerCase(Locale.ROOT);
				return ("true".equals(normalized) || "yes".equals(normalized)) ? 1 : 0;
			}
		}
	}
}
