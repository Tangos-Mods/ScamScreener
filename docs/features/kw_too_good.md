# Feature: `kw_too_good`

The purpose of `kw_too_good` is straightforward: Captures too-good-to-be-true offer language.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set to 1.0 when terms such as free/100%/guaranteed/garantiert/dupe/rank appear; otherwise 0.0.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Unrealistic value promises are classic scam bait.

For maintenance and tuning, the most useful debugging mindset is this: Pair with funnel and behavior features to avoid punishing harmless giveaways.
