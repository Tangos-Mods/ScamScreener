# Feature: `intent_rep`

The purpose of `intent_rep` is straightforward: Intent-tag flag for reputation/vouch requests.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from intent tagging when rep/vouch request intent appears.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Rep requests often bridge offer language to trust manipulation.

For maintenance and tuning, the most useful debugging mindset is this: Legit rep chatter is common; rely on sequence context, not this feature alone.
