package net.ixdarklord.whycantisleep.neoforge;

import net.ixdarklord.coolcatcore.api.core.neoforge.NeoForgeModEntrypoint;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.WhyCantISleepClient;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** NeoForge entrypoint: hands the common and client mod constructors to CoolCatLib. */
@Mod(WhyCantISleep.MOD_ID)
public final class WhyCantISleepNeoForge extends NeoForgeModEntrypoint {
    public WhyCantISleepNeoForge(ModContainer container) {
        super(container);
        this.common(WhyCantISleep::new);
        this.client(() -> WhyCantISleepClient::new);
    }
}
