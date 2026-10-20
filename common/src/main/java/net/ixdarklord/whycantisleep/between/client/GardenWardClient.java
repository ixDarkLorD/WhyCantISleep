package net.ixdarklord.whycantisleep.between.client;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.ixdarklord.coolcatcanvas.api.client.vfx.Curve;
import net.ixdarklord.coolcatcanvas.api.client.vfx.ParticleSpec;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxBlend;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldParticles;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.lurker.LurkerPalette;
import net.ixdarklord.whycantisleep.network.WardPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.ixdarklord.whycantisleep.between.WCISBlocks;
import net.ixdarklord.whycantisleep.between.HedgeWallBlock;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Garden's ward, as the player feels it (GardenWard): a block it holds, struck, glints and throws the blow back:
 * a ripple of red light spreads from where it was hit, shards of light glance off it, the garden's veins wake under
 * the faces of the blocks round it, a pulse of blood-light running out through them (shaders/vfx/ward_field.fsh) and dies away within a few blocks, it rings, and the
 * view jolts up a touch. Once told a block is held, the client stops cracking it (MultiPlayerGameModeMixin), and each blow on it
 * after rings the same, for a while (the ward may break meanwhile: the Gardener may die).
 */
public final class GardenWardClient {
    private static final long REMEMBER = 60;   // ticks a held block is known to be held, since last struck
    private static final long RING_EVERY = 8;  // ticks between glints, holding the button on one

    private static final ParticleSpec RIPPLE = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.RING).withSoftness(0.05F))
            .lifetime(12)
            .size(Curve.of(0.15F, 1.3F))
            .opacity(Curve.of(1.0F, 0.0F))
            .color(0xFFE2E6, LurkerPalette.RED)
            .build();
    private static final ParticleSpec FLASH = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.STAR).withSoftness(0.05F))
            .lifetime(7)
            .size(Curve.of(0.2F, 0.7F, 0.0F))
            .opacity(Curve.of(1.0F, 0.8F, 0.0F))
            .color(0xFFFFFF, 0xFF8A96)
            .randomRoll()
            .build();
    private static final ParticleSpec GLANCE = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.STREAK).withSoftness(0.05F))
            .shape(ParticleSpec.Shape.SPARK)
            .lifetime(6, 11)
            .size(Curve.of(0.05F, 0.0F))
            .opacity(Curve.of(1.0F, 0.0F))
            .color(0xFFD0D6, LurkerPalette.RED)
            .drag(0.8F)
            .stretch(2.5F)
            .build();
    /** The field of force: drawn by its own shader over the faces round the blow. */
    private static final VfxMaterial FIELD = new VfxMaterial(VfxMaterial.GLOW, VfxBlend.ADDITIVE, 0.0F).withShader(WhyCantISleep.id("vfx/ward_field"));
    private static final int FIELD_REACH = 3;          // blocks round the struck one whose faces it can cover
    private static final double FIELD_RADIUS = 3.5;    // and how far from the blow, at most (the shader's REACH)
    private static final int FIELD_FACES = 200;        // at most, nearest first
    private static final float FIELD_SECONDS = 0.9F;
    private static final int FIELDS = 3;               // at once, at most
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Long2LongOpenHashMap HELD = new Long2LongOpenHashMap();
    private static final List<Field> ACTIVE = new ArrayList<>();
    private static long lastRing;
    private static float fieldClock;  // the world effects' clock, as of the last frame

    private GardenWardClient() {
    }

    public static void init() {
        ClientPlayerEvents.LEAVE.register(player -> {
            HELD.clear();
            ACTIVE.clear();
        });
        WorldVfx.register(WhyCantISleep.id("ward_field"), GardenWardClient::renderFields);
    }

    // A face of a block the field lies over: its corners, and where on its plane the blow is.
    private record Face(Vec3 a, Vec3 b, Vec3 c, Vec3 d, float u0, float v0, float u1, float v1, int perp) {
    }

    private record Field(List<Face> faces, float start) {
    }

    // The faces the field covers, round a blow at `at` on the block at pos: every face of a full block within reach
    // that is open to the air (or to something that isn't a full block), nearest the blow first, so many and no more.
    private static List<Face> faces(ClientLevel level, BlockPos pos, Vec3 at) {
        record Found(BlockPos pos, Direction side, double distance) {
        }
        List<Found> found = new ArrayList<>();
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        for (BlockPos block : BlockPos.betweenClosed(pos.offset(-FIELD_REACH, -FIELD_REACH, -FIELD_REACH), pos.offset(FIELD_REACH, FIELD_REACH, FIELD_REACH))) {
            if (!solid(level, block)) continue;
            for (Direction side : Direction.values()) {
                next.setWithOffset(block, side);
                if (solid(level, next) && !inset(level.getBlockState(block), side)) continue;
                Vec3 middle = Vec3.atCenterOf(block).add(Vec3.atLowerCornerOf(side.getUnitVec3i()).scale(0.5));
                double distance = middle.distanceTo(at);
                if (distance <= FIELD_RADIUS + 0.7) found.add(new Found(block.immutable(), side, distance));
            }
        }
        found.sort(Comparator.comparingDouble(Found::distance));
        List<Face> faces = new ArrayList<>();
        for (Found f : found.subList(0, Math.min(found.size(), FIELD_FACES))) faces.add(face(f.pos(), f.side(), at, inset(level.getBlockState(f.pos()), f.side())));
        return faces;
    }

    // One face, laid a hair over the block's own so it never fights it.
    // What the field lies over: full blocks, and the trimmed hedge's panels.
    private static boolean solid(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isCollisionShapeFullBlock(level, pos) || state.is(WCISBlocks.TRIMMED_HEDGE.get());
    }

    // A trimmed hedge's broad faces lie two pixels in from its block's sides.
    private static boolean inset(BlockState state, Direction side) {
        return state.is(WCISBlocks.TRIMMED_HEDGE.get()) && side.getAxis() != Direction.Axis.Y
                && side.getAxis() != state.getValue(HedgeWallBlock.AXIS);
    }

    private static Face face(BlockPos pos, Direction side, Vec3 at, boolean inset) {
        Direction.Axis axis = side.getAxis();
        double plane = (side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? pos.get(axis) + 1 : pos.get(axis))
                - (inset ? (side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 2.0 / 16.0 : -2.0 / 16.0) : 0.0) + side.getStepX() * 0.004
                + side.getStepY() * 0.004 + side.getStepZ() * 0.004;
        // The face's two directions across: u and v.
        Direction.Axis uAxis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Direction.Axis vAxis = axis == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
        double u0 = pos.get(uAxis), v0 = pos.get(vAxis);
        Vec3 a = point(axis, plane, uAxis, u0, vAxis, v0);
        Vec3 b = point(axis, plane, uAxis, u0, vAxis, v0 + 1);
        Vec3 c = point(axis, plane, uAxis, u0 + 1, vAxis, v0 + 1);
        Vec3 d = point(axis, plane, uAxis, u0 + 1, vAxis, v0);
        float hu = (float) at.get(uAxis), hv = (float) at.get(vAxis);
        int perp = Mth.clamp((int) Math.round(Math.abs(plane - at.get(axis)) * 40.0), 0, 255);
        return new Face(a, b, c, d, (float) u0 - hu, (float) v0 - hv, (float) u0 + 1 - hu, (float) v0 + 1 - hv, perp);
    }

    private static Vec3 point(Direction.Axis axis, double plane, Direction.Axis uAxis, double u, Direction.Axis vAxis, double v) {
        double[] xyz = new double[3];
        xyz[axis.ordinal()] = plane;
        xyz[uAxis.ordinal()] = u;
        xyz[vAxis.ordinal()] = v;
        return new Vec3(xyz[0], xyz[1], xyz[2]);
    }

    private static void renderFields(VfxFrame frame) {
        fieldClock = frame.time();
        if (ACTIVE.isEmpty()) return;
        VfxDraw draw = frame.draw(FIELD);
        ACTIVE.removeIf(field -> frame.time() - field.start() > FIELD_SECONDS || frame.time() < field.start());
        for (Field field : ACTIVE) {
            float life = Mth.clamp((frame.time() - field.start()) / FIELD_SECONDS, 0.0F, 1.0F);
            int g = Math.round(life * 255.0F);
            for (Face face : field.faces()) {
                draw.quad(face.a(), face.b(), face.c(), face.d(), face.u0(), face.v0(), face.u1(), face.v1(), ARGB.color(255, face.perp(), g, 0));
            }
        }
    }

    /** The server: the ward held the block the player struck. */
    public static void on(WardPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        HELD.put(payload.pos().asLong(), level.getGameTime() + REMEMBER);
        ring(payload.pos(), payload.face(), true);
    }

    /**
     * Whether the block at pos is known to be held; if it is, the blow on it rings (now and then, holding the
     * button), and the client doesn't crack it.
     */
    public static boolean held(BlockPos pos, Direction face) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !HELD.containsKey(pos.asLong())) return false;
        if (HELD.get(pos.asLong()) < level.getGameTime()) {
            HELD.remove(pos.asLong());
            return false;
        }
        HELD.put(pos.asLong(), level.getGameTime() + REMEMBER);
        ring(pos, face, false);
        return true;
    }

    private static void ring(BlockPos pos, Direction face, boolean first) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) return;
        long now = level.getGameTime();
        if (!first && now - lastRing < RING_EVERY) return;
        lastRing = now;
        // Where it was struck: where the player looks on the face, else the face's middle.
        Vec3 normal = Vec3.atLowerCornerOf(face.getUnitVec3i());
        Vec3 at = Vec3.atCenterOf(pos).add(normal.scale(0.52));
        HitResult hit = minecraft.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceToSqr(at) < 1.0) {
            at = hit.getLocation().add(normal.scale(0.03));
        }
        // The field of force over the faces round the blow.
        if (ACTIVE.size() >= FIELDS) ACTIVE.removeFirst();
        ACTIVE.add(new Field(faces(level, pos, at), fieldClock));
        // The ripple and the flash in front of the face, toward the eye, so they never sink into the block.
        Vec3 eye = minecraft.gameRenderer.getMainCamera().position();
        Vec3 front = at.add(eye.subtract(at).normalize().scale(0.3));
        WorldParticles.spawn(RIPPLE, front.x, front.y, front.z, 0, 0, 0);
        WorldParticles.spawn(FLASH, front.x, front.y, front.z, 0, 0, 0);
        for (int i = 0; i < 7; i++) {
            // Glancing off: out of the face, and sideways along it.
            Vec3 random = new Vec3(RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.5);
            Vec3 along = random.subtract(normal.scale(random.dot(normal)));
            if (along.lengthSqr() > 1.0E-4) along = along.normalize().scale(0.18);
            Vec3 velocity = normal.scale(0.12 + RANDOM.nextDouble() * 0.1).add(along);
            WorldParticles.spawn(GLANCE, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
        }
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.7F, 0.5F + RANDOM.nextFloat() * 0.1F, false);
        level.playLocalSound(at.x, at.y, at.z, SoundEvents.SHIELD_BLOCK.value(), SoundSource.BLOCKS, 0.5F, 0.6F, false);
        // The blow bounces back: the view jolts up a touch.
        player.setXRot(player.getXRot() - 1.5F);
        player.xRotO = player.getXRot();
    }
}
