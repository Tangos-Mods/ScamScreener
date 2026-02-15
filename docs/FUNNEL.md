# Funnel Stage (Developer Explanation)

In the pipeline, the Funnel Stage is the part that does not only look at isolated suspicious messages, but at the **flow of an entire conversation per player**. Before it runs, rule-based, similarity, behavior, and trend detection have already produced context, so the stage can reuse existing signals instead of starting from scratch. Afterward, it forwards a structured funnel signal to the AI and scoring stages.

At its core, the stage tries to detect typical scam dialogs as a sequence: first an offer, then a trust or rep trigger, then a redirect to another platform, and finally a concrete instruction. Single words like "rep" or "discord" are often harmless on their own, but as an ordered sequence inside a short time window they become much more meaningful.

To do that, each message is first passed through intent tagging. Based on text patterns, lightweight heuristics, and upstream signals, the stage derives tags such as `SERVICE_OFFER`, `FREE_OFFER`, `REP_REQUEST`, `PLATFORM_REDIRECT`, `INSTRUCTION_INJECTION`, `PAYMENT_UPFRONT`, and `COMMUNITY_ANCHOR`. At the same time, it checks for a negative context signal meant to catch legitimate contexts such as recruiting. If that negative context is active, offer-related tags are intentionally removed so normal community communication does not accidentally start a funnel.

State is stored per anonymized player key in a short rolling history. Each entry contains timestamp, channel, a message snippet, detected tags, and the negative-context flag. This history is strictly bounded by a time window (`windowMillis`), a maximum number of messages (`windowSize`), and a context TTL (`contextTtlMillis`) for inactive players. That keeps detection current, efficient, and resistant to stale data.

On top of that history, the stage searches for ordered step chains. The primary full sequence is `OFFER -> REP -> REDIRECT -> INSTRUCTION`. In addition, it detects partial sequences such as `REP -> REDIRECT`, `REDIRECT -> INSTRUCTION`, `OFFER -> PAYMENT`, and `OFFER -> PAYMENT -> REDIRECT`. If multiple partial chains reinforce each other, they are evaluated together. Order matters, but direct adjacency is not required as long as all steps are still inside the active window.

For scoring, a full sequence receives the configured `fullSequenceWeight`, partial matches receive `partialSequenceWeight`, and combined partial constellations can receive extra uplift. A funnel signal is emitted only when the funnel rule is enabled and the computed evaluation is positive. The emitted signal uses source `FUNNEL`, rule id `FUNNEL_SEQUENCE_PATTERN`, the computed weight, an evidence text describing the sequence, and a small set of related message snippets.

Downstream, this signal increases the total score and improves detection quality for multi-step scam patterns that could otherwise slip through when messages are evaluated in isolation. Funnel context is also useful for AI and training paths because it already provides structured conversational information.

The main tuning levers are the funnel patterns themselves, the window and TTL values, and the two weights for full versus partial sequences. If detection is unexpectedly missing or too aggressive, first verify player-key consistency, time-window behavior, correctly forwarded upstream signals, negative-context matching, and whether redirect/instruction phrasing is actually covered by patterns and heuristics.

A typical full-hit flow looks like this semantically: a player offers a service, asks for rep, redirects into voice/Discord context, and then gives a concrete instruction. A typical partial hit is "offer + upfront payment" even without the full four-step chain.

In practice, the Funnel Stage is best understood as a **conversation-structure engine**: less "one word equals scam," more "multiple connected steps form a clear risk pattern."
