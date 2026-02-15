# Feature: `funnel_full_chain`

The purpose of `funnel_full_chain` is straightforward: Marks whether a full funnel chain is currently present.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from funnel context tracking when full-chain criteria are met.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Full-chain evidence is one of the strongest conversation-structure risk cues.

For maintenance and tuning, the most useful debugging mindset is this: If absent despite obvious chains, verify window size/time constraints.
