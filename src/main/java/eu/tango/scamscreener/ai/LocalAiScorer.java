package eu.tango.scamscreener.ai;

import eu.tango.scamscreener.config.LocalAiModelConfig;
import eu.tango.scamscreener.rules.ScamRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LocalAiScorer {
	private static final Set<String> FUNNEL_ONLY_DENSE_FEATURES = Set.copyOf(AiFeatureSpace.FUNNEL_DENSE_FEATURE_NAMES);
	private volatile ModelWeights model = ModelWeights.from(LocalAiModelConfig.loadOrCreate());

	public void reloadModel() {
		model = ModelWeights.from(LocalAiModelConfig.loadOrCreate());
	}

	public AiResult score(ScamRules.BehaviorContext context, int maxScore, double triggerProbability) {
		ScamRules.BehaviorContext safeContext = context == null ? emptyContext() : context;
		Map<String, Double> denseFeatures = AiFeatureSpace.extractDenseFeatures(safeContext);
		ModelWeights w = model;
		double linear = w.intercept;
		for (Map.Entry<String, Double> entry : denseFeatures.entrySet()) {
			Double weight = w.denseFeatureWeights.get(entry.getKey());
			if (weight == null) {
				continue;
			}
			linear += entry.getValue() * weight;
		}
		linear += tokenContribution(safeContext.message(), w.tokenWeights);

		double probability = sigmoid(linear);
		int rawScore = (int) Math.round(probability * clampScore(maxScore));
		boolean triggered = probability >= clampProbability(triggerProbability);
		int appliedScore = triggered ? rawScore : 0;
		String explanation = buildExplanation(
			safeContext.message(),
			denseFeatures,
			w.denseFeatureWeights,
			w.tokenWeights,
			null,
			true
		);

		return new AiResult(appliedScore, probability, triggered, explanation);
	}

	public AiResult scoreFunnelOnly(ScamRules.BehaviorContext context, int maxScore, double triggerProbability) {
		ScamRules.BehaviorContext safeContext = context == null ? emptyContext() : context;
		Map<String, Double> denseFeatures = AiFeatureSpace.extractDenseFeatures(safeContext);
		ModelWeights w = model;
		double linear = w.funnelHead.intercept();
		for (Map.Entry<String, Double> entry : denseFeatures.entrySet()) {
			if (!FUNNEL_ONLY_DENSE_FEATURES.contains(entry.getKey())) {
				continue;
			}
			Double weight = w.funnelHead.denseFeatureWeights().get(entry.getKey());
			if (weight == null) {
				continue;
			}
			linear += entry.getValue() * weight;
		}

		double probability = sigmoid(linear);
		int rawScore = (int) Math.round(probability * clampScore(maxScore));
		boolean triggered = probability >= clampProbability(triggerProbability);
		int appliedScore = triggered ? rawScore : 0;
		String explanation = buildExplanation(
			safeContext.message(),
			denseFeatures,
			w.funnelHead.denseFeatureWeights(),
			null,
			FUNNEL_ONLY_DENSE_FEATURES,
			false
		);

		return new AiResult(appliedScore, probability, triggered, explanation);
	}

	private static int clampScore(int value) {
		return Math.max(0, Math.min(100, value));
	}

	private static double clampProbability(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}

	private static ScamRules.BehaviorContext emptyContext() {
		return new ScamRules.BehaviorContext(
			"",
			"unknown",
			0L,
			false,
			false,
			false,
			false,
			0,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
			false,
			0,
			0.0,
			false,
			false,
			0,
			0,
			0,
			0,
			0
		);
	}

	private static double sigmoid(double x) {
		double clamped = Math.max(-30.0, Math.min(30.0, x));
		return 1.0 / (1.0 + Math.exp(-clamped));
	}

	private static double tokenContribution(String message, Map<String, Double> tokenWeights) {
		if (tokenWeights == null || tokenWeights.isEmpty()) {
			return 0.0;
		}

		Set<String> tokens = TokenFeatureExtractor.extractFeatureTokens(message);
		double sum = 0.0;
		for (String token : tokens) {
			Double weight = tokenWeights.get(token);
			if (weight != null) {
				sum += weight;
			}
		}
		return sum;
	}

	private static String buildExplanation(
		String message,
		Map<String, Double> denseFeatures,
		Map<String, Double> denseWeights,
		Map<String, Double> tokenWeights,
		Set<String> allowedDense,
		boolean includeTokens
	) {
		List<Contribution> contributions = new ArrayList<>();
		for (Map.Entry<String, Double> feature : denseFeatures.entrySet()) {
			if (allowedDense != null && !allowedDense.contains(feature.getKey())) {
				continue;
			}
			Double weight = denseWeights.get(feature.getKey());
			if (weight == null || feature.getValue() == null || feature.getValue() <= 0.0) {
				continue;
			}
			contributions.add(new Contribution("dense " + feature.getKey(), feature.getValue() * weight));
		}

		if (includeTokens && tokenWeights != null && !tokenWeights.isEmpty()) {
			for (String token : TokenFeatureExtractor.extractFeatureTokens(message)) {
				Double weight = tokenWeights.get(token);
				if (weight != null) {
					contributions.add(new Contribution("token " + token, weight));
				}
			}
		}

		if (contributions.isEmpty()) {
			return "Top model factors: none";
		}

		contributions.sort(Comparator.comparingDouble((Contribution c) -> Math.abs(c.value())).reversed());
		int limit = Math.min(4, contributions.size());
		StringBuilder text = new StringBuilder("Top model factors:");
		for (int i = 0; i < limit; i++) {
			Contribution c = contributions.get(i);
			text.append("\n- ").append(c.label()).append(" (").append(formatSigned(c.value())).append(")");
		}
		return text.toString();
	}

	private static String formatSigned(double value) {
		return String.format(Locale.ROOT, "%+.3f", value);
	}

	private record Contribution(String label, double value) {
	}

	public record AiResult(int score, double probability, boolean triggered, String explanation) {
	}

	private record ModelWeights(
		double intercept,
		Map<String, Double> denseFeatureWeights,
		Map<String, Double> tokenWeights,
		DenseHeadWeights funnelHead
	) {
		private static ModelWeights from(LocalAiModelConfig cfg) {
			Map<String, Double> dense = cfg.denseFeatureWeights == null
				? new LinkedHashMap<>(AiFeatureSpace.defaultDenseWeights())
				: cfg.denseFeatureWeights;
			int maxTokenWeights = LocalAiModelConfig.normalizeMaxTokenWeights(cfg.maxTokenWeights);
			Map<String, Double> tokens = LocalAiModelConfig.pruneTokenWeights(cfg.tokenWeights, maxTokenWeights);
			DenseHeadWeights funnel = DenseHeadWeights.from(cfg, dense);
			return new ModelWeights(cfg.intercept, dense, tokens, funnel);
		}
	}

	private record DenseHeadWeights(double intercept, Map<String, Double> denseFeatureWeights) {
		private static DenseHeadWeights from(LocalAiModelConfig cfg, Map<String, Double> mainDense) {
			Map<String, Double> out = new LinkedHashMap<>(AiFeatureSpace.defaultFunnelDenseWeights());
			double headIntercept = cfg.intercept;
			LocalAiModelConfig.DenseHeadConfig source = cfg.funnelHead;
			if (source != null) {
				if (Double.isFinite(source.intercept)) {
					headIntercept = source.intercept;
				}
				if (source.denseFeatureWeights != null) {
					for (Map.Entry<String, Double> entry : source.denseFeatureWeights.entrySet()) {
						if (!AiFeatureSpace.isFunnelDenseFeature(entry.getKey()) || entry.getValue() == null) {
							continue;
						}
						out.put(entry.getKey(), entry.getValue());
					}
				}
			}

			for (String key : AiFeatureSpace.FUNNEL_DENSE_FEATURE_NAMES) {
				if (out.containsKey(key)) {
					continue;
				}
				Double main = mainDense.get(key);
				out.put(key, main == null ? 0.0 : main);
			}

			return new DenseHeadWeights(headIntercept, out);
		}
	}
}
