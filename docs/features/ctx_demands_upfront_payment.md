# Feature: `ctx_demands_upfront_payment`

The purpose of `ctx_demands_upfront_payment` is straightforward: Indicates demand for payment before delivery.

In practical model terms, this feature is represented as a **Binary** signal with the value range **0.0 or 1.0**. During extraction, it is computed as follows: Set from behavior context when upfront-payment pressure is detected.

Within the AI system, this feature is consumed by **Main head only**. That placement matters because it defines whether the signal influences only broad risk scoring or also the funnel-specialized decision path.

From a detection perspective, the reason this feature exists is simple: Upfront payment demand is one of the highest-value fraud indicators.

For maintenance and tuning, the most useful debugging mindset is this: If noisy, tune thresholding in behavior extraction before reducing model weight.
