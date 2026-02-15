# Feature: `ctx_asks_for_stuff`

The purpose of `ctx_asks_for_stuff` is straightforward: Marks contexts where the sender asks for items/money/resources.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from behavior context when request-for-value patterns appear.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Resource-request intent is important in social-engineering attempts.

For maintenance and tuning, the most useful debugging mindset is this: Tune in relation to trade channels where normal requests are frequent.
