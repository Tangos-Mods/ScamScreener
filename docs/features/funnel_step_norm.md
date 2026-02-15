# Feature: `funnel_step_norm`

The purpose of `funnel_step_norm` is straightforward: Represents current funnel progression depth.

In practical model terms, this feature is represented as a **Normalized numeric** signal with the value range **0.0 to 1.0**. During extraction, it is computed as follows: Computed as clamped funnelStepIndex divided by 4.0.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Gives the model a smooth measure of how far a conversation has progressed.

For maintenance and tuning, the most useful debugging mindset is this: If always near zero, inspect funnel tracker state and speaker identity mapping.
