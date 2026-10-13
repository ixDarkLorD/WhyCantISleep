package net.ixdarklord.whycantisleep.client.highlight;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.sleep.ThreatType;
import net.minecraft.world.entity.Entity;

import java.util.Collection;

/**
 * The monsters glowing for this player right now, by network id. Nothing is changed on the entities: the client
 * mixins ask here whether an entity should be drawn with an outline, and in which color, so only this player sees it.
 */
public final class Highlights {
    // Entity id to the client tick its highlight ends at.
    private static final Int2LongMap ENDS = new Int2LongOpenHashMap();
    private static long tick;

    private Highlights() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> {
            tick++;
            if (!ENDS.isEmpty()) ENDS.int2LongEntrySet().removeIf(entry -> entry.getLongValue() <= tick);
        });
        ClientPlayerEvents.LEAVE.register(player -> ENDS.clear());
    }

    /** Highlights the monsters that blocked a sleep, for the time the server's config sets. */
    public static void show(Collection<? extends Entity> blockers) {
        if (!WCISConfigs.isEnabled(WCISClientConfig.HIGHLIGHT_MONSTERS)) return;
        long end = tick + WCISServerConfig.HIGHLIGHT_DURATION.get() * 20L;
        for (Entity blocker : blockers) ENDS.put(blocker.getId(), end);
    }

    public static boolean isHighlighted(Entity entity) {
        return !ENDS.isEmpty() && ENDS.containsKey(entity.getId());
    }

    /** The outline color of a highlighted entity, as {@code 0xRRGGBB}. */
    public static int color(Entity entity) {
        return WCISClientConfig.THREAT_COLORS.get() ? ThreatType.of(entity).color() : WCISClientConfig.HIGHLIGHT_COLOR.get() & 0xFFFFFF;
    }
}
