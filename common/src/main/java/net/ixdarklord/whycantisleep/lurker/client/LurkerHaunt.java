package net.ixdarklord.whycantisleep.lurker.client;

import net.ixdarklord.coolcatcore.api.client.registry.EntityRendererRegistry;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.whycantisleep.client.fx.SleepEffects;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.lurker.Dread;
import net.ixdarklord.whycantisleep.lurker.LurkerEntity;
import net.ixdarklord.whycantisleep.lurker.LurkerServer;
import net.ixdarklord.whycantisleep.lurker.WCISEntities;
import net.ixdarklord.whycantisleep.network.ScaryPreferencePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * What a player's own dread shows them, and nobody else: eyes under dark beds and whispers once they are
 * {@linkplain Dread.Stage#WATCHED watched}, and once {@linkplain Dread.Stage#HAUNTED haunted} the creature itself, out
 * in the dark around them or crawling from under the bed they lie down in, gone as soon as it is looked at. That
 * figure is a real {@link LurkerEntity} the server never hears of, added to the client's level only.
 * <p>
 * With the "Scary content" option off none of it happens, and the server is told to leave the player alone.
 */
public final class LurkerHaunt {
    // The hallucination's network id: negative, so it can never be one of the server's.
    private static final int GHOST_ID = -0x57C15;
    private static final int GHOST_TICKS = 160;
    // How long the player must look straight at it before it is gone.
    private static final int SEEN_TICKS = 8;
    private static final double BED_RANGE = 8.0;
    private static final RandomSource RANDOM = RandomSource.create();

    private static @Nullable LurkerEntity ghost;
    private static int ghostAge;
    private static int seenTicks;
    private static int eyesWait = 200;
    private static int ghostWait = 400;

    private LurkerHaunt() {
    }

    public static void init() {
        EntityRendererRegistry.register(WCISEntities.LURKER, LurkerRenderer::new);
        LurkerEntity.clientTick = SleepEffects::shadowTrail;
        ClientTickEvents.END.register(LurkerHaunt::tick);
        ClientPlayerEvents.JOIN.register(player -> sendPreference());
        ClientPlayerEvents.LEAVE.register(player -> ghost = null);
        WCISClientConfig.SCARY_CONTENT.addListener((before, now) -> {
            if (Minecraft.getInstance().getConnection() != null) sendPreference();
        });
    }

    private static void sendPreference() {
        Network.sendToServer(new ScaryPreferencePayload(WCISClientConfig.SCARY_CONTENT.get()));
    }

    private static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.isPaused()) return;
        if (!WCISClientConfig.SCARY_CONTENT.get()) {
            banish(level, false);
            return;
        }
        tickGhost(level, player);
        Dread.Stage stage = Dread.stage(player);

        if (stage.reached(Dread.Stage.WATCHED) && --eyesWait <= 0) {
            eyesWait = 160 + RANDOM.nextInt(240);
            BlockPos bed = darkBedNear(level, player);
            if (bed != null) {
                SleepEffects.eyes(bed);
                SleepEffects.whisper(Vec3.atCenterOf(bed));
            }
        }
        // It only shows itself where it is dark.
        if (stage.reached(Dread.Stage.HAUNTED) && ghost == null && --ghostWait <= 0) {
            ghostWait = 400 + RANDOM.nextInt(500);
            if (level.getMaxLocalRawBrightness(player.blockPosition()) < 8) appearAround(level, player);
        }
    }

    /**
     * The player used a bed ({@code SleepClient}): a haunted player lying down in the dark sees it crawl out from under
     * the bed.
     */
    public static void onBedUsed(BlockPos bed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || !WCISClientConfig.SCARY_CONTENT.get()) return;
        if (!Dread.stage(minecraft.player).reached(Dread.Stage.HAUNTED) || !LurkerServer.isDark(minecraft.level, bed) || ghost != null) return;
        // Out at the side of the bed away from the player.
        Vec3 center = Vec3.atBottomCenterOf(bed);
        Vec3 away = center.subtract(minecraft.player.position()).multiply(1.0, 0.0, 1.0);
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
        appear(minecraft.level, minecraft.player, center.add(away.scale(1.2)));
        SleepEffects.eyes(bed);
    }

    // Somewhere on the ground in the dark around the player, a little way off.
    private static void appearAround(ClientLevel level, LocalPlayer player) {
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2.0;
            double distance = 9.0 + RANDOM.nextDouble() * 5.0;
            BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);
            for (int dy = 2; dy >= -3; dy--) {
                BlockPos pos = column.above(dy);
                if (level.getBlockState(pos.below()).isSolidRender() && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getMaxLocalRawBrightness(pos) < 8) {
                    appear(level, player, Vec3.atBottomCenterOf(pos));
                    return;
                }
            }
        }
    }

    private static void appear(ClientLevel level, LocalPlayer player, Vec3 at) {
        LurkerEntity lurker = WCISEntities.LURKER.get().create(level, EntitySpawnReason.TRIGGERED);
        if (lurker == null) return;
        // Facing the player, and still: it watches.
        float yaw = (float) (Mth.atan2(player.getZ() - at.z, player.getX() - at.x) * Mth.RAD_TO_DEG) - 90.0F;
        lurker.setId(GHOST_ID);
        lurker.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        lurker.yBodyRot = lurker.yBodyRotO = yaw;
        lurker.yHeadRot = lurker.yHeadRotO = yaw;
        lurker.setNoAi(true);
        lurker.setSilent(true);
        level.addEntity(lurker);
        ghost = lurker;
        ghostAge = 0;
        seenTicks = 0;
    }

    // The hallucination lasts until it is looked at, approached, or a few seconds have passed.
    private static void tickGhost(ClientLevel level, LocalPlayer player) {
        if (ghost == null) return;
        if (ghost.isRemoved() || ghost.level() != level) {
            ghost = null;
            return;
        }
        ghostAge++;
        Vec3 toGhost = ghost.getEyePosition().subtract(player.getEyePosition());
        boolean lookedAt = toGhost.normalize().dot(player.getLookAngle()) > 0.97 && player.hasLineOfSight(ghost);
        seenTicks = lookedAt ? seenTicks + 1 : 0;
        if (seenTicks >= SEEN_TICKS || toGhost.lengthSqr() < 3.5 * 3.5 || ghostAge > GHOST_TICKS) banish(level, true);
    }

    private static void banish(ClientLevel level, boolean withEffects) {
        if (ghost == null) return;
        if (withEffects) SleepEffects.shadowBurst(ghost.position().add(0.0, 0.4, 0.0));
        level.removeEntity(ghost.getId(), Entity.RemovalReason.DISCARDED);
        ghost = null;
    }

    // The head of a bed in the dark within reach, the nearest one.
    private static @Nullable BlockPos darkBedNear(ClientLevel level, LocalPlayer player) {
        BlockPos center = player.blockPosition();
        BlockPos nearest = null;
        double nearestDistance = BED_RANGE * BED_RANGE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -3, -8), center.offset(8, 3, 8))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BedBlock) || state.getValue(BedBlock.PART) != BedPart.HEAD || !LurkerServer.isDark(level, pos)) continue;
            double distance = pos.distSqr(center);
            if (distance < nearestDistance) {
                nearest = pos.immutable();
                nearestDistance = distance;
            }
        }
        return nearest;
    }
}
