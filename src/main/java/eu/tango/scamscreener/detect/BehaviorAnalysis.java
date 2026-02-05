package eu.tango.scamscreener.detect;

public record BehaviorAnalysis(
	String message,
	String normalizedMessage,
	boolean pushesExternalPlatform,
	boolean demandsUpfrontPayment,
	boolean requestsSensitiveData,
	boolean claimsTrustedMiddlemanWithoutProof,
	int repeatedContactAttempts
) {
}
