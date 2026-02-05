package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.List;
import java.util.Map;

public record DetectionResult(
	double totalScore,
	DetectionLevel level,
	List<Signal> signals,
	Map<ScamRules.ScamRule, String> triggeredRules,
	boolean shouldCapture,
	List<String> evaluatedMessages
) {
	public DetectionResult {
		signals = signals == null ? List.of() : List.copyOf(signals);
		triggeredRules = triggeredRules == null ? Map.of() : Map.copyOf(triggeredRules);
		evaluatedMessages = evaluatedMessages == null ? List.of() : List.copyOf(evaluatedMessages);
	}
}
