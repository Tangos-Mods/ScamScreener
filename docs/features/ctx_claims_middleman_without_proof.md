# Feature: `ctx_claims_middleman_without_proof`

The purpose of `ctx_claims_middleman_without_proof` is straightforward: Captures unsupported trusted-middleman claims.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from behavior context when middleman trust claims appear without corroboration.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Fake authority/middleman framing is a common scam persuasion method.

For maintenance and tuning, the most useful debugging mindset is this: Review local market jargon to separate normal brokerage terms from manipulative claims.
