package net.ixdarklord.whycantisleep.client.highlight;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxBlend;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.client.Blockers;
import net.ixdarklord.whycantisleep.client.hud.HudStyle;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Footprints where the blocking monsters walked: while they are pointed out, each step they take leaves a clawed print
 * on the ground, a left foot and then a right a little to either side, turned the way it went, glowing faintly in its
 * highlight color. A print presses in as it is made and fades over its time, so once the highlight is gone the player
 * can still tell where they went, and how long ago. Drawn with CoolCatLib's world effects each frame rather than
 * spawned: one shape a print, in two draw calls for them all.
 */
public final class BlockerTrails {
    // A print every STRIDE blocks walked, this far either side of the path; prints last LIFE ticks. No monster leaves
    // more than MOST prints.
    private static final double STRIDE = 0.8;
    private static final double SIDE = 0.15;
    private static final int LIFE = 20 * 15;
    private static final int MOST = 32;
    // A print's half width, how much larger it starts and how quickly it presses in, its brightness, the share of its
    // time it fades over, and how far away prints are drawn.
    private static final float HALF = 0.2F;
    private static final float PRESS_SCALE = 0.35F;
    private static final float PRESS_TICKS = 5.0F;
    private static final float ALPHA = 0.85F;
    private static final float FADE_SHARE = 0.6F;
    private static final double RANGE = 48.0;

    // Flat on the ground, so no softness (it would fade into the very ground it lies on); crisp, as pixel art.
    private static final VfxMaterial LEFT = new VfxMaterial(WhyCantISleep.id("textures/vfx/footprint_left.png"), VfxBlend.ADDITIVE, 0.0F).withSmoothing(false);
    private static final VfxMaterial RIGHT = new VfxMaterial(WhyCantISleep.id("textures/vfx/footprint_right.png"), VfxBlend.ADDITIVE, 0.0F).withSmoothing(false);

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
        WorldVfx.register(WhyCantISleep.id("blocker_trails"), BlockerTrails::render);
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.isPaused()) return;
        tick++;
        PRINTS.removeIf(print -> tick - print.at > LIFE);
        if (minecraft.level == null || !WCISConfigs.isEnabled(WCISClientConfig.BLOCKER_TRAILS)) return;
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
            // To the side of the way it walked, toes pointing along it.
            Vec3 across = new Vec3(-step.z, 0.0, step.x).normalize().scale(walker.left ? -SIDE : SIDE);
            float yaw = (float) Mth.atan2(-step.x, -step.z);
            PRINTS.add(new Print(blocker.getId(), at.add(across), yaw, walker.left, Highlights.color(blocker), tick));
            if (++walker.prints > MOST) removeOldest(blocker.getId());
        }
    }

    private static void render(VfxFrame frame) {
        if (PRINTS.isEmpty()) return;
        // The game's own clock, which stops with the pause screen.
        float now = tick + (Minecraft.getInstance().isPaused() ? 0.0F : frame.partialTick());
        Vec3 camera = frame.cameraPosition();
        for (Print print : PRINTS) {
            if (camera.distanceToSqr(print.pos) > RANGE * RANGE) continue;
            float age = now - print.at;
            float press = Mth.clamp(age / PRESS_TICKS, 0.0F, 1.0F);
            float fade = Mth.clamp((LIFE - age) / (LIFE * FADE_SHARE), 0.0F, 1.0F);
            float alpha = ALPHA * press * fade * fade;
            if (alpha <= 0.0F) continue;
            float half = HALF * (1.0F + PRESS_SCALE * (1.0F - press) * (1.0F - press));
            frame.draw(print.left ? LEFT : RIGHT).flat(print.pos.x, print.pos.y + 0.015, print.pos.z, half, print.yaw, HudStyle.color(print.color, alpha));
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

    private record Print(int owner, Vec3 pos, float yaw, boolean left, int color, long at) {
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
