package net.ixdarklord.whycantisleep;

import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;
import net.ixdarklord.whycantisleep.client.Eyelids;
import net.ixdarklord.whycantisleep.client.HidingAcoustics;
import net.ixdarklord.whycantisleep.client.HidingClient;
import net.ixdarklord.whycantisleep.client.SleepEyes;
import net.ixdarklord.whycantisleep.client.HidingMinigame;
import net.ixdarklord.whycantisleep.client.NightTerrorClient;
import net.ixdarklord.whycantisleep.client.GuideBookMusic;
import net.ixdarklord.whycantisleep.client.SleepDebtMoon;
import net.ixdarklord.whycantisleep.client.SleepStatus;
import net.ixdarklord.whycantisleep.client.SleepClient;
import net.ixdarklord.whycantisleep.client.Blockers;
import net.ixdarklord.whycantisleep.client.highlight.BlockerTrails;
import net.ixdarklord.whycantisleep.client.board.SleepBoard;
import net.ixdarklord.whycantisleep.client.fx.SleepEffects;
import net.ixdarklord.whycantisleep.client.hud.BlockerArrows;
import net.ixdarklord.whycantisleep.client.hud.HudFade;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.client.hud.SleepTooltip;

/**
 * Client mod constructor (CoolCatLib): highlights, HUD layers and other client-only setup.
 * Fabric lists it under {@code coolcatlib:client}; NeoForge's {@code WhyCantISleepNeoForge} passes it to {@code client(...)}.
 */
public final class WhyCantISleepClient implements ClientModConstructor {
    @Override
    public void onConstructMod() {
        // First of the HUD layers, so the mod's own fade with the game's while the heartbeat minigame runs.
        HudFade.init();
        HidingClient.init();
        HidingAcoustics.init();
        Eyelids.init();
        SleepEyes.init();
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
        GuideBookMusic.init();
        // Last, so the minigame draws over everything else on the HUD.
        HidingMinigame.init();
    }
}
