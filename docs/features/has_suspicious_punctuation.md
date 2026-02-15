# Feature: `has_suspicious_punctuation`

The purpose of `has_suspicious_punctuation` is straightforward: Captures punctuation patterns associated with spam pressure.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set to 1.0 when text contains !!!, ??, or $$; otherwise 0.0.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Aggressive punctuation is a weak but useful spam/manipulation signal.

For maintenance and tuning, the most useful debugging mindset is this: Keep weight modest; this is an auxiliary feature, not a primary detector.
