## ScamScreener 2.4.0

This release replaces the old Training Hub account login inside the mod with a direct anonymous upload flow based on the local `trainingClientId`.

### Changed
- Removed the old in-mod Training Hub username/password login flow.
- Training uploads now go directly through the installation-local `trainingClientId`.
- The Training Hub screen inside Minecraft is now a simpler upload-only UI.

### Added
- SHA-256 upload handshake headers so the Training Hub can verify payload integrity together with the submitted client ID.

### Notes
- No ScamScreener web account is needed inside the mod anymore.
- Client/account linking is now intended to happen later inside the Training Hub website.
- No Microsoft Account Needed. STAY SAFE!
