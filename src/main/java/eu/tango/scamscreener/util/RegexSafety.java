package eu.tango.scamscreener.util;

import org.slf4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegexSafety {
	private RegexSafety() {
	}

	public static boolean safeFind(Pattern pattern, String input, Logger logger, String context) {
		if (pattern == null || input == null || input.isBlank()) {
			return false;
		}
		try {
			return pattern.matcher(input).find();
		} catch (StackOverflowError error) {
			warn(logger, context);
			return false;
		}
	}

	public static boolean safeMatches(Pattern pattern, String input, Logger logger, String context) {
		if (pattern == null || input == null || input.isBlank()) {
			return false;
		}
		try {
			return pattern.matcher(input).matches();
		} catch (StackOverflowError error) {
			warn(logger, context);
			return false;
		}
	}

	public static String safeFirstMatch(Pattern pattern, String input, Logger logger, String context) {
		if (pattern == null || input == null || input.isBlank()) {
			return null;
		}
		try {
			Matcher matcher = pattern.matcher(input);
			if (!matcher.find()) {
				return null;
			}
			return matcher.group();
		} catch (StackOverflowError error) {
			warn(logger, context);
			return null;
		}
	}

	public static String safePatternReplaceAll(Pattern pattern, String input, String replacement, Logger logger, String context) {
		if (pattern == null || input == null) {
			return input == null ? "" : input;
		}
		try {
			return pattern.matcher(input).replaceAll(replacement);
		} catch (StackOverflowError error) {
			warn(logger, context);
			return input;
		}
	}

	public static String safeStringReplaceAll(String input, String regex, String replacement, Logger logger, String context) {
		if (input == null) {
			return "";
		}
		try {
			return input.replaceAll(regex, replacement);
		} catch (StackOverflowError error) {
			warn(logger, context);
			return input;
		}
	}

	private static void warn(Logger logger, String context) {
		if (logger == null) {
			return;
		}
		String safeContext = context == null || context.isBlank() ? "unknown regex context" : context;
		logger.warn("Skipped regex execution due to StackOverflowError ({})", safeContext);
	}
}
