# Feature: `ctx_pushes_external_platform`

The purpose of `ctx_pushes_external_platform` is straightforward: Indicates behavior-level platform redirect pressure.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from behavior/intent context when external-platform push is present.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Cross-platform push strongly increases risk when combined with offer/rep/instruction steps.

For maintenance and tuning, the most useful debugging mindset is this: If missing, check upstream behavior and intent tagging first.
