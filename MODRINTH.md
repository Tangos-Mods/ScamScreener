## ScamScreener 2.5.1

This patch tightens repeat-wave handling and fixes a config regression around the local Training Hub client ID.

- tightened `TrendStage` so only repeated scam-like pitches score, instead of generic repeated public chat
- moved reposted dungeon/mod helper callouts into one central pre-pipeline player-message filter and added more known lines
- preserved the installation-local `trainingClientId` when old or unversioned `runtime.json` files are recreated during schema updates
