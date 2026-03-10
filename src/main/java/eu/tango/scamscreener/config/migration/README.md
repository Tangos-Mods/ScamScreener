# Config Migration

All versioned JSON configs use the same migration flow:

1. Add or keep a `version` field on the config and implement `VersionedConfig`.
2. Register the current schema version in `ConfigSchema`.
3. Use `MigratingConfigStore` for the store.
4. Use `SimpleVersionedConfigMigration` for the store migration.

## When you change a config schema

1. Bump the matching value in `ConfigSchema`.
2. Older or unversioned on-disk configs are replaced with the current default config.
3. Saving an in-memory config keeps the supplied values and stamps the current version.
4. Add or update store tests to verify:
   - old JSON is replaced with current defaults
   - `version` is updated
   - current-version JSON is preserved
   - saved values are persisted back to disk

## Current usage

- `runtime.json`, `rules.json`, `whitelist.json`, `blacklist.json`, and `review.json` all use the same strict versioned replacement flow.
- `LegacyV1ConfigMigration` is separate from schema versioning; it imports old v1 artifacts into the v2 file layout once.
