# Feature: `intent_instruction`

The purpose of `intent_instruction` is straightforward: Intent-tag flag for explicit instruction injection.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from intent tagging when imperative instruction patterns are detected.

Within the AI system, this feature is consumed by **Main head and Funnel head**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Instruction steps often mark the execution phase of a scam flow.

For maintenance and tuning, the most useful debugging mindset is this: Command-heavy benign chats can trigger this; use with other funnel features.
