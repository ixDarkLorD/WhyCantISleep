package net.ixdarklord.whycantisleep.config;

import net.ixdarklord.coolcatcore.api.config.ConfigValue;

/**
 * Loads the configs and answers what is in effect right now: a client option counts only while the server setting
 * gating it (its {@code enabledWhen}) allows it.
 */
public final class WCISConfigs {
    private WCISConfigs() {
    }

    /** Builds both configs, which registers them and loads their files. The server config first: client options depend on it. */
    public static void register() {
        WCISServerConfig.CONFIG.id();
        WCISClientConfig.CONFIG.id();
    }

    /** Whether a client option is on and everything it depends on, up to the server's settings, allows it. */
    public static boolean isEnabled(ConfigValue<Boolean> option) {
        for (ConfigValue<?> value = option; value != null; value = value.dependency().map(dependency -> dependency.source()).orElse(null)) {
            if (!value.isActive()) return false;
        }
        return option.get();
    }
}
