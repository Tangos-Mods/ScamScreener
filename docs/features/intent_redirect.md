# Feature: `intent_redirect`

The purpose of `intent_redirect` is straightforward: Intent-tag flag for platform/channel redirection.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from intent tagging when redirect intent is found.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Redirect intent is a central pivot in off-platform scam funnels.

For maintenance and tuning, the most useful debugging mindset is this: If underfiring, broaden redirect heuristics for obfuscated platform mentions.
