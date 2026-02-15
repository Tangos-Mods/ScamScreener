# Feature: `funnel_hits_norm`

The purpose of `funnel_hits_norm` is straightforward: Encodes amount of funnel-stage evidence.

In practical model terms, this feature is represented as a **Normalized numeric** signal with the value range **0.0 to 1.0**. During extraction, it is computed as follows: Computed as clamped funnelHits divided by 2.0.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Allows AI to amplify confidence when explicit funnel detections already exist.

For maintenance and tuning, the most useful debugging mindset is this: If flat zero, inspect funnel-stage enablement and signal wiring.
