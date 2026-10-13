package net.ixdarklord.whycantisleep;

import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;
import net.ixdarklord.whycantisleep.client.SleepClient;
import net.ixdarklord.whycantisleep.client.Blockers;
import net.ixdarklord.whycantisleep.client.board.SleepBoard;
import net.ixdarklord.whycantisleep.client.hud.BlockerArrows;

/**
 * Client mod constructor (CoolCatLib: Core): highlights, HUD layers and other client-only setup.
 * Fabric lists it under {@code coolcatcore:client}; NeoForge's {@code WhyCantISleepNeoForge} passes it to {@code client(...)}.
 */
public final class WhyCantISleepClient implements ClientModConstructor {
    @Override
    public void onConstructMod() {
        SleepClient.init();
        Blockers.init();
        BlockerArrows.init();
        SleepBoard.init();
    }
}
