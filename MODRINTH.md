## ScamScreener 2.5.0

This release retunes the default v2 pipeline to reduce noisy review alerts from common SkyBlock chat traffic.

- raised the default review threshold from `1` to `15`
- disabled `TrendStage` by default and reduced its repeat-wave fallback tuning
- tightened `BehaviorStage` burst defaults
- reduced default scores for generic Discord/trust hits and the `external after contact` funnel step
- tightened the fuzzy `join my discord` match and reduced the fuzzy `trust me` score
- bumped the `RULES` schema version, so existing `rules.json` files are reset to the new defaults on load
