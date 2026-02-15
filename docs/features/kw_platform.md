# Feature: `kw_platform`

The purpose of `kw_platform` is straightforward: Captures platform-shift vocabulary.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set to 1.0 when words like discord/telegram/t.me/server/dm/vc/voice appear; otherwise 0.0.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Platform moves are frequently used to evade moderation and isolate victims.

For maintenance and tuning, the most useful debugging mindset is this: If your community uses Discord heavily, rely on combination effects rather than this feature alone.
