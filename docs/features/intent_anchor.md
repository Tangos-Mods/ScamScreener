# Feature: `intent_anchor`

The purpose of `intent_anchor` is straightforward: Intent-tag flag for community-anchor references.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set when known community anchor terms are present.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Anchors can be used to appear legitimate and lower victim skepticism.

For maintenance and tuning, the most useful debugging mindset is this: This is context-sensitive and should usually stay moderately weighted.
