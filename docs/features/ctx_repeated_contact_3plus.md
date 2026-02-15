# Feature: `ctx_repeated_contact_3plus`

The purpose of `ctx_repeated_contact_3plus` is straightforward: Flags repeated contact attempts crossing a threshold.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set to 1.0 when repeated contact attempts are at least 3; else 0.0.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Persistence and repeated nudging correlate with coercive scam flows.

For maintenance and tuning, the most useful debugging mindset is this: Ensure repeat counting is keyed correctly per speaker to avoid cross-user contamination.
