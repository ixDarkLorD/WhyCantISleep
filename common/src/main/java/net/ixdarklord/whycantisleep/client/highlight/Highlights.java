package net.ixdarklord.whycantisleep.client.highlight;

import net.ixdarklord.whycantisleep.client.Blockers;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.sleep.ThreatType;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * The glow on the monsters that blocked a sleep. Nothing is changed on the entities: the client mixins ask here
 * whether an entity should be drawn with an outline, and in which color, so only this player sees it.
 */
public final class Highlights {
    // The greatest threat's color, this much toward white, give or take its throb.
    private static final float TOP_BRIGHTEN = 0.35F;
    private static final float TOP_THROB = 0.15F;
    private static final float TOP_THROB_SPEED = 0.3F;

    private Highlights() {
    }

    public static boolean isHighlighted(Entity entity) {
        return Blockers.contains(entity) && WCISConfigs.isEnabled(WCISClientConfig.HIGHLIGHT_MONSTERS);
    }

    /**
     * The color a blocking monster is pointed out in (its outline and its arrow), as {@code 0xRRGGBB}. The greatest
     * threat's is brighter, and throbs.
     */
    public static int color(Entity entity) {
        int color = WCISClientConfig.THREAT_COLORS.get() ? ThreatType.of(entity).color() : WCISClientConfig.HIGHLIGHT_COLOR.get() & 0xFFFFFF;
        if (!Blockers.isTop(entity)) return color;
        float bright = TOP_BRIGHTEN + TOP_THROB * Mth.sin(Blockers.age(0.0F) * TOP_THROB_SPEED);
        return ARGB.srgbLerp(bright, color, 0xFFFFFF) & 0xFFFFFF;
    }
}
