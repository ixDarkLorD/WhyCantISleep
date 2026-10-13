package net.ixdarklord.whycantisleep.client;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The monsters that blocked the player's last sleep, by network id, for as long as they are pointed out (the server's
 * highlight duration). The highlights and the arrows both read this.
 */
public final class Blockers {
    // Entity id to the client tick it stops being pointed out at.
    private static final Int2LongMap ENDS = new Int2LongOpenHashMap();
    private static long tick;

    private Blockers() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> {
            tick++;
            if (!ENDS.isEmpty()) ENDS.int2LongEntrySet().removeIf(entry -> entry.getLongValue() <= tick);
        });
        ClientPlayerEvents.LEAVE.register(player -> ENDS.clear());
    }

    /** Points these monsters out, replacing those of an earlier sleep. */
    public static void set(Collection<? extends Entity> blockers) {
        ENDS.clear();
        long end = tick + WCISServerConfig.HIGHLIGHT_DURATION.get() * 20L;
        for (Entity blocker : blockers) ENDS.put(blocker.getId(), end);
    }

    public static boolean isEmpty() {
        return ENDS.isEmpty();
    }

    public static boolean contains(Entity entity) {
        return !ENDS.isEmpty() && ENDS.containsKey(entity.getId());
    }

    /** The pointed-out monsters still alive in the client's level. */
    public static List<Entity> entities() {
        Minecraft minecraft = Minecraft.getInstance();
        if (ENDS.isEmpty() || minecraft.level == null) return List.of();
        List<Entity> entities = new ArrayList<>(ENDS.size());
        for (int id : ENDS.keySet()) {
            Entity entity = minecraft.level.getEntity(id);
            if (entity != null && entity.isAlive()) entities.add(entity);
        }
        return entities;
    }
}
