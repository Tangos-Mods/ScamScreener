# ScamScreener 1.2.3

This update improves review accuracy and readability: more decorated chat lines are captured, duplicate self-message echoes are removed, and each review row now shows the mod score.

## Added

- Added per-row mod risk score display in the live reviewer, shown directly after `[I] [S] [L]` as `(0)` to `(100)`.

## Changed

- Expanded player chat parsing so heavily decorated lines (for example with level/rank tags and extra symbols between them) are recognized more reliably.

## Fixed

- Fixed duplicate entries in live review for your own messages by de-duplicating outgoing chat against immediate incoming echo lines.
- Fixed missing review captures for some decorated player chat formats that previously failed to parse.

