# Feature: `behavior_hits_norm`

The purpose of `behavior_hits_norm` is straightforward: Encodes amount of behavior-stage evidence.

In practical model terms, this feature is represented as a **Normalized numeric** signal with the value range **0.0 to 1.0**. During extraction, it is computed as follows: Computed as clamped behaviorHits divided by 3.0.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Behavior-stage density helps AI separate isolated noise from consistent risk behavior.

For maintenance and tuning, the most useful debugging mindset is this: Review behavior-stage thresholds when this feature dominates.
