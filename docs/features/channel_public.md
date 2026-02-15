# Feature: `channel_public`

The purpose of `channel_public` is straightforward: Channel-context indicator for public chat.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set to 1.0 when channel equals public; otherwise 0.0.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Helps the model distinguish public baiting from private execution phases.

For maintenance and tuning, the most useful debugging mindset is this: If always zero, channel parser normalization is likely broken.
