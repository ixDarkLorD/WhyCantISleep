package net.ixdarklord.whycantisleep.between.client;

import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffects;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.between.DarkBetween;
import net.ixdarklord.whycantisleep.between.WCISBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * The labyrinth's air: down between its hedges it is dim and close, a murky fog the corridors run away into, so the
 * far end of a long way is only a shape and the lanterns on the piers glow through it. It comes on as the player
 * walks in among the hedges and lifts as they leave (the plaza, the walls, the open sky), over a couple of seconds.
 * <p>
 * Being in the maze is told by what is round the player: withered hedge close by on at least two sides, at the
 * height of their head, with the hedge's top above them. The fog is laid over the game's own (FogRendererMixin calls
 * {@link #fog}); the dimness is a dark tint over the world.
 */
public final class LabyrinthAtmosphere {
    /** How far the fog starts and how far it is thick, in blocks, all the way in. */
    private static final float FOG_START = 3.0F;
    private static final float FOG_END = 46.0F;
    /** The fog's colour: the dark of the Dark Between, a little red in it. */
    private static final float RED = 0.055F, GREEN = 0.03F, BLUE = 0.055F;
    /** How far it comes on (or goes) per tick: about two seconds from nothing to all. */
    private static final float CHANGE = 0.025F;
    private static final int LOOK = 6;
    private static float inside;
    private static float insideBefore;
    private static boolean inMaze;

    private LabyrinthAtmosphere() {
    }

    public static void init() {
        ClientTickEvents.END.register(minecraft -> {
            insideBefore = inside;
            LocalPlayer player = minecraft.player;
            ClientLevel level = minecraft.level;
            if (player == null || level == null || !DarkBetween.isBetween(level)) {
                inside = 0.0F;
                inMaze = false;
                return;
            }
            if (player.tickCount % 5 == 0) inMaze = amongHedges(level, player);
            inside = Mth.approach(inside, inMaze ? 1.0F : 0.0F, CHANGE);
        });
        ClientPlayerEvents.LEAVE.register(player -> inside = insideBefore = 0.0F);
        // Dim: a dark veil over the world, as deep as the player is in.
        ScreenEffects.tint(WhyCantISleep.id("labyrinth_dim"), 0x66050208)
                .selectable(false)
                .fade(0, 0, Easing.LINEAR)
                .activeWhen(context -> strength() > 0.01F)
                .strength(context -> strength());
    }

    /** How far into the labyrinth's air the player is, 0 to 1, smoothed between ticks. */
    public static float strength() {
        float t = Mth.lerp(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false), insideBefore, inside);
        return t * t * (3.0F - 2.0F * t);
    }

    /** Lays the labyrinth's fog over the game's own (FogRendererMixin, after the game has worked its fog out). */
    public static void fog(FogData data) {
        float f = strength();
        if (f <= 0.0F) return;
        data.environmentalStart = Mth.lerp(f, data.environmentalStart, Math.min(data.environmentalStart, FOG_START));
        data.environmentalEnd = Mth.lerp(f, data.environmentalEnd, Math.min(data.environmentalEnd, FOG_END));
        data.skyEnd = Mth.lerp(f, data.skyEnd, Math.min(data.skyEnd, FOG_END * 1.6F));
        data.cloudEnd = Mth.lerp(f, data.cloudEnd, Math.min(data.cloudEnd, FOG_END * 1.6F));
        data.color.set(Mth.lerp(f, data.color.x(), RED), Mth.lerp(f, data.color.y(), GREEN), Mth.lerp(f, data.color.z(), BLUE), data.color.w());
    }

    private static boolean hedge(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(WCISBlocks.WITHERED_HEDGE.get()) || state.is(WCISBlocks.TRIMMED_HEDGE.get());
    }

    // Hedge close by on at least two sides at the player's head height, and hedge above their head somewhere near
    // (so not up on top of the maze).
    private static boolean amongHedges(ClientLevel level, LocalPlayer player) {
        BlockPos head = BlockPos.containing(player.getEyePosition());
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int sides = 0;
        boolean under = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            for (int k = 1; k <= LOOK; k++) {
                at.set(head).move(direction, k);
                if (hedge(level.getBlockState(at))) {
                    sides++;
                    at.move(Direction.UP, 2);
                    if (hedge(level.getBlockState(at))) under = true;
                    break;
                }
                if (!level.getBlockState(at).isAir()) break;
            }
        }
        return sides >= 2 && under;
    }
}
