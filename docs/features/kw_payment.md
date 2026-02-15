# Feature: `kw_payment`

The purpose of `kw_payment` is straightforward: Detects payment-first or money-transfer language in the message text.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set to 1.0 when normalized text contains payment-related words such as pay/payment/vorkasse/coins/money/btc/crypto; otherwise 0.0.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Payment pressure is a strong scam prior in trade-related chats, especially when combined with redirect or instruction intent.

For maintenance and tuning, the most useful debugging mindset is this: If this never fires, inspect text normalization and verify that payment vocabulary still matches your server language.
