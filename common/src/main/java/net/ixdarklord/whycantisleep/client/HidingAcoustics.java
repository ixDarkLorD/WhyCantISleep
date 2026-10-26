package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilter;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilterProvider;
import net.ixdarklord.coolcatcanvas.api.client.sound.SoundFilters;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.ixdarklord.whycantisleep.hiding.HidingView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sounds heard from a hiding spot come muffled, as the spot lets them through. When the local player hides, the spot
 * is read once (and again every second): the blocks they are in, from their feet to their eyes, and each of their
 * sides that faces out, with how open it is: air lets sound through, a slab or fence the part of the side its shape
 * leaves open, glass a little, a solid block hardly at all, a door or trapdoor as much as its texture is see-through
 * (an oak door's top through its windows, an iron door not at all). Tucked in a container (a chest) only the crack
 * under the raised lid, at the front, is open. A sound is then muffled by how open the sides facing it are, the most
 * by those turned straight to it, and roughened by a little distortion; a sound inside the spot (their own breath and heart) not at all. The muffling eases
 * in and out with the eyelids, over {@link #FADE_TICKS}. Through {@link SoundFilters}, which the sound thread asks: it
 * reads only the immutable {@link Enclosure} swapped in here.
 */
public final class HidingAcoustics {
    private static final int FADE_TICKS = 14;
    private static final int RESCAN_TICKS = 20;
    // How often the sounds already playing are filtered again: each tick while the muffling eases, every REFILTER_TICKS
    // while it holds (what moves is filtered as it moves anyway).
    private static final int REFILTER_TICKS = 10;
    // How much of a side gets through: shut by a solid block, by wool, through glass and the like, and the lid's crack.
    private static final float SOLID = 0.03F;
    private static final float WOOL = 0.005F;
    private static final float SEE_THROUGH = 0.25F;
    private static final float LID_CRACK = 0.3F;
    // How a side counts for a sound: by how straight it faces it, plus this much whatever way it faces (sound gets round).
    private static final float SPREAD = 0.15F;
    // From how open the spot is towards a sound to its filter: the highs left (from all but none) and the loudness.
    private static final float MIN_HIGHS = 0.04F;
    private static final float MIN_GAIN = 0.3F;
    // A little distortion on what comes through, more the more it is shut out: this much at most.
    private static final float DISTORTION = 0.35F;
    // How far round the spot's blocks a sound still counts as inside it.
    private static final double INSIDE = 0.3;
    // How many rays across a side tell how much of it a block's shape covers.
    private static final int RAYS = 4;
    private static final Map<BlockState, Float> TEXTURE_OPENNESS = new HashMap<>();

    private static volatile Enclosure enclosure = Enclosure.NONE;
    private static float strength;
    private static long tick;

    private HidingAcoustics() {
    }

    public static void init() {
        SoundFilters.register(SoundFilterProvider.inWorld(position -> enclosure.filter(position)));
        ClientTickEvents.END.register(HidingAcoustics::tick);
    }

    private static void tick(Minecraft minecraft) {
        // Paused, the muffling holds where it is.
        if (minecraft.isPaused()) return;
        tick++;
        LocalPlayer player = minecraft.player;
        boolean hiding = player != null && minecraft.level != null && HidingClient.isLocalHiding() && WCISClientConfig.MUFFLE_SOUNDS.get();
        float target = hiding ? 1.0F : 0.0F;
        float before = strength;
        strength = Mth.approach(strength, target, 1.0F / FADE_TICKS);
        if (strength <= 0.0F) {
            if (enclosure != Enclosure.NONE) {
                enclosure = Enclosure.NONE;
                SoundFilters.refresh();
            }
            TEXTURE_OPENNESS.clear();
            return;
        }
        Enclosure now = enclosure;
        // Read the spot on hiding, and every second while in it (a door opened, a wall broken); coming out, the last
        // reading fades with them.
        if (hiding && (now == Enclosure.NONE || tick % RESCAN_TICKS == 0)) now = scan(minecraft.level, player);
        enclosure = now.withStrength(eased(strength));
        if (strength != before || tick % REFILTER_TICKS == 0) SoundFilters.refresh();
    }

    // The muffling's ease in and out, as the eyelids slide.
    private static float eased(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    // The spot the player is in: the blocks they are in, and each side of those facing out, with how open it is.
    private static Enclosure scan(ClientLevel level, LocalPlayer player) {
        HidingView view = Hiding.view(player);
        if (view == null) return Enclosure.NONE;
        // The blocks from their feet up to their eyes: the body's box would poke out of a chest's front into the open.
        Set<BlockPos> cells = new HashSet<>();
        BlockPos feet = player.blockPosition();
        for (int y = feet.getY(); y <= BlockPos.containing(player.getEyePosition()).getY(); y++) cells.add(feet.atY(y));
        Direction front = Direction.fromYRot(view.yaw());
        boolean tucked = view.posture() == HidingPosture.TUCKED;
        List<Side> sides = new ArrayList<>();
        AABB inside = null;
        for (BlockPos cell : cells) {
            AABB box = new AABB(cell);
            inside = inside == null ? box : inside.minmax(box);
            BlockState own = level.getBlockState(cell);
            for (Direction direction : Direction.values()) {
                BlockPos next = cell.relative(direction);
                if (cells.contains(next)) continue;
                float open = ownSide(level, cell, own, direction, tucked, front) * openness(level, next, level.getBlockState(next), direction);
                sides.add(new Side(Vec3.atCenterOf(cell).add(direction.getUnitVec3().scale(0.5)),
                        direction.getUnitVec3(), open));
            }
        }
        if (inside == null) return Enclosure.NONE;
        return new Enclosure(inside.inflate(INSIDE), sides.toArray(Side[]::new), 0.0F);
    }

    // How open a side of the block the player is in is: tucked in a container that fills its block, only the lid's crack
    // at the front; else as any block between them and the sound.
    private static float ownSide(ClientLevel level, BlockPos pos, BlockState state, Direction direction, boolean tucked, Direction front) {
        if (tucked && coverage(state.getShape(level, pos), Direction.Axis.Y) > 0.8F) return direction == front ? LID_CRACK : SOLID;
        return openness(level, pos, state, direction);
    }

    // How much sound a block lets through across one of its sides: what its shape leaves open, and some through what it
    // covers, by what it is.
    private static float openness(ClientLevel level, BlockPos pos, BlockState state, Direction direction) {
        if (state.isAir()) return 1.0F;
        float covered = coverage(state.getShape(level, pos), direction.getAxis());
        if (covered <= 0.0F) return 1.0F;
        float through;
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock) through = Math.max(SOLID, textureOpenness(state, direction));
        else if (state.is(BlockTags.WOOL)) through = WOOL;
        else if (!state.canOcclude()) through = SEE_THROUGH;
        else through = SOLID;
        return 1.0F - covered + covered * through;
    }

    // How much of a block's side its shape covers, looking through it along the axis: the share of a grid of rays that
    // meet it.
    private static float coverage(VoxelShape shape, Direction.Axis axis) {
        if (shape.isEmpty()) return 0.0F;
        List<AABB> boxes = shape.toAabbs();
        int hit = 0;
        for (int i = 0; i < RAYS; i++) {
            for (int j = 0; j < RAYS; j++) {
                double a = (i + 0.5) / RAYS;
                double b = (j + 0.5) / RAYS;
                for (AABB box : boxes) {
                    boolean meets = switch (axis) {
                        case X -> box.minY <= a && a <= box.maxY && box.minZ <= b && b <= box.maxZ;
                        case Y -> box.minX <= a && a <= box.maxX && box.minZ <= b && b <= box.maxZ;
                        case Z -> box.minX <= a && a <= box.maxX && box.minY <= b && b <= box.maxY;
                    };
                    if (meets) {
                        hit++;
                        break;
                    }
                }
            }
        }
        return hit / (float) (RAYS * RAYS);
    }

    // How see-through a door's (or trapdoor's) face across that side is: the share of clear pixels in the textures of
    // its model's faces that way. Remembered per block state while hiding.
    private static float textureOpenness(BlockState state, Direction direction) {
        return TEXTURE_OPENNESS.computeIfAbsent(state, key -> {
            BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(key);
            List<BlockStateModelPart> parts = new ArrayList<>();
            model.collectParts(RandomSource.create(42L), parts);
            int clear = 0;
            int total = 0;
            for (BlockStateModelPart part : parts) {
                List<BakedQuad> quads = new ArrayList<>(part.getQuads(null));
                for (Direction face : Direction.values()) quads.addAll(part.getQuads(face));
                for (BakedQuad quad : quads) {
                    if (quad.direction().getAxis() != direction.getAxis()) continue;
                    SpriteContents sprite = quad.materialInfo().sprite().contents();
                    for (int x = 0; x < sprite.width(); x++) {
                        for (int y = 0; y < sprite.height(); y++) {
                            if (sprite.isTransparent(0, x, y)) clear++;
                            total++;
                        }
                    }
                }
            }
            return total == 0 ? 0.0F : clear / (float) total;
        });
    }

    // A side of the spot facing out: its middle, which way it faces, and how open it is (0 to 1).
    private record Side(Vec3 center, Vec3 normal, float open) {
    }

    /** The spot as the sound thread reads it: inside it, its sides facing out, and how strongly it muffles now. */
    private record Enclosure(AABB inside, Side[] sides, float strength) {
        static final Enclosure NONE = new Enclosure(new AABB(0, 0, 0, 0, 0, 0), new Side[0], 0.0F);

        Enclosure withStrength(float strength) {
            return this == NONE ? NONE : new Enclosure(this.inside, this.sides, strength);
        }

        SoundFilter filter(Vec3 position) {
            if (this.strength <= 0.0F || this.sides.length == 0 || this.inside.contains(position)) return SoundFilter.NONE;
            // How open the spot is towards the sound: each side's openness, weighed by how straight it faces it.
            float weights = 0.0F;
            float open = 0.0F;
            for (Side side : this.sides) {
                Vec3 to = position.subtract(side.center());
                double length = to.length();
                float facing = length < 1.0E-4 ? 1.0F : (float) Math.max(0.0, side.normal().dot(to) / length);
                float weight = facing + SPREAD;
                weights += weight;
                open += weight * side.open();
            }
            float through = open / weights;
            float highs = Mth.lerp(this.strength, 1.0F, MIN_HIGHS + (1.0F - MIN_HIGHS) * through);
            float gain = Mth.lerp(this.strength, 1.0F, MIN_GAIN + (1.0F - MIN_GAIN) * Mth.sqrt(through));
            return new SoundFilter(gain, highs, this.strength * DISTORTION * (1.0F - 0.5F * through));
        }
    }
}
