# ScamScreener 1.2.2

This update focuses on smoother in-game review workflows and better async stability, especially for loading review data and running background tasks without stuttering the client tick.

## Added

- Added a new rules config option: `capturedChatCacheSize` in `scam-screener-rules.json` (default `1000`) to control how many captured chat lines stay in memory for review.

## Changed

- Moved review data loading (`/ss review`, player review, alert review, CSV review) to managed background execution before opening screens.
- Unified background task handling for training upload, model update check/download, and warning sound scheduling through one async dispatcher.
- Review list size now follows the configured `capturedChatCacheSize` instead of a hardcoded limit.

