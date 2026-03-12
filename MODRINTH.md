# ScamScreener 2.1.0

- added a live in-game profiler HUD with `/ss profiler on` and `/ss profiler off`
- added an optional browser profiler through Tango Web API with `/ss profiler open`
- web profiler now shows live MSPT/TPS, lifetime averages, phase breakdown, recent event log, reset, and `.sspp` export
- profiler no longer records while disabled and can be shared with developers via exported profile files
- moved config writes and training export work onto a background file worker to reduce client-thread stalls
- reduced message-related micro-lag with a lighter chat ingress path and cached normalization/fingerprint work
- senderless mod chat is classified before pipeline entry, and a local-echo reentry crash in warning handling is fixed
