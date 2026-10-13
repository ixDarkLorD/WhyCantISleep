package net.ixdarklord.whycantisleep.client.highlight;

import net.ixdarklord.whycantisleep.client.Blockers;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.sleep.ThreatType;
import net.minecraft.world.entity.Entity;

/**
 * The glow on the monsters that blocked a sleep. Nothing is changed on the entities: the client mixins ask here
 * whether an entity should be drawn with an outline, and in which color, so only this player sees it.
 */
public final class Highlights {
    private Highlights() {
    }

    public static boolean isHighlighted(Entity entity) {
        return Blockers.contains(entity) && WCISConfigs.isEnabled(WCISClientConfig.HIGHLIGHT_MONSTERS);
    }

    /** The color a blocking monster is pointed out in (its outline and its arrow), as {@code 0xRRGGBB}. */
    public static int color(Entity entity) {
        return WCISClientConfig.THREAT_COLORS.get() ? ThreatType.of(entity).color() : WCISClientConfig.HIGHLIGHT_COLOR.get() & 0xFFFFFF;
    }
}
