# Vocabulary Selection and Retention

Token vocabulary is not hardcoded. It is built from labeled samples by counting extracted token features, measuring how much each token shifts positive-label rate relative to the global base rate, and ranking candidates by discriminative strength with frequency-aware scaling.

Only tokens that meet minimum count requirements are retained, and the final vocabulary is capped by maximum size. This keeps the model compact and avoids unstable weights for one-off phrases.

The practical implication is important: token behavior is data-dependent. If the training distribution changes, the token vocabulary and weight profile should be refreshed rather than manually patched feature by feature.
