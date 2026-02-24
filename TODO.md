Task: Implement performance + memory optimizations in ScamScreener

Repository: Tangos-Mods/ScamScreener
Branch: perf/memory-optimizations

########################################
OBJECTIVES
########################################

Reduce RAM usage, prevent unbounded map growth, remove regex backtracking risk,
and eliminate large file memory spikes.

########################################
1) REMOVE HEURISTIC BODY NAME SCANNING
########################################

File:
src/main/java/eu/tango/scamscreener/util/TextUtil.java

Actions:
- Delete MIXED_NAME_TOKEN_PATTERN constant.
- Remove replaceAll() call using it.
- Do NOT replace with another regex heuristic.
- Keep only:
  - color code stripping
  - @mentions anonymization
  - command target anonymization
  - optional speaker hint replace

Reason:
Eliminates catastrophic backtracking + reduces CPU + avoids StackOverflowError.

########################################
2) ADD REGEX SAFETY CATCHES
########################################

Files:
- TextUtil.java
- MutePatternManager.java

Actions:
Wrap all regex execution:

try {
    matcher.find() / replaceAll()
} catch (StackOverflowError e) {
    log warning
    skip execution
}

Reason:
Prevents client crashes from malicious or pathological regex.

########################################
3) ADD TTL CLEANUP — TREND STORE
########################################

File:
src/main/java/eu/tango/scamscreener/pipeline/core/TrendStore.java

Add:
- lastSeenMillis per player
- TTL constant (e.g. 5–10 minutes)

Cleanup trigger:
- every 64 evaluations OR
- when map size increases

Logic:

historyByPlayer.entrySet().removeIf(
  now - entry.lastSeenMillis > TTL
);

Also:
If a player deque becomes empty → remove key.

########################################
4) ADD TTL CLEANUP — TRAINING DATA SERVICE
########################################

File:
src/main/java/eu/tango/scamscreener/ai/TrainingDataService.java

Maps to protect:
- lastTimestampByPlayer
- repeatedContactByPlayer

Add:
- lastSeen tracking
- periodic cleanup

Example trigger:
every 128 recorded chat lines.

########################################
5) HARD CAP CHAT CACHES
########################################

Ensure max limits exist + enforced:

- recentChat
- pendingReviewChat

If not already configurable:
Add config value:

maxCapturedChatLines = 200–500 default

########################################
6) LIMIT LOCAL AI TOKEN VOCABULARY
########################################

File:
LocalAiScorer / Model config loader

Add:
- maxTokenWeights limit (e.g. 5k tokens)
- prune lowest-weight tokens when exceeded

Reason:
Token weight map is the largest RAM consumer.

########################################
7) STREAM TRAINING CSV — REMOVE RAM SPIKES
########################################

File:
TrainingDataService.java

Replace:

Files.readAllLines()

With:

BufferedReader streaming read.

Add safeguard:

If CSV > X MB → skip auto migration.

########################################
8) OPTIONAL LRU FAILSAFE (GLOBAL)
########################################

For all player maps:

If map.size() > MAX_PLAYERS_TRACKED:
    evict oldest entries

Prevents hub/lobby explosion scenarios.

########################################
9) DO NOT USE System.gc()
########################################

Explicitly avoid forced GC calls.

Memory is freed by:
- removing references
- TTL pruning
- capped collections

########################################
EXPECTED RESULTS
########################################

- No StackOverflow from regex
- Stable heap usage over long sessions
- No unbounded player map growth
- Reduced CPU from regex removal
- No large CSV read spikes
- Predictable memory ceiling

########################################
COMMIT MESSAGE
########################################

"Performance + memory optimization pass:

- Removed MIXED_NAME_TOKEN_PATTERN heuristic scanning
- Added StackOverflowError guards to regex execution
- Implemented TTL cleanup for TrendStore + TrainingDataService
- Added cache size caps
- Limited AI token vocabulary
- Replaced CSV readAllLines with streaming IO

Prevents unbounded RAM growth and regex backtracking crashes."