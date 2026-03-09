package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.RulesConfig;

/**
 * Versioned migration pipeline for {@code rules.json}.
 *
 * <p>Each migration step should update only values that still match an older
 * shipped default so user overrides survive schema upgrades.
 */
final class RulesConfigMigration {
    static final int CURRENT_VERSION = 1;

    private RulesConfigMigration() {
    }

    static MigrationResult migrate(RulesConfig config) {
        RulesConfig migrated = config == null ? new RulesConfig() : config;
        boolean changed = false;
        int version = migrated.version();

        if (version < 1) {
            changed |= migrateToV1(migrated);
            version = 1;
        }

        if (migrated.getVersion() != version) {
            migrated.setVersion(version);
            changed = true;
        }

        return new MigrationResult(migrated, changed);
    }

    private static boolean migrateToV1(RulesConfig config) {
        boolean changed = false;
        RulesConfig.RuleStageSettings ruleStage = config.ruleStage();
        String externalPlatformPattern = ruleStage.getExternalPlatformPattern();

        if (externalPlatformPattern == null
            || externalPlatformPattern.isBlank()
            || RulesConfig.RuleStageSettings.LEGACY_EXTERNAL_PLATFORM_PATTERN.equals(externalPlatformPattern)) {
            ruleStage.setExternalPlatformPattern(RulesConfig.RuleStageSettings.DEFAULT_EXTERNAL_PLATFORM_PATTERN);
            changed = true;
        }

        return changed;
    }

    record MigrationResult(RulesConfig config, boolean changed) {
    }
}
