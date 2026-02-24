package eu.tango.scamscreener.chat.mute;

import eu.tango.scamscreener.chat.IgnoredChatMessages;
import eu.tango.scamscreener.config.MutePatternsConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.tango.scamscreener.util.RegexSafety;

public final class MutePatternManager {
	private static final int REGEX_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
	private static final Logger LOGGER = LoggerFactory.getLogger(MutePatternManager.class);

	private final Set<String> patterns = new LinkedHashSet<>();
	private final List<Pattern> compiledPatterns = new ArrayList<>();
	private boolean enabled = true;
	private boolean notifyEnabled = true;
	private int notifyIntervalSeconds = 30;
	private long lastNotifyMillis = System.currentTimeMillis();
	private int blockedSinceLastNotify = 0;

	public void load() {
		MutePatternsConfig cfg = MutePatternsConfig.loadOrCreate();
		patterns.clear();
		compiledPatterns.clear();
		for (String pattern : cfg.patterns) {
			tryAddPattern(pattern);
		}
		enabled = cfg.enabled == null ? true : cfg.enabled;
		notifyEnabled = cfg.notifyEnabled;
		notifyIntervalSeconds = cfg.notifyIntervalSeconds;
	}

	public AddResult addPattern(String rawPattern) {
		String normalized = normalize(rawPattern);
		if (normalized == null) {
			return AddResult.INVALID;
		}
		if (patterns.contains(normalized)) {
			return AddResult.ALREADY_EXISTS;
		}
		if (!tryAddPattern(normalized)) {
			return AddResult.INVALID;
		}
		save();
		return AddResult.ADDED;
	}

	public boolean removePattern(String rawPattern) {
		String normalized = normalize(rawPattern);
		if (normalized == null) {
			return false;
		}
		if (!patterns.remove(normalized)) {
			return false;
		}
		rebuildCompiledPatterns();
		save();
		return true;
	}

	public List<String> allPatterns() {
		List<String> list = new ArrayList<>(patterns);
		list.sort(Comparator.naturalOrder());
		return list;
	}

	public boolean shouldBlock(String message) {
		if (!enabled || message == null || message.isBlank() || compiledPatterns.isEmpty()) {
			return false;
		}
		if (IgnoredChatMessages.isMuteExemptLine(message)) {
			return false;
		}
		for (Pattern pattern : compiledPatterns) {
			if (RegexSafety.safeFind(pattern, message, LOGGER, "mute pattern matching")) {
				blockedSinceLastNotify++;
				return true;
			}
		}
		return false;
	}

	public boolean shouldNotifyNow(long nowMillis) {
		if (!enabled || !notifyEnabled) {
			return false;
		}
		return nowMillis - lastNotifyMillis >= (long) notifyIntervalSeconds * 1000L;
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		save();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int consumeBlockedCount(long nowMillis) {
		lastNotifyMillis = nowMillis;
		int count = blockedSinceLastNotify;
		blockedSinceLastNotify = 0;
		return count;
	}

	public int notifyIntervalSeconds() {
		return notifyIntervalSeconds;
	}

	private boolean tryAddPattern(String pattern) {
		try {
			Pattern compiled = compilePattern(pattern);
			patterns.add(pattern);
			compiledPatterns.add(compiled);
			return true;
		} catch (PatternSyntaxException ignored) {
			return false;
		}
	}

	private void rebuildCompiledPatterns() {
		compiledPatterns.clear();
		for (String pattern : patterns) {
			try {
				compiledPatterns.add(compilePattern(pattern));
			} catch (PatternSyntaxException ignored) {
			}
		}
	}

	private static Pattern compilePattern(String pattern) {
		if (isLikelyRegex(pattern)) {
			return Pattern.compile(pattern, REGEX_FLAGS);
		}
		String wordPattern = "\\b" + Pattern.quote(pattern) + "\\b";
		return Pattern.compile(wordPattern, REGEX_FLAGS);
	}

	private static boolean isLikelyRegex(String pattern) {
		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			if ("\\.^$|?*+()[]{}".indexOf(c) >= 0) {
				return true;
			}
		}
		return false;
	}

	private void save() {
		MutePatternsConfig cfg = new MutePatternsConfig();
		cfg.patterns = allPatterns();
		cfg.enabled = enabled;
		cfg.notifyEnabled = notifyEnabled;
		cfg.notifyIntervalSeconds = notifyIntervalSeconds;
		MutePatternsConfig.save(cfg);
	}

	private static String normalize(String rawPattern) {
		if (rawPattern == null) {
			return null;
		}
		String trimmed = rawPattern.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	public enum AddResult {
		ADDED,
		ALREADY_EXISTS,
		INVALID
	}
}
