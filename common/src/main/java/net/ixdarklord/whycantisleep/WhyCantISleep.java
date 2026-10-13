package net.ixdarklord.whycantisleep;

import net.ixdarklord.coolcatcore.api.core.ModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Common mod constructor (CoolCatLib: Core). Fabric lists it under the {@code coolcatcore:common} entrypoint, and
 * NeoForge's {@code WhyCantISleepNeoForge} passes it to {@code common(...)}. The client half is {@link WhyCantISleepClient}.
 */
public final class WhyCantISleep implements ModConstructor {
    public static final String MOD_ID = "whycantisleep";
    public static final String MOD_NAME = "Why Can't I Sleep?";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onConstructMod() {
        LOGGER.info("Loading {} on {}", MOD_NAME, Platform.isFabric() ? "Fabric" : "NeoForge");

        WCISConfigs.register();
        WCISNetwork.register();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path.toLowerCase(Locale.ROOT));
    }
}
