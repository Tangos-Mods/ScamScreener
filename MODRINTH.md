# ScamScreener 2.0.0

This release moves training uploads to a secure relay flow (`Mod -> ScamScreener relay server -> Discord`) and adds in-game upload authentication, while keeping the full whitelist feature set and behavior.

## Added

- Added a new in-game `Upload Auth` screen in settings.
- Added invite-code redeem flow for upload credentials.
- Added upload-auth status display and credential reset action in the GUI.

## Changed

- `/scamscreener upload` now uses the relay server upload path instead of direct Discord webhook upload.
- Upload messaging is now relay-focused (clearer relay auth/upload status and failures).
- Whitelist workflow remains fully available (commands + GUI + early trusted-player bypass in detection).

## Fixed

- Improved upload reliability by centralizing relay validation and signed request handling.
- Improved config fallback handling in non-standard runtime/test contexts.

## Removed

- Removed direct client-side Discord webhook upload path.
- Removed legacy webhook-based upload configuration usage.
