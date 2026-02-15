# Feature: `funnel_partial_chain`

The purpose of `funnel_partial_chain` is straightforward: Marks whether a partial funnel chain is currently present.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from funnel tracking when partial chain patterns are satisfied.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Captures meaningful sequence risk even before a full chain is complete.

For maintenance and tuning, the most useful debugging mindset is this: Use with funnel_step_norm to avoid overreacting to weak partials.
