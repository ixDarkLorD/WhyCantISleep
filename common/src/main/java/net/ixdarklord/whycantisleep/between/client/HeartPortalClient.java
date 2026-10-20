package net.ixdarklord.whycantisleep.between.client;

import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffect;
import net.ixdarklord.coolcatcanvas.api.client.effect.ScreenEffects;
import net.ixdarklord.coolcatcanvas.api.client.vfx.Curve;
import net.ixdarklord.coolcatcanvas.api.client.vfx.ParticleSpec;
import net.ixdarklord.coolcatcanvas.api.client.vfx.Rift;
import net.ixdarklord.coolcatcanvas.api.client.vfx.RiftSpec;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldParticles;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldRifts;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.between.HeartPortalBlock;
import net.ixdarklord.whycantisleep.between.HeartPortalBlockEntity;
import net.ixdarklord.whycantisleep.lurker.LurkerPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * The heart portal, awake, on the client:
 * <ul>
 *     <li>its rift: a blood-red tear hung in the opening in the chamber's back wall, set in the wall like a door, which
 *     opens with a flash when the portal wakes and stays, its edge crawling, while it is awake;</li>
 *     <li>embers and sparks streaming out of it, out the way it faces, into the chamber;</li>
 *     <li>its pull on whoever comes near ({@code whycantisleep:heart_pull}, {@code shaders/post/heart_pull.fsh}): the
 *     view drawn in and turning, colours split, a crimson veined vignette pulsing with a heartbeat, growing as they
 *     come closer;</li>
 *     <li>and as it throws them out: a red flash, then the pull at its height, easing away where they land.</li>
 * </ul>
 */
public final class HeartPortalClient {
    private static final RiftSpec SPEC = RiftSpec.builder().size(2.1F, 2.3F).color(0xE0203A).timing(40, 14)
            .sounds(SoundEvents.PORTAL_TRIGGER, SoundEvents.PORTAL_AMBIENT, SoundEvents.PORTAL_TRAVEL).ambientEvery(90).volume(0.35F)
            .shake(1.5F).build();
    private static final ParticleSpec EMBER = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.GLOW))
            .lifetime(30, 50)
            .size(Curve.of(0.05F, 0.16F, 0.0F))
            .opacity(Curve.of(0.0F, 0.9F, 0.0F))
            .color(LurkerPalette.RED, LurkerPalette.CRIMSON)
            .drag(0.97F)
            .build();
    private static final ParticleSpec SPARK = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.STREAK))
            .shape(ParticleSpec.Shape.SPARK)
            .lifetime(12, 20)
            .size(Curve.of(0.05F, 0.07F, 0.0F))
            .opacity(Curve.of(1.0F, 0.8F, 0.0F))
            .color(0xFFD6D2, LurkerPalette.RED)
            .stretch(5.0F)
            .drag(0.95F)
            .build();
    /** The pull starts this far from the rift (blocks) and is full this near. */
    private static final double PULL_FROM = 3.0;
    private static final double PULL_FULL = 1.0;
    private static final int THROW_TICKS = 40;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<BlockPos, Rift> RIFTS = new HashMap<>();

    private static ScreenEffect flash;
    private static float pull;
    private static float pullO;
    private static int thrown = -1;
    private static Vec3 lastPos;

    private HeartPortalClient() {
    }

    public static void init() {
        HeartPortalBlockEntity.clientTick = HeartPortalClient::tick;
        HeartPortalBlockEntity.clientRemoved = be -> close(be.getBlockPos());
        ScreenEffects.register(WhyCantISleep.id("heart_pull"), WhyCantISleep.id("heart_pull"))
                .selectable(false)
                .fade(0, 0, Easing.LINEAR)
                .activeWhen(context -> strength() > 0.01F)
                .strength(context -> strength());
        flash = ScreenEffects.tint(WhyCantISleep.id("heart_throw"), 0xD0FF3048)
                .selectable(false)
                .fade(1, 26, Easing.SINE_OUT);
        ClientTickEvents.END.register(HeartPortalClient::playerTick);
        ClientPlayerEvents.LEAVE.register(player -> {
            RIFTS.clear();
            pull = pullO = 0.0F;
            thrown = -1;
            lastPos = null;
        });
    }

    // Each tick of an awake portal's middle block: keep its rift, and pour embers out of it.
    private static void tick(HeartPortalBlockEntity portal) {
        BlockState state = portal.getBlockState();
        boolean awake = state.getBlock() instanceof HeartPortalBlock && state.getValue(HeartPortalBlock.ACTIVE);
        BlockPos pos = portal.getBlockPos().immutable();
        if (!awake) {
            close(pos);
            return;
        }
        Direction out = state.getValue(HeartPortalBlock.FACING);
        Vec3 normal = new Vec3(out.getStepX(), 0.0, out.getStepZ());
        Rift rift = RIFTS.get(pos);
        if (rift == null) {
            // Set in the wall like a door: it looks out toward the chamber, and doesn't turn after the camera.
            rift = WorldRifts.open(SPEC, Vec3.atCenterOf(pos)).facing(normal);
            RIFTS.put(pos, rift);
        }
        float open = Mth.clamp(rift.openness(0.0F), 0.0F, 1.0F);
        int embers = Math.round(open * (1.5F + RANDOM.nextFloat() * 2.0F));
        for (int i = 0; i < embers; i++) pour(EMBER, pos, normal, 0.05 + RANDOM.nextDouble() * 0.06);
        if (RANDOM.nextFloat() < open * 0.35F) pour(SPARK, pos, normal, 0.16 + RANDOM.nextDouble() * 0.1);
    }

    // One ember from a random point on the rift's face, out along the way it faces, spreading and rising a little.
    private static void pour(ParticleSpec spec, BlockPos core, Vec3 normal, double speed) {
        Vec3 side = new Vec3(-normal.z, 0.0, normal.x);
        double across = (RANDOM.nextDouble() * 2.0 - 1.0) * SPEC.width() * 0.45;
        double up = (RANDOM.nextDouble() * 2.0 - 1.0) * SPEC.height() * 0.45;
        Vec3 from = Vec3.atCenterOf(core).add(side.scale(across)).add(0.0, up, 0.0).add(normal.scale(0.15));
        Vec3 velocity = normal.scale(speed)
                .add(side.scale((RANDOM.nextDouble() - 0.5) * 0.04))
                .add(0.0, (RANDOM.nextDouble() - 0.3) * 0.025, 0.0);
        WorldParticles.spawn(spec, from, velocity);
    }

    // The player's side: how near the nearest awake rift they are, and whether it just threw them out.
    private static void playerTick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.isPaused()) return;
        pullO = pull;
        double nearest = Double.MAX_VALUE;
        Vec3 eye = player.getEyePosition();
        for (Map.Entry<BlockPos, Rift> entry : RIFTS.entrySet()) {
            if (entry.getValue().phase() == Rift.Phase.GONE) continue;
            nearest = Math.min(nearest, eye.distanceTo(Vec3.atCenterOf(entry.getKey())));
        }
        float target = nearest == Double.MAX_VALUE ? 0.0F
                : (float) Mth.clamp((PULL_FROM - nearest) / (PULL_FROM - PULL_FULL), 0.0, 1.0);
        target = target * target * (3.0F - 2.0F * target);
        pull += (target - pull) * 0.15F;

        // Thrown out: right at the rift one tick, far off the next.
        Vec3 pos = player.position();
        if (lastPos != null && pullO > 0.25F && pos.distanceToSqr(lastPos) > 16.0 * 16.0) {
            thrown = 0;
            pull = pullO = 0.0F;
            flash.enableFor(2);
        }
        lastPos = pos;
        if (thrown >= 0 && ++thrown > THROW_TICKS) thrown = -1;
    }

    private static float strength() {
        float throwing = thrown < 0 ? 0.0F : 1.0F - Easing.SINE_IN_OUT.apply(Mth.clamp(thrown / (float) THROW_TICKS, 0.0F, 1.0F));
        return Math.max(pull * 0.85F, throwing);
    }

    private static void close(BlockPos pos) {
        Rift rift = RIFTS.remove(pos);
        if (rift != null) rift.close();
    }
}
