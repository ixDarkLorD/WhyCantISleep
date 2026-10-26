package net.ixdarklord.whycantisleep;

import net.ixdarklord.coolcatcore.api.client.registry.ItemRendererRegistry;
import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;
import net.ixdarklord.folios.internal.client.FoliosBookItemModel;
import net.ixdarklord.whycantisleep.client.Eyelids;
import net.ixdarklord.whycantisleep.client.HidingAcoustics;
import net.ixdarklord.whycantisleep.client.HideCooldown;
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
import net.ixdarklord.whycantisleep.compat.WCISItems;

/**
 * Client mod constructor (CoolCatLib): highlights, HUD layers and other client-only setup.
 * Fabric lists it under {@code coolcatlib:client}; Forge's {@code WhyCantISleepForge} passes it to {@code client(...)}.
 */
public final class WhyCantISleepClient implements ClientModConstructor {
    @Override
    public void onConstructMod() {
        // First of the HUD layers, so the mod's own fade with the game's while the heartbeat minigame runs.
        HudFade.init();
        HidingClient.init();
        HideCooldown.init();
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
        // The guide book is drawn as Folios draws its own (a flat cover in slots, a bound book elsewhere): on 1.20.1 its
        // item model is builtin/entity, and Folios has no API for a mod's own book item, so its own renderer is used.
        ItemRendererRegistry.register(WCISItems.GUIDE_BOOK, FoliosBookItemModel::render);
        // Last, so the minigame draws over everything else on the HUD.
        HidingMinigame.init();
    }
}
