package eu.tango.scamscreener.pipeline.model;

public record DetectionEvaluation(
	MessageEvent event,
	DetectionResult result,
	DetectionDecision decision
) {
}
