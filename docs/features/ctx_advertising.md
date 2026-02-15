# Feature: `ctx_advertising`

The purpose of `ctx_advertising` is straightforward: Detects ad-like behavior context.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from behavior context when message style looks like service/shop advertising.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Advertising itself is neutral, but combined with redirect and urgency it becomes informative.

For maintenance and tuning, the most useful debugging mindset is this: Do not overweight this feature in ad-heavy communities.
