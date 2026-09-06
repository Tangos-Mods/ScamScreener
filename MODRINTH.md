## ScamScreener 2.6.1

This release brings back Minecraft `26.1.x` support next to `26.2.x` and trims a few chat false positives.

- added Minecraft `26.1.x` builds again through a multi-version build setup
- lowered the default too-good-to-be-true score so a lone `guaranteed` or `dupe` mention no longer opens a review alert by itself
- stopped treating casual standalone `vc` mentions as external-platform pushes (existing rules files are migrated automatically)
- removed the unused legacy trend-escalation mapping and settings
- reposted Skyblocker helper lines (score, crypt, and livid callouts) stay out of the trend wave; the recent reports came from clients older than 2.5, so please update
- updated build dependencies (Fabric API, Mod Menu, Gradle, Stonecutter)
