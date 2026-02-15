# Feature: `kw_trust`

The purpose of `kw_trust` is straightforward: Captures explicit trust framing.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set to 1.0 when terms such as trust/legit/safe/trusted/middleman occur; otherwise 0.0.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Trust language can be benign, but in risky contexts it often appears in persuasion funnels.

For maintenance and tuning, the most useful debugging mindset is this: Treat as context-sensitive: tune weight against false positives in legit vouching conversations.
