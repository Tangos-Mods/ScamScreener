# ScamScreener 1.2.2

This 1.2.2 update improves review flow responsiveness and chat capture reliability, with live review updates and broader in-game chat format support.

## Added

- Added live auto-refresh for `Review Logged Chat` so newly captured lines appear while the screen is open.
- Added centralized ignored-message handling.
- Added support for additional captured chat formats, including `Co-op >`, decorated public player lines
- Added a new rules config option: `capturedChatCacheSize` in `scam-screener-rules.json` (default `1000`) to control how many captured chat lines stay in memory for review.

## Changed

- Moved review data loading (`/ss review`, player review, alert review, CSV review) to managed background execution before opening screens.
- Unified background task handling for training upload, model update check/download, and warning sound scheduling through one async dispatcher.
- Expanded player chat parsing to handle more in-game formatting variants (including decorative symbols before names).
- Improved `Review Logged Chat` row identity stability during live refresh to keep current review selections more consistent.

## Fixed

- Fixed cases where valid player messages (especially in public/team-style formats) were not captured and therefore missing from review.
- Reduced risk of client tick/render blocking during review-related loading by consistently moving heavy preparation and CSV review loading into managed background execution.
