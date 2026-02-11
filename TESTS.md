# Tests

This file describes which automated tests currently exist, what they cover, and how they validate functionality.

## Running Tests

- All tests run with: `.\gradlew.bat test`
- Framework: JUnit 5

## Current Test Classes

Base directory: `src/test/java/eu/tango/scamscreener/`

### `ai/ModelUpdateServiceHashTest.java`
- **What is tested:**
  - Hash validation during model updates (`hashMatchesExpected` in `ModelUpdateService`).
  - Regression for LF/CRLF and BOM/no BOM variants.
- **How it is tested:**
  - Calls the private static method via reflection.
  - Compares against locally computed SHA-256 hashes.
  - Positive and negative cases:
    - exact hash matches,
    - semantically identical text with UTF-8 variants matches,
    - changed content does not match,
    - missing inputs (`null`/blank) return `false`.

### `chat/parser/ChatLineParserTest.java`
- **What is tested:**
  - Detection of valid player chat lines.
  - Separation from system and NPC lines.
- **How it is tested:**
  - Direct input/output assertions on `parsePlayerLine` and `isSystemLine`.
  - Positive cases (direct chat, whisper) and negative cases (trade system message, `[NPC]`).

### `pipeline/core/MessageEventParserTest.java`
- **What is tested:**
  - Mapping chat formats to context/channel (`party`, `team`, `pm`, `public`).
  - Filtering of system lines.
- **How it is tested:**
  - Parses representative example lines with fixed timestamps.
  - Asserts `MessageContext`, `channel`, and `null` for system lines.

### `pipeline/core/WarningDeduplicatorTest.java`
- **What is tested:**
  - Deduplication: same player + risk-level combination warns only once.
  - Behavior with invalid inputs.
  - Reset behavior via `reset()`.
- **How it is tested:**
  - Stateful sequence tests:
    - first call returns `true`, second identical call returns `false`,
    - different level for the same player still returns `true`,
    - after `reset()`, it returns `true` again.

### `pipeline/stage/FunnelSignalStageTest.java`
- **What is tested:**
  - Funnel behavior scenarios from the Funnel TODO block:
    - benign service offer only -> no funnel signal,
    - discord mention only -> no funnel signal,
    - rep + redirect + instruction -> high partial funnel signal,
    - full offer + rep + redirect + instruction -> full funnel signal,
    - offer + upfront-payment request -> partial funnel signal.
  - Regression-focused cases:
    - guild recruiting context suppresses offer/free intent tags,
    - legit carry ads without redirect/instruction do not trigger funnel.
  - Stage integration boundary:
    - `FunnelSignalStage` consumes upstream-like `existingSignals` (e.g. `EXTERNAL_PLATFORM_PUSH`) to derive redirect intent.
- **How it is tested:**
  - Uses a test `RuleConfig` with default regex patterns and funnel weights.
  - Feeds timestamped `MessageEvent`s through `FunnelSignalStage` with stateful `FunnelStore`.
  - Asserts emitted signal count, sequence evidence text, and bonus weights for partial/full chains.

### `security/SafetyBypassStoreTest.java`
- **What is tested:**
  - Pattern-based blocking and retrieval of pending entries.
  - One-time allow logic (`allowOnce`/`consumeAllowOnce`).
  - Allow queue limits.
- **How it is tested:**
  - Uses a test store with a simple regex (`secret`) in `@BeforeEach`.
  - Lifecycle assertions:
    - `blockIfMatch` creates ID + pending entry,
    - `takePending` consumes exactly once,
    - `allowOnce` is single-use and scoped by `isCommand`,
    - with 6 entries, the oldest one is dropped (limit 5).

### `ui/MessagesTest.java`
- **What is tested:**
  - Message factory stability for most `Messages` methods.
  - Contract checks for critical message contents (error codes, bypass actions, fallback text).
- **How it is tested:**
  - Reflection-based smoke test:
    - calls all safe `public static` message methods and checks `!= null`.
  - Contract tests:
    - error messages include stable codes (e.g., `MU-CHECK-001`, `TR-SAVE-001`, `MUTE-REGEX-001`),
    - `[BYPASS]` messages include expected run command (`/scamscreener bypass <id>`),
    - download-link message includes expected run command,
    - null inputs fall back to safe defaults like `unknown`, `n/a`, `0`.

### `util/IoErrorMapperTest.java`
- **What is tested:**
  - Error detail mapping for training I/O failures.
- **How it is tested:**
  - Targeted exception types (`NoSuchFileException`, `AccessDeniedException`, generic `IOException`).
  - Exact-message assertions for:
    - `null` error,
    - missing file,
    - access denied,
    - path-only message,
    - blank message (fallback to class name).

### `util/TextUtilTest.java`
- **What is tested:**
  - Text normalization and command normalization.
  - AI anonymization (`@name`, command targets, mixed-name tokens).
  - Stability of `anonymizedSpeakerKey`.
- **How it is tested:**
  - Deterministic string-to-string assertions.
  - Equality checks for case-insensitive key generation and fallback `"speaker-unknown"`.

### `util/UuidUtilTest.java`
- **What is tested:**
  - UUID parsing for valid and invalid inputs.
- **How it is tested:**
  - Positive case using a randomly generated UUID (including whitespace trimming).
  - Negative cases (`null`, empty, invalid) expecting `null`.
