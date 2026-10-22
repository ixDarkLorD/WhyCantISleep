package net.ixdarklord.whycantisleep;

import net.ixdarklord.coolcatcore.api.core.ModConstructor;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.whycantisleep.compat.GuideBookGift;
import net.ixdarklord.whycantisleep.compat.WCISItems;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.hiding.HidingServer;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.ixdarklord.whycantisleep.sleep.SleepBoardServer;
import net.ixdarklord.whycantisleep.sleep.RoomClearing;
import net.ixdarklord.whycantisleep.sleep.SleepNights;
import net.ixdarklord.whycantisleep.sleep.SleepVotes;
import net.ixdarklord.whycantisleep.sleep.SleepTroubles;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

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
        WCISItems.register();
        WCISNetwork.register();
        SleepBoardServer.init();
        SleepTroubles.init();
        SleepNights.init();
        RoomClearing.init();
        SleepVotes.init();
        HidingServer.init();
        GuideBookGift.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path.toLowerCase(Locale.ROOT));
    }
}
