package eu.tango.scamscreener.ai;

import java.util.Locale;

final class TrainingTypeAiClassifier {
	private static final double THRESHOLD = 0.55;

	TrainingFlags.Values predict(String message) {
		String text = normalize(message);

		double externalPlatformScore = 0.0;
		externalPlatformScore += tokenScore(text, 1.2, "discord", "telegram", "t.me");
		externalPlatformScore += tokenScore(text, 0.6, "dm me", "add me", "msg me", "message me");
		externalPlatformScore += tokenScore(text, 0.5, "server", "join");

		double upfrontPaymentScore = 0.0;
		upfrontPaymentScore += tokenScore(text, 1.4, "pay first", "send first", "vorkasse", "upfront");
		upfrontPaymentScore += tokenScore(text, 0.8, "payment first", "first payment", "before trade");
		upfrontPaymentScore += tokenScore(text, 0.4, "coins first", "money first");

		double sensitiveDataScore = 0.0;
		sensitiveDataScore += tokenScore(text, 1.5, "password", "passwort", "2fa", "auth code");
		sensitiveDataScore += tokenScore(text, 0.8, "email login", "verification code", "otp");
		sensitiveDataScore += tokenScore(text, 0.5, "login", "email");

		double middlemanScore = 0.0;
		middlemanScore += tokenScore(text, 1.3, "trusted middleman", "legit middleman");
		middlemanScore += tokenScore(text, 0.8, "middleman", "mm");
		middlemanScore += tokenScore(text, 0.4, "trust me", "trusted", "safe trade");

		double repeatedContactScore = 0.0;
		repeatedContactScore += tokenScore(text, 1.0, "again", "still waiting", "reply", "respond");
		repeatedContactScore += tokenScore(text, 0.6, "??", "!!!", "last chance");

		double spamScore = 0.0;
		spamScore += tokenScore(text, 1.1, "spam", "buy now", "cheap", "fast carry", "last chance");
		spamScore += tokenScore(text, 0.7, "!!!", "???", "join now", "limited");

		double askingForStuffScore = 0.0;
		askingForStuffScore += tokenScore(text, 1.2, "can i borrow", "lend me", "can i have");
		askingForStuffScore += tokenScore(text, 0.8, "borrow", "give me", "spare", "free items");

		double advertisingScore = 0.0;
		advertisingScore += tokenScore(text, 1.3, "/visit", "visit me", "join my", "discord.gg", "t.me");
		advertisingScore += tokenScore(text, 0.8, "selling", "shop", "service", "carry");

		return new TrainingFlags.Values()
			.set(TrainingFlags.Flag.PUSHES_EXTERNAL_PLATFORM, bool(sigmoid(externalPlatformScore) >= THRESHOLD))
			.set(TrainingFlags.Flag.DEMANDS_UPFRONT_PAYMENT, bool(sigmoid(upfrontPaymentScore) >= THRESHOLD))
			.set(TrainingFlags.Flag.REQUESTS_SENSITIVE_DATA, bool(sigmoid(sensitiveDataScore) >= THRESHOLD))
			.set(TrainingFlags.Flag.CLAIMS_MIDDLEMAN_WITHOUT_PROOF, bool(sigmoid(middlemanScore) >= THRESHOLD))
			.set(TrainingFlags.Flag.REPEATED_CONTACT_ATTEMPTS, bool(sigmoid(repeatedContactScore) >= THRESHOLD))
			.set(TrainingFlags.Flag.IS_SPAM, bool(sigmoid(spamScore) >= THRESHOLD))
			.set(TrainingFlags.Flag.ASKS_FOR_STUFF, bool(sigmoid(askingForStuffScore) >= THRESHOLD))
			.set(TrainingFlags.Flag.ADVERTISING, bool(sigmoid(advertisingScore) >= THRESHOLD));
	}

	private static double tokenScore(String text, double weight, String... tokens) {
		for (String token : tokens) {
			if (text.contains(token)) {
				return weight;
			}
		}
		return 0.0;
	}

	private static int bool(boolean value) {
		return value ? 1 : 0;
	}

	private static String normalize(String text) {
		return text == null ? "" : text.toLowerCase(Locale.ROOT);
	}

	private static double sigmoid(double x) {
		return 1.0 / (1.0 + Math.exp(-x));
	}
}
