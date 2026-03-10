# ScamScreener 2.0.2

- only validated player messages reach the detection pipeline
- system and invalid pseudo-player messages are skipped instead of producing false alerts
- added `/ss enable` and `/ss disable` with a clickable re-enable notice on join
- added a join-time Modrinth update notification with changelog hover preview
- default alert threshold is now `MEDIUM`
- config schema versioning is centralized and old or unversioned v2 configs are replaced with current defaults
