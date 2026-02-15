# Feature: `intent_offer`

The purpose of `intent_offer` is straightforward: Intent-tag flag for offer semantics.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from intent tagging when service/free offer intent is present.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Offer intent is the first stage of many funnel-like scam conversations.

For maintenance and tuning, the most useful debugging mindset is this: Negative-context suppression should clear this in recruitment-like messages.
