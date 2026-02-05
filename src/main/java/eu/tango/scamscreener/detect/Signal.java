package eu.tango.scamscreener.detect;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.List;

public record Signal(
	String id,
	SignalSource source,
	double weight,
	String evidence,
	ScamRules.ScamRule ruleId,
	List<String> relatedMessages
) {
	public Signal {
		relatedMessages = relatedMessages == null ? List.of() : List.copyOf(relatedMessages);
	}
}
