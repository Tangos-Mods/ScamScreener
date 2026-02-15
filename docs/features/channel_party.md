# Feature: `channel_party`

The purpose of `channel_party` is straightforward: Channel-context indicator for party chat.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set to 1.0 when channel equals party; otherwise 0.0.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Party chat often contains trade coordination, useful as contextual prior.

For maintenance and tuning, the most useful debugging mindset is this: Treat as contextual prior only; never as standalone risk evidence.
