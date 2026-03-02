package eu.tango.scamscreener.config.store;

import eu.tango.scamscreener.config.data.RulesConfig;

/**
 * JSON-backed store for the deterministic and similarity rule config.
 */
public final class RulesConfigStore extends BaseConfig<RulesConfig> {
    /**
     * Creates the rules config store bound to {@code rules.json}.
     */
    public RulesConfigStore() {
        super(ConfigPaths.rulesFile(), RulesConfig.class);
    }

    @Override
    protected RulesConfig createDefault() {
        return new RulesConfig();
    }
}
