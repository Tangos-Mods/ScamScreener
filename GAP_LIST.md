# v1 -> v2 Gap List

This file tracks the remaining behavior gaps between the old v1 detection logic and the current v2 pipeline.

## Baseline

- v2 is structurally ahead of v1:
  - unified stage contract
  - shared `StageResult` / `PipelineDecision`
  - config-backed rules
  - review queue, review details, persistence
  - modular state stores for `Behavior`, `Trend`, `Funnel`
- v1 is still ahead in tuning depth:
  - broader signal coverage
  - more mature false-positive guards
  - richer review context and older training-driven heuristics

## Real Data Source

The real chat examples for parity and tuning come from the v1 training data:

- `origin/main:scripts/scam-screener-training-data.csv`
- `origin/main:funnel.csv`
- `origin/main:src/main/java/eu/tango/scamscreener/ai/TrainingDataService.java`

Important notes from v1:

- the training CSV already contains normalized `message` values and labels
- it also stores weak feature columns such as:
  - `rule_hits`
  - `similarity_hits`
  - `behavior_hits`
  - `trend_hits`
  - `funnel_hits`
  - `pushes_external_platform`
  - `demands_upfront_payment`
  - `requests_sensitive_data`
  - `claims_middleman_without_proof`
  - `too_good_to_be_true`
  - `repeated_contact_attempts`
- `TrainingDataService` also filtered system lines and one-token noise before writing samples

This means the v1 data is good enough to:

- build golden tests
- tune stage weights
- spot false positives
- compare v2 stage outputs against known labeled examples

## Stage Parity

### Mute

v2 status:

- only bypasses `SYSTEM` messages
- does not mute or suppress player messages

v1 gap:

- v1 early filtering was still small, but its surrounding pipeline had more implicit skip behavior
- v2 still lacks any optional local dedupe or non-risk bypass rules

Missing for parity:

- optional dedupe/cooldown ignore logic
- optional ignore rules for clearly harmless noise patterns
- explicit config for mute behavior if we want it adjustable

### Player List

v2 status:

- functionally stronger than v1
- whitelist + blacklist handled in one early stage
- explicit `WHITELIST` and `BLACKLIST` decisions

v1 gap:

- no meaningful gap here in core behavior

Remaining polish:

- none required for parity
- only optional UX/API expansion

### Rule

v2 status:

- has first-pass checks for:
  - suspicious links
  - external platform push
  - upfront payment
  - account data
  - too-good wording
  - urgency under suspicious context
  - trust under suspicious context
  - discord handle + platform combination

v1 gap:

- v1 `RuleSignalStage` had broader category coverage and more nuanced combinations
- v2 still uses a reduced rule set
- v2 still uses only one free-text `reason` string, not structured reason codes

Missing for parity:

- more complete v1 phrase and pattern coverage
- more compound rules, especially:
  - link + redirect
  - trust + payment
  - urgency + sensitive request
  - stronger middleman/fake-proof claims
- more explicit false-positive guards for benign trading/chat language
- structured reason codes instead of only human-readable reasons

### Levenshtein

v2 status:

- small config-backed phrase list
- token-window comparison already in place
- conservative fuzzy scoring only

v1 gap:

- v1 had a broader similarity corpus and more mature tuning
- v2 still has a small phrase inventory
- normalization is still basic

Missing for parity:

- more phrases from v1 categories
- better obfuscation normalization:
  - `disc0rd`
  - `fr33`
  - `g1ve`
  - common symbol substitutions
- category-specific thresholds or tighter grouping
- stronger guards against normal chat matching short noisy phrases

### Behavior

v2 status:

- detects:
  - repeated same message from same sender
  - burst contact in short window
  - small combo bonus when both happen

v1 gap:

- v1 behavior analysis was richer than simple repeat/burst
- v2 currently has only the first layer of sender-local heuristics

Missing for parity:

- more sender pattern signals
- better repeat escalation over time
- stronger link between repeated contact and suspicious content type
- optional weighting by recent history shape, not only counts

### Trend

v2 status:

- detects exact normalized cross-sender repeats
- scores:
  - single cross-sender repeat
  - multi-sender wave
  - small escalation for wider waves
- ignores very short messages

v1 gap:

- v1 trend analysis was broader than exact same normalized text
- v2 is still exact-message oriented

Missing for parity:

- template/fingerprint grouping for near-identical spam waves
- stronger spread logic by sender count and time density
- optional tie-in with suspicious wording so harmless repeated chat is weighted less
- broader global trend metrics than just same-message matches

### Funnel

v2 status:

- tracks sender-local step sequences
- current steps:
  - `MESSAGE`
  - `TRUST`
  - `EXTERNAL_PLATFORM`
  - `PAYMENT`
  - `ACCOUNT_DATA`
- scores:
  - contact -> external
  - trust -> external
  - external -> payment
  - trust -> payment
  - external -> account
  - trust -> account
  - full chain bonus

v1 gap:

- v1 funnel handling was still more context-aware at the feature level
- v2 sequence model is good, but still intentionally simple

Missing for parity:

- more nuanced step tagging
- stronger partial-chain weighting
- decaying weight by step age
- more sequence variants from real scam flows in v1 data
- direct use of labeled `funnel.csv` examples as golden funnel tests

### Model

v2 status:

- still stub by design

v1 gap:

- all of it

Missing for parity:

- intentionally deferred

## Review / Data Gaps

v2 status:

- review queue is strong
- detail screen exists
- reasons and stage trace are persisted

v1 gap:

- v1 review tooling was tightly tied to older alert IDs, rule weights and alert contexts
- v2 still does not surface all structured per-rule weighting information

Missing for parity:

- optional structured reason codes per stage
- optional per-stage weight details, not just final reason text
- direct training export workflow from review actions if we want to recreate v1 labeling speed

## Config Gaps

v2 status:

- scores for `Rule`, `Levenshtein`, `Behavior`, `Trend`, `Funnel` are configurable
- runtime, message and review settings exist

v1 gap:

- not all rule families and thresholds are externally tunable yet

Missing for parity:

- more detailed rule-category toggles
- phrase-list management workflow
- possibly stage-level enable/disable flags beyond `Model`

## Most Important Next Work

### Highest Priority

- expand `RuleStage` to full v1 category coverage
- expand `LevenshteinStage` phrase inventory and normalization
- derive golden test cases from v1 training CSV rows

### Medium Priority

- improve `BehaviorStage` with more sender-local heuristics
- improve `TrendStage` with fingerprint/template matching
- improve `FunnelStage` using `funnel.csv` examples

### Lower Priority

- bring back richer structured per-rule detail in review
- add tuning workflow around captured false positives / false negatives

## Recommended Tuning Workflow

1. Extract labeled examples from v1 CSV into small golden test sets.
2. Start with `RuleStage`, because it has the largest parity impact.
3. Move to `LevenshteinStage`, because it catches disguised variants of the same wording.
4. Then tune `Behavior`, `Trend`, and `Funnel` using real repeated/sequenced chat examples.
5. Only after those stages are stable, decide whether any hard `BLOCK` rules should be added for very clear cases.

