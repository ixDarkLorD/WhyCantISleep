package net.ixdarklord.whycantisleep;

import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;
import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.client.NightTerrorClient;
import net.ixdarklord.whycantisleep.client.SleepDebtMoon;
import net.ixdarklord.whycantisleep.client.SleepStatus;
import net.ixdarklord.whycantisleep.client.SleepClient;
import net.ixdarklord.whycantisleep.client.Blockers;
import net.ixdarklord.whycantisleep.client.highlight.BlockerTrails;
import net.ixdarklord.whycantisleep.client.board.SleepBoard;
import net.ixdarklord.whycantisleep.client.fx.SleepEffects;
import net.ixdarklord.whycantisleep.client.hud.BlockerArrows;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.client.hud.SleepTooltip;
import net.ixdarklord.whycantisleep.compat.GuideBookModel;

/**
 * Client mod constructor (CoolCatLib): highlights, HUD layers and other client-only setup.
 * Fabric lists it under {@code coolcatlib:client}; NeoForge's {@code WhyCantISleepNeoForge} passes it to {@code client(...)}.
 */
public final class WhyCantISleepClient implements ClientModConstructor {
    @Override
    public void onConstructMod() {
        HidingClient.init();
        SleepClient.init();
        Blockers.init();
        BlockerTrails.init();
        BlockerArrows.init();
        SleepNotices.init();
        SleepBoard.init();
        SleepEffects.init();
        SleepDebtMoon.init();
        SleepStatus.init();
        SleepTooltip.init();
        NightTerrorClient.init();
        GuideBookModel.register();
    }
}
