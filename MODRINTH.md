# ScamScreener

- Blacklist entries added by player name now resolve and persist the player's UUID automatically in the background.
- Companion mods querying ScamScreener's runtime-backed API data now receive the current state when calling the API again instead of only the initialization state.
- Returned API lists and entry objects are still snapshots, so later changes require a fresh API query.
