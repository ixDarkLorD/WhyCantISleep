package net.ixdarklord.whycantisleep.client.highlight;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.client.Blockers;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Footprints where the blocking monsters walked: while they are pointed out, each step they take is marked, a little
 * to one side and then the other, in their highlight color; once the highlight is gone the prints stay a while, faint
 * specks on the ground fading out, so the player can still tell where they went.
 */
public final class BlockerTrails {
    // A print every STRIDE blocks walked, this far either side of the path; prints last LIFE ticks, and are shown
    // (a speck of dust on each) every SHOW_TICKS. No monster leaves more than MOST prints.
    private static final double STRIDE = 0.8;
    private static final double SIDE = 0.15;
    private static final int LIFE = 20 * 15;
    private static final int SHOW_TICKS = 6;
    private static final int MOST = 32;
    private static final Int2ObjectMap<Walker> WALKERS = new Int2ObjectOpenHashMap<>();
    private static final List<Print> PRINTS = new ArrayList<>();
    private static long tick;

    private BlockerTrails() {
    }

    public static void init() {
        ClientTickEvents.END.register(BlockerTrails::tick);
        ClientPlayerEvents.LEAVE.register(player -> {
            WALKERS.clear();
            PRINTS.clear();
        });
    }

    private static void tick(Minecraft minecraft) {
        tick++;
        PRINTS.removeIf(print -> tick - print.at > LIFE);
        if (minecraft.level == null || minecraft.isPaused() || !WCISConfigs.isEnabled(WCISClientConfig.BLOCKER_TRAILS)) return;
        // Their steps, while they are pointed out.
        List<Entity> blockers = Blockers.entities();
        if (blockers.isEmpty()) WALKERS.clear();
        for (Entity blocker : blockers) {
            Walker walker = WALKERS.computeIfAbsent(blocker.getId(), id -> new Walker(blocker.position()));
            Vec3 at = blocker.position();
            Vec3 step = at.subtract(walker.last);
            if (step.horizontalDistance() < STRIDE || !blocker.onGround()) continue;
            walker.last = at;
            walker.left = !walker.left;
            // To the side of the way it walked.
            Vec3 across = new Vec3(-step.z, 0.0, step.x).normalize().scale(walker.left ? SIDE : -SIDE);
            PRINTS.add(new Print(blocker.getId(), at.add(across), Highlights.color(blocker), tick));
            if (++walker.prints > MOST) removeOldest(blocker.getId());
        }
        if (tick % SHOW_TICKS != 0) return;
        RandomSource random = minecraft.level.getRandom();
        for (Print print : PRINTS) {
            // Fading: older prints are shown less and less often.
            float left = 1.0F - (tick - print.at) / (float) LIFE;
            if (random.nextFloat() > Mth.clamp(left * 1.4F, 0.0F, 1.0F)) continue;
            minecraft.level.addParticle(new DustParticleOptions(Vec3.fromRGB24(print.color).toVector3f(), 0.5F + 0.3F * left), print.pos.x, print.pos.y + 0.05, print.pos.z, 0.0, 0.0, 0.0);
        }
    }

    // A monster that has left too many prints: its oldest goes.
    private static void removeOldest(int owner) {
        for (int i = 0; i < PRINTS.size(); i++) {
            if (PRINTS.get(i).owner == owner) {
                PRINTS.remove(i);
                return;
            }
        }
    }

    private record Print(int owner, Vec3 pos, int color, long at) {
    }

    private static final class Walker {
        private Vec3 last;
        private boolean left;
        private int prints;

        private Walker(Vec3 last) {
            this.last = last;
        }
    }
}
