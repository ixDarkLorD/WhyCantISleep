package net.ixdarklord.whycantisleep.forge;

import net.ixdarklord.coolcatcore.api.core.forge.ForgeModEntrypoint;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.WhyCantISleepClient;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge entrypoint: hands the common and client mod constructors to CoolCatLib. */
@Mod(WhyCantISleep.MOD_ID)
public final class WhyCantISleepForge extends ForgeModEntrypoint {
    public WhyCantISleepForge(FMLJavaModLoadingContext context) {
        super(context);
        this.common(WhyCantISleep::new);
        this.client(() -> WhyCantISleepClient::new);
    }
}
