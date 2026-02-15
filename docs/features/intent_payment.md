# Feature: `intent_payment`

The purpose of `intent_payment` is straightforward: Intent-tag flag for upfront payment semantics.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from intent tagging when payment-upfront intent is present.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Payment intent is a high-value signal in trade abuse scenarios.

For maintenance and tuning, the most useful debugging mindset is this: Check language variants and slang to maintain recall.
