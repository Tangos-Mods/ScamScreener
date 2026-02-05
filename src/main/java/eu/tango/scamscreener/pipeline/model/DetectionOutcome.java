package eu.tango.scamscreener.pipeline.model;

public record DetectionOutcome(MessageEvent event, DetectionResult result) {
}
