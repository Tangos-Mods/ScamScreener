package eu.tango.scamscreener.pipeline.core;

import eu.tango.scamscreener.pipeline.model.IntentTag;
import eu.tango.scamscreener.pipeline.model.MessageEvent;
import eu.tango.scamscreener.pipeline.model.Signal;
import eu.tango.scamscreener.rules.DefaultPatterns;
import eu.tango.scamscreener.rules.ScamRules;
import eu.tango.scamscreener.util.RegexSafety;
import eu.tango.scamscreener.util.TextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class IntentTagger {
	private static final Logger LOGGER = LoggerFactory.getLogger(IntentTagger.class);

	private final RuleConfig ruleConfig;

	/**
	 * Derives high-level intent tags from existing stage signals and light text checks.
	 */
	public IntentTagger(RuleConfig ruleConfig) {
		this.ruleConfig = ruleConfig;
	}

	public TaggingResult tag(MessageEvent event, List<Signal> existingSignals) {
		if (event == null) {
			return TaggingResult.empty();
		}

		String normalized = TextUtil.normalizeForMatch(event.rawMessage());
		if (normalized.isBlank()) {
			normalized = TextUtil.normalizeForMatch(event.normalizedMessage());
		}
		if (normalized.isBlank()) {
			return TaggingResult.empty();
		}

		ScamRules.FunnelConfig config = ruleConfig.funnelConfig();
		Set<IntentTag> tags = EnumSet.noneOf(IntentTag.class);
		List<Signal> safeSignals = existingSignals == null ? List.of() : existingSignals;

		boolean hasUpfrontSignal = false;
		boolean hasPlatformSignal = false;
		boolean hasFreeSignal = false;
		boolean hasLinkSignal = false;

		for (Signal signal : safeSignals) {
			if (signal == null || signal.ruleId() == null) {
				continue;
			}
			switch (signal.ruleId()) {
				case UPFRONT_PAYMENT -> hasUpfrontSignal = true;
				case EXTERNAL_PLATFORM_PUSH, DISCORD_HANDLE -> hasPlatformSignal = true;
				case TOO_GOOD_TO_BE_TRUE -> hasFreeSignal = true;
				case SUSPICIOUS_LINK -> hasLinkSignal = true;
				default -> {
				}
			}
		}

		if (hasUpfrontSignal) {
			tags.add(IntentTag.PAYMENT_UPFRONT);
		}
		if (hasPlatformSignal) {
			tags.add(IntentTag.PLATFORM_REDIRECT);
		}
		if (hasFreeSignal) {
			tags.add(IntentTag.FREE_OFFER);
		}

		if (matches(config.serviceOfferPattern(), normalized)) {
			tags.add(IntentTag.SERVICE_OFFER);
		}
		if (matches(config.freeOfferPattern(), normalized)) {
			tags.add(IntentTag.FREE_OFFER);
		}
		if (matches(config.repRequestPattern(), normalized)) {
			tags.add(IntentTag.REP_REQUEST);
		}
		if (matches(config.instructionInjectionPattern(), normalized)) {
			tags.add(IntentTag.INSTRUCTION_INJECTION);
		}
		if (matches(config.communityAnchorPattern(), normalized)) {
			tags.add(IntentTag.COMMUNITY_ANCHOR);
		}
		if (matches(ruleConfig.behaviorPatterns().upfrontPayment(), normalized) || containsUpfrontPaymentPhrase(normalized)) {
			tags.add(IntentTag.PAYMENT_UPFRONT);
		}
		if (matches(config.platformRedirectPattern(), normalized)) {
			tags.add(IntentTag.PLATFORM_REDIRECT);
		}
		if (containsChannelRedirectInstruction(normalized)) {
			tags.add(IntentTag.PLATFORM_REDIRECT);
		}

		String foldedCompact = foldCompact(normalized);
		if (containsFoldedRedirect(foldedCompact)) {
			tags.add(IntentTag.PLATFORM_REDIRECT);
		}
		if (hasLinkSignal && containsLinkRedirectHint(normalized, foldedCompact)) {
			tags.add(IntentTag.PLATFORM_REDIRECT);
		}

		boolean negativeContext = matches(config.negativeIntentPattern(), normalized);
		if (negativeContext) {
			tags.remove(IntentTag.SERVICE_OFFER);
			tags.remove(IntentTag.FREE_OFFER);
		}

		return new TaggingResult(tags, negativeContext);
	}

	private static boolean matches(Pattern pattern, String text) {
		if (pattern == null || text == null || text.isBlank()) {
			return false;
		}
		return RegexSafety.safeFind(pattern, text, LOGGER, "intent pattern matching");
	}

	private static String foldCompact(String text) {
		if (text == null || text.isBlank()) {
			return "";
		}
		StringBuilder out = new StringBuilder(text.length());
		for (int i = 0; i < text.length(); i++) {
			char raw = Character.toLowerCase(text.charAt(i));
			char mapped = switch (raw) {
				case '0' -> 'o';
				case '1', '!' -> 'i';
				case '3' -> 'e';
				case '4', '@' -> 'a';
				case '5', '$' -> 's';
				case '7' -> 't';
				default -> raw;
			};
			if (Character.isLetterOrDigit(mapped)) {
				out.append(mapped);
			}
		}
		return out.toString();
	}

	private static boolean containsFoldedRedirect(String compact) {
		return containsAny(compact, DefaultPatterns.FOLDED_PLATFORM_HINTS);
	}

	private static boolean containsLinkRedirectHint(String normalized, String compact) {
		if (containsAny(compact, DefaultPatterns.LINK_SIGNAL_PLATFORM_HINTS)) {
			return true;
		}
		return containsAny(normalized, DefaultPatterns.LINK_REDIRECT_HINTS);
	}

	private static boolean containsChannelRedirectInstruction(String normalized) {
		if (normalized == null || normalized.isBlank()) {
			return false;
		}
		return RegexSafety.safeFind(DefaultPatterns.CHANNEL_REDIRECT_PATTERN, normalized, LOGGER, "channel redirect instruction");
	}

	private static boolean containsUpfrontPaymentPhrase(String normalized) {
		return containsAny(normalized, DefaultPatterns.UPFRONT_PAYMENT_HINTS);
	}

	private static boolean containsAny(String text, List<String> hints) {
		if (text == null || text.isBlank() || hints == null || hints.isEmpty()) {
			return false;
		}
		for (String hint : hints) {
			if (text.contains(hint)) {
				return true;
			}
		}
		return false;
	}

	public record TaggingResult(Set<IntentTag> tags, boolean negativeContext) {
		public TaggingResult {
			tags = tags == null ? Set.of() : Set.copyOf(tags);
		}

		public static TaggingResult empty() {
			return new TaggingResult(Set.of(), false);
		}
	}
}
