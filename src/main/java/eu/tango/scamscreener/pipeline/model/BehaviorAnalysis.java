package eu.tango.scamscreener.pipeline.model;

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
