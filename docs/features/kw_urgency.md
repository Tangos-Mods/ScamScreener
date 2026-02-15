# Feature: `kw_urgency`

The purpose of `kw_urgency` is straightforward: Captures urgency or time-pressure wording.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set to 1.0 when words like now/quick/fast/urgent/sofort/jetzt appear; otherwise 0.0.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Urgency is a common manipulation tactic that increases risky compliance.

For maintenance and tuning, the most useful debugging mindset is this: If over-triggering occurs, lower weight before removing the feature entirely.
