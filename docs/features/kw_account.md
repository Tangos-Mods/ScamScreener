# Feature: `kw_account`

The purpose of `kw_account` is straightforward: Detects account-credential vocabulary.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set to 1.0 when normalized text contains account/security terms such as password/2fa/code/email/login; otherwise 0.0.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Credential-related language usually indicates account-takeover or social-engineering attempts.

For maintenance and tuning, the most useful debugging mindset is this: False negatives often come from obfuscation; add synonyms in preprocessing or adapt vocabulary.
