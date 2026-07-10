## ScamScreener 2.6.0

This release adds Minecraft `26.2.x` support and updates the in-game GUI wiring to match the new client API.

- added support for Minecraft `26.2.x`
- updated ScamScreener screen opening and profiler HUD handling for the `26.2` client API
- refined account-data detection so harmless mentions score low while direct credential requests remain high-risk
- adjusted cross-sender trend scoring to add one point per other sender, with a fixed `+6` wave score from six participating players
- ignored known Discord security-warning messages for link and external-platform scoring while keeping normal Discord invitations active
