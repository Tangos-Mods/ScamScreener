# Feature: `ctx_too_good_to_be_true`

The purpose of `ctx_too_good_to_be_true` is straightforward: Behavior-level flag for unrealistic offers.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from behavior analysis when too-good conditions are detected.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Provides a context-level counterpart to lexical offer hype features.

For maintenance and tuning, the most useful debugging mindset is this: If duplicate influence with kw_too_good is too strong, lower one weight instead of disabling both.
