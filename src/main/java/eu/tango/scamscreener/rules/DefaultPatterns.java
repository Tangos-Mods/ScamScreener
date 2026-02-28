package eu.tango.scamscreener.rules;

import java.util.List;
import java.util.regex.Pattern;

public final class DefaultPatterns {
	public static final String LINK_PATTERN = "(https?://|www\\.|discord\\.gg/|t\\.me/)";
	public static final String URGENCY_PATTERN = "\\b(now|quick|fast|urgent|sofort|jetzt)\\b";
	public static final String PAYMENT_FIRST_PATTERN = "\\b(pay first|first payment|vorkasse|send first)\\b";
	public static final String ACCOUNT_DATA_PATTERN =
		"(?:\\b(?:password|passwort|2fa|email login)\\b|\\b(?:give|gimme)\\b.*\\bcode\\b|\\bcode\\b.*\\b(?:give|gimme)\\b)";
	public static final String TOO_GOOD_PATTERN = "\\b(free coins|free rank|dupe|100% safe|garantiert)\\b";
	public static final String TRUST_BAIT_PATTERN = "\\b(trust me|vertrau mir|legit)\\b";
	public static final String LEGACY_EXTERNAL_PLATFORM_PATTERN = "\\b(discord|telegram|t\\.me|dm me|add me)\\b";
	public static final String EXTERNAL_PLATFORM_PATTERN = "\\b(discord|telegram|t\\.me|dm me|add me|vc|voice chat|voice channel|call)\\b";
	public static final String MIDDLEMAN_PATTERN = "\\b(trusted middleman|legit middleman|middleman)\\b";
	public static final String FUNNEL_SERVICE_OFFER_PATTERN = "\\b(carry|service|offer|offering|sell|selling|helping)\\b";
	public static final String FUNNEL_FREE_OFFER_PATTERN = "\\b(free|for free|giveaway|free carry)\\b";
	public static final String FUNNEL_REP_REQUEST_PATTERN = "\\b(rep|reputation|vouch|voucher|feedback|rep me|vouch me)\\b";
	public static final String CHANNEL_REDIRECT_FRAGMENT = "(?:go to|join) [a-z0-9 ]{2,40} channel";
	public static final String CHANNEL_REDIRECT_PATTERN_TEXT = "\\b" + CHANNEL_REDIRECT_FRAGMENT + "\\b";
	public static final String LEGACY_FUNNEL_PLATFORM_REDIRECT_PATTERN = "\\b(discord|telegram|t\\.me|vc|voice chat|call|join vc)\\b";
	public static final String LEGACY_FUNNEL_PLATFORM_REDIRECT_PATTERN_V2 =
		"\\b(discord|telegram|t\\.me|vc|voice chat|call|join vc|" + CHANNEL_REDIRECT_FRAGMENT + ")\\b";
	public static final String FUNNEL_PLATFORM_REDIRECT_PATTERN =
		"\\b(discord|telegram|t\\.me|vc|voice chat|voice channel|call|join vc|" + CHANNEL_REDIRECT_FRAGMENT + ")\\b";
	public static final String FUNNEL_INSTRUCTION_INJECTION_PATTERN = "\\b(go to|type|do rep|copy this|run this|use command|join and)\\b";
	public static final String FUNNEL_COMMUNITY_ANCHOR_PATTERN = "\\b(sbz|hsb|sbm|skyblockz|hypixel skyblock)\\b";
	public static final String FUNNEL_NEGATIVE_INTENT_PATTERN = "\\b(guild recruit|guild req|guild only|looking for members|lf members|recruiting)\\b";
	public static final String DISCORD_HANDLE_PATTERN_TEXT = "@[a-z0-9._-]{2,32}";
	public static final String DISCORD_WORD_PATTERN_TEXT = "\\bdiscord\\b";
	public static final Pattern URGENCY_ALLOWLIST_PATTERN = Pattern.compile("\\b(auction|ah|flip|bin|bid|bidding)\\b");
	public static final Pattern TRADE_CONTEXT_ALLOWLIST_PATTERN =
		Pattern.compile("\\b(sell|selling|buy|buying|trade|trading|price|coins?|payment|pay|lf|lb)\\b");
	public static final Pattern COERCION_THREAT_PATTERN = Pattern.compile(
		"\\b(?:you\\s+will\\s+not\\s+get\\s+(?:your|ur)\\s+(?:stuff|items?|armor|gear)\\s+back"
			+ "|you\\s+won\\s+t\\s+get\\s+(?:your|ur)\\s+(?:stuff|items?|armor|gear)\\s+back"
			+ "|well\\s+then\\s+you\\s+will\\s+not\\s+get\\s+(?:your|ur)\\s+(?:stuff|items?|armor|gear)\\s+back"
			+ "|unless\\s+you\\s+(?:join|come)\\s+(?:vc|voice\\s+chat|voice\\s+channel|call))\\b"
	);
	public static final Pattern DISCORD_HANDLE_PATTERN = Pattern.compile(DISCORD_HANDLE_PATTERN_TEXT);
	public static final Pattern DISCORD_WORD_PATTERN = Pattern.compile(DISCORD_WORD_PATTERN_TEXT);
	public static final Pattern CHANNEL_REDIRECT_PATTERN = Pattern.compile(CHANNEL_REDIRECT_PATTERN_TEXT);
	public static final List<String> URGENCY_KEYWORDS = List.of(
		"now",
		"quick",
		"fast",
		"urgent",
		"asap",
		"immediately",
		"right",
		"sofort",
		"jetzt"
	);
	public static final List<String> URGENCY_PHRASES = List.of(
		"right now",
		"right away",
		"as soon as possible",
		"need it now",
		"need this now",
		"need this right now",
		"fast fast",
		"quick payment"
	);
	public static final List<String> TRUST_KEYWORDS = List.of(
		"trust",
		"trusted",
		"legit",
		"safe",
		"verified",
		"vouched",
		"reputable",
		"middleman"
	);
	public static final List<String> TRUST_PHRASES = List.of(
		"trust me",
		"i am legit",
		"its legit",
		"it's legit",
		"safe trade",
		"trusted middleman",
		"legit middleman"
	);
	public static final List<String> FOLDED_PLATFORM_HINTS = List.of(
		"discord",
		"telegram",
		"teamspeak",
		"voicechat",
		"discgg"
	);
	public static final List<String> LINK_SIGNAL_PLATFORM_HINTS = List.of(
		"discord",
		"telegram"
	);
	public static final List<String> LINK_REDIRECT_HINTS = List.of(
		"discord gg",
		"discord com invite",
		"t me"
	);
	public static final List<String> UPFRONT_PAYMENT_HINTS = List.of(
		"pay first",
		"send first",
		"you give me",
		"give me first",
		"before i give",
		"before i send"
	);

	private DefaultPatterns() {
	}
}
