# Feature: `ctx_requests_sensitive_data`

The purpose of `ctx_requests_sensitive_data` is straightforward: Indicates requests for sensitive account information.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from behavior context when sensitive-data request signals fire.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Sensitive-data requests are highly predictive for account compromise attempts.

For maintenance and tuning, the most useful debugging mindset is this: High false positives usually indicate overly broad account keyword logic upstream.
