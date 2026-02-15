# Feature: `rapid_followup`

The purpose of `rapid_followup` is straightforward: Encodes how quickly the same speaker follows up.

In practical model terms, this feature is represented as a **Normalized numeric** signal with the value range **0.0 to 1.0**. During extraction, it is computed as follows: Set to 0.0 when delta is missing or non-positive, else computed as 1 minus clamped deltaMs divided by 120000.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Fast repeated follow-ups can indicate pressure and manipulation cadence.

For maintenance and tuning, the most useful debugging mindset is this: Clock/skew issues can flatten this feature; validate timestamp quality.
