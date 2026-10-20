package net.ixdarklord.whycantisleep;

import net.ixdarklord.coolcatcore.api.core.ModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.between.Dream;
import net.ixdarklord.whycantisleep.between.DarkBetween;
import net.ixdarklord.whycantisleep.between.WCISBlocks;
import net.ixdarklord.whycantisleep.between.WCISItems;
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
        WCISBlocks.register();
        // What climbs out of a tomb is whoever lay in it: a Tomb Revenant.
        net.ixdarklord.whycantisleep.between.TombBlock.risen = () -> WCISEntities.TOMB_REVENANT.get();
        WCISItems.register();
        // Everything kept in the mod's attachments is declared before they are registered.
        Objects.requireNonNull(Dream.DREAM);
        Objects.requireNonNull(DarkBetween.ORIGIN);
        Objects.requireNonNull(net.ixdarklord.whycantisleep.between.BetweenProgress.PROGRESS);
        Mind.ATTACHMENTS.register();
        WCISNetwork.register();
        MindServer.init();
        LurkerServer.init();
        SleepBoardServer.init();
        DarkBetween.init();
        net.ixdarklord.whycantisleep.between.GardenWard.init();
        net.ixdarklord.whycantisleep.between.BetweenCreatures.init();
        net.ixdarklord.whycantisleep.between.WildsServer.init();
        net.ixdarklord.whycantisleep.between.DreamGlider.init();
        net.ixdarklord.whycantisleep.between.BetweenProgress.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path.toLowerCase(Locale.ROOT));
    }
}
