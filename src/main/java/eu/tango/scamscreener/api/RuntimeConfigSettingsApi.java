package eu.tango.scamscreener.api;

import eu.tango.scamscreener.config.data.AlertRiskLevel;
import eu.tango.scamscreener.config.data.RuntimeConfig;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Runtime-backed implementation of the stable public settings API.
 */
final class RuntimeConfigSettingsApi implements ScamScreenerSettingsApi {
    private final Supplier<RuntimeConfig> configSupplier;
    private final Runnable saveAction;

    RuntimeConfigSettingsApi(Supplier<RuntimeConfig> configSupplier, Runnable saveAction) {
        this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier");
        this.saveAction = Objects.requireNonNull(saveAction, "saveAction");
    }

    @Override
    public boolean pingOnRiskWarning() {
        return config().output().isPingOnRiskWarning();
    }

    @Override
    public void setPingOnRiskWarning(boolean enabled) {
        RuntimeConfig.OutputSettings output = config().output();
        if (output.isPingOnRiskWarning() == enabled) {
            return;
        }

        output.setPingOnRiskWarning(enabled);
        saveAction.run();
    }

    @Override
    public boolean pingOnBlacklistWarning() {
        return config().output().isPingOnBlacklistWarning();
    }

    @Override
    public void setPingOnBlacklistWarning(boolean enabled) {
        RuntimeConfig.OutputSettings output = config().output();
        if (output.isPingOnBlacklistWarning() == enabled) {
            return;
        }

        output.setPingOnBlacklistWarning(enabled);
        saveAction.run();
    }

    @Override
    public ScamScreenerAlertLevel alertMinimumRiskLevel() {
        return ScamScreenerAlertLevel.valueOf(config().alerts().minimumRiskLevel().name());
    }

    @Override
    public void setAlertMinimumRiskLevel(ScamScreenerAlertLevel level) {
        Objects.requireNonNull(level, "level");

        RuntimeConfig.AlertSettings alerts = config().alerts();
        AlertRiskLevel mappedLevel = AlertRiskLevel.valueOf(level.name());
        if (alerts.getMinimumRiskLevel() == mappedLevel) {
            return;
        }

        alerts.setMinimumRiskLevel(mappedLevel);
        saveAction.run();
    }

    private RuntimeConfig config() {
        return Objects.requireNonNull(configSupplier.get(), "runtimeConfig");
    }
}
