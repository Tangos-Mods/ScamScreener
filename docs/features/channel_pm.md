# Feature: `channel_pm`

The purpose of `channel_pm` is straightforward: Channel-context indicator for private messages.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set to 1.0 when channel equals pm (case-insensitive); otherwise 0.0.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Private channels can increase scam execution probability after public baiting.

For maintenance and tuning, the most useful debugging mindset is this: Ensure channel normalization is stable across parser paths.
