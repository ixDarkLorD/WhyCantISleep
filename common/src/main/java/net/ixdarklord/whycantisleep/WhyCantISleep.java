package net.ixdarklord.whycantisleep;

import net.ixdarklord.coolcatcore.api.core.ModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.deep.Dream;
import net.ixdarklord.whycantisleep.deep.LucidDeep;
import net.ixdarklord.whycantisleep.deep.WCISItems;
import net.ixdarklord.whycantisleep.mind.Mind;
import net.ixdarklord.whycantisleep.mind.MindServer;
import net.ixdarklord.whycantisleep.lurker.LurkerServer;
import net.ixdarklord.whycantisleep.lurker.WCISEntities;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.ixdarklord.whycantisleep.sleep.SleepBoardServer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Objects;

/**
 * Common mod constructor (CoolCatLib). Fabric lists it under the {@code coolcatlib:common} entrypoint, and
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
        WCISEntities.register();
        WCISItems.register();
        // Everything kept in the mod's attachments is declared before they are registered.
        Objects.requireNonNull(Dream.DREAM);
        Objects.requireNonNull(LucidDeep.ORIGIN);
        Mind.ATTACHMENTS.register();
        WCISNetwork.register();
        MindServer.init();
        LurkerServer.init();
        SleepBoardServer.init();
        LucidDeep.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path.toLowerCase(Locale.ROOT));
    }
}
