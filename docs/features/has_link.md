# Feature: `has_link`

The purpose of `has_link` is straightforward: Signals explicit link presence.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set to 1.0 if normalized text contains http://, https://, or www.; otherwise 0.0.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Links often carry phishing or off-platform routing risk.

For maintenance and tuning, the most useful debugging mindset is this: This does not parse all URL variants; extend detectors if shortened links are common.
