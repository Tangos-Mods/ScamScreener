## ScamScreener 2.5.2

This patch fixes false scam-review cases from reposted mod/helper chat lines by classifying them as `SYSTEM_PLAYER` instead of normal player chat.

- reclassified reposted helper callouts from visible player lines as `SYSTEM_PLAYER`, so they keep separate source semantics without entering the normal player-chat scam pipeline
- fixed false case creation for wrapped lines such as `[Skyblocker] The livid color is LIME`, `[Skyblocker] 300 Score Reached!`, and `[Skyblocker] We only have 4 crypts out of 5, we need more!`
