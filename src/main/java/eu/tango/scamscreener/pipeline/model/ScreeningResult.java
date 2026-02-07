package eu.tango.scamscreener.pipeline.model;

public record ScreeningResult(
	MessageEvent event,
	DetectionResult result,
	boolean shouldWarn,
	boolean muted
) {
}
