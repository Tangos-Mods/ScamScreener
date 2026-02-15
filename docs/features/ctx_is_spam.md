# Feature: `ctx_is_spam`

The purpose of `ctx_is_spam` is straightforward: Indicates spam-like behavior context.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from behavior analysis when spam pressure patterns are detected.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Spam-like delivery amplifies suspicion for otherwise ambiguous content.

For maintenance and tuning, the most useful debugging mindset is this: Watch for event bursts that could mark benign broadcasts as spam.
