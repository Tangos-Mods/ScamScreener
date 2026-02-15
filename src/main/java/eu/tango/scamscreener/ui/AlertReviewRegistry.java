package eu.tango.scamscreener.ui;

import eu.tango.scamscreener.rules.ScamRules;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AlertReviewRegistry {
	private static final int MAX_ENTRIES = 120;
	private static final Map<String, AlertContext> RECENT = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, AlertContext> eldest) {
			return size() > MAX_ENTRIES;
		}
	};

	private AlertReviewRegistry() {
	}

	public static String register(
		String playerName,
		ScamRules.ScamAssessment assessment,
		Map<ScamRules.ScamRule, Double> ruleWeights
	) {
		String id = UUID.randomUUID().toString().replace("-", "");
		String safePlayerName = playerName == null || playerName.isBlank() ? "unknown" : playerName.trim();
		ScamRules.ScamAssessment safeAssessment = assessment == null
			? new ScamRules.ScamAssessment(0, ScamRules.ScamRiskLevel.LOW, Set.of(), Map.of(), null, List.of())
			: assessment;
		Map<ScamRules.ScamRule, Double> safeRuleWeights = ruleWeights == null ? Map.of() : Map.copyOf(ruleWeights);

		AlertContext context = new AlertContext(
			id,
			safePlayerName,
			safeAssessment.riskScore(),
			safeAssessment.riskLevel() == null ? ScamRules.ScamRiskLevel.LOW : safeAssessment.riskLevel(),
			safeAssessment.triggeredRules() == null ? Set.of() : Set.copyOf(safeAssessment.triggeredRules()),
			safeAssessment.ruleDetails() == null ? Map.of() : Map.copyOf(safeAssessment.ruleDetails()),
			safeRuleWeights,
			safeAssessment.allEvaluatedMessages() == null ? List.of() : List.copyOf(safeAssessment.allEvaluatedMessages())
		);
		RECENT.put(id, context);
		return id;
	}

	public static AlertContext contextById(String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		return RECENT.get(id.trim());
	}

	public static String bestRuleCode(AlertContext context) {
		if (context == null) {
			return null;
		}
		ScamRules.ScamRule best = null;
		double bestScore = Double.NEGATIVE_INFINITY;

		for (Map.Entry<ScamRules.ScamRule, Double> entry : context.ruleWeights().entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) {
				continue;
			}
			double score = entry.getValue();
			if (best == null || score > bestScore) {
				best = entry.getKey();
				bestScore = score;
			}
		}

		if (best == null && !context.triggeredRules().isEmpty()) {
			best = context.triggeredRules().stream().sorted(Comparator.naturalOrder()).findFirst().orElse(null);
		}
		return best == null ? null : best.name();
	}

	public record AlertContext(
		String id,
		String playerName,
		int riskScore,
		ScamRules.ScamRiskLevel riskLevel,
		Set<ScamRules.ScamRule> triggeredRules,
		Map<ScamRules.ScamRule, String> ruleDetails,
		Map<ScamRules.ScamRule, Double> ruleWeights,
		List<String> evaluatedMessages
	) {
	}
}
