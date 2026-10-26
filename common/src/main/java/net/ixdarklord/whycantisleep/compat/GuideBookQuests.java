package net.ixdarklord.whycantisleep.compat;

import net.ixdarklord.folios.api.FoliosQuests;
import net.ixdarklord.whycantisleep.api.HidingEvents;
import net.ixdarklord.whycantisleep.api.SleepEvents;

/**
 * Flags the guide book's server quests ({@code data/whycantisleep/folios/quests/}) follow, for what Folios can't see
 * by itself: {@code whycantisleep:shook_off}, once something hunting the player gave up and vanished while they hid,
 * and {@code whycantisleep:well_rested}, once they woke well rested. Kept with the player by Folios.
 */
public final class GuideBookQuests {
    public static final String SHOOK_OFF = "whycantisleep:shook_off";
    public static final String WELL_RESTED = "whycantisleep:well_rested";

    private GuideBookQuests() {
    }

    public static void init() {
        HidingEvents.SHAKEN_OFF.register(shaken -> {
            if (!shaken.hunters().isEmpty()) FoliosQuests.setFlag(shaken.player(), SHOOK_OFF, true);
        });
        SleepEvents.WOKE.register(woke -> {
            if (woke.wellRested()) FoliosQuests.setFlag(woke.player(), WELL_RESTED, true);
        });
    }
}
