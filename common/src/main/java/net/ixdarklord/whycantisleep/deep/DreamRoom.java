package net.ixdarklord.whycantisleep.deep;

import net.ixdarklord.whycantisleep.network.DreamRoomBlocksPayload;
import net.ixdarklord.whycantisleep.network.DreamRoomEntitiesPayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The live view in a Window: the server watches the room a dreamer fell asleep in and tells their client what is in it,
 * so the Window shows it as it is now, not as it was: live. It stays cheap: the room is a small box round the bed; its
 * blocks are looked at four times a second and sent only when they change; what moves in it (the living things and
 * other players, up to a dozen) is sent every tick while anything is there, and while the room is empty only now and
 * then, for its light. The room is kept loaded while they dream, so the
 * world goes on there: a Lurker could come to stand by the empty bed.
 */
public final class DreamRoom {
    /**
     * The box sent: this far out from the bed to each side, this far below it and above it. It stops two blocks up, so
     * the Window, looking down into the room, sees the bed and not the ceiling over it.
     */
    public static final int RADIUS = 6;
    public static final int BELOW = 1;
    public static final int ABOVE = 2;
    public static final int SIDE = RADIUS * 2 + 1;
    public static final int HEIGHT = BELOW + ABOVE + 1;
    private static final int BLOCKS_EVERY = 5;
    private static final int TICKET_EVERY = 20;
    private static final int EMPTY_EVERY = 20;
    private static final int MOST_VISITORS = 12;

    // What each dreamer's client was last sent: the blocks' hash, and whether anything was moving.
    private static final Map<UUID, Integer> SENT_BLOCKS = new HashMap<>();
    private static final Map<UUID, Boolean> SENT_VISITORS = new HashMap<>();

    private DreamRoom() {
    }

    /** A dreamer's tick in the Deep: keeps their room loaded and their Window up to date. */
    static void tick(ServerPlayer player, Dream.Place from) {
        ServerLevel home = player.level().getServer().getLevel(from.dimension());
        if (home == null) return;
        BlockPos bed = BlockPos.containing(from.pos());
        // A portal's ticket: refreshed while they dream, it lapses by itself a few seconds after they wake.
        if (player.tickCount % TICKET_EVERY == 0) home.getChunkSource().addTicketWithRadius(TicketType.PORTAL, ChunkPos.containing(bed), 1);
        if (player.tickCount % BLOCKS_EVERY == 0) sendBlocks(player, home, bed, bedFacing(home, bed, from.yaw()), false);
        sendVisitors(player, home, from.pos());
    }

    /** The dream began: send the whole room at once, whatever was sent before. */
    static void begin(ServerPlayer player, Dream.Place from) {
        ServerLevel home = player.level().getServer().getLevel(from.dimension());
        SENT_BLOCKS.remove(player.getUUID());
        SENT_VISITORS.remove(player.getUUID());
        BlockPos bed = BlockPos.containing(from.pos());
        if (home != null) sendBlocks(player, home, bed, bedFacing(home, bed, from.yaw()), true);
    }

    // Which way the bed points, from its foot to its head, as a yaw; the sleeper's own yaw if there is no bed there.
    private static float bedFacing(ServerLevel home, BlockPos bed, float otherwise) {
        BlockState state = home.hasChunkAt(bed) ? home.getBlockState(bed) : null;
        return state != null && state.getBlock() instanceof BedBlock ? state.getValue(BedBlock.FACING).toYRot() : otherwise;
    }

    static void forget(ServerPlayer player) {
        SENT_BLOCKS.remove(player.getUUID());
        SENT_VISITORS.remove(player.getUUID());
    }

    private static void sendBlocks(ServerPlayer player, ServerLevel home, BlockPos bed, float facing, boolean always) {
        if (!home.hasChunkAt(bed)) return;
        int[] states = new int[SIDE * SIDE * HEIGHT];
        int i = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = -BELOW; y <= ABOVE; y++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                for (int x = -RADIUS; x <= RADIUS; x++) {
                    pos.setWithOffset(bed, x, y, z);
                    states[i++] = home.hasChunkAt(pos) ? Block.getId(home.getBlockState(pos)) : 0;
                }
            }
        }
        int hash = Arrays.hashCode(states);
        Integer sent = SENT_BLOCKS.get(player.getUUID());
        if (!always && sent != null && sent == hash) return;
        SENT_BLOCKS.put(player.getUUID(), hash);
        WCISNetwork.sendToPlayer(player, new DreamRoomBlocksPayload(states, facing));
    }

    private static void sendVisitors(ServerPlayer player, ServerLevel home, Vec3 bed) {
        AABB room = new AABB(bed, bed).inflate(RADIUS + 0.5, 0.0, RADIUS + 0.5).expandTowards(0.0, ABOVE, 0.0).expandTowards(0.0, -BELOW, 0.0);
        List<DreamRoomEntitiesPayload.Visitor> visitors = new ArrayList<>();
        // Living things and other players; not the dreamer, nor anyone unseen (a spectator, or invisible).
        for (LivingEntity entity : home.getEntitiesOfClass(LivingEntity.class, room, entity -> entity.isAlive() && entity != player
                && !entity.isSpectator() && !entity.isInvisible())) {
            if (visitors.size() >= MOST_VISITORS) break;
            boolean isPlayer = entity instanceof Player;
            ItemStack held = entity.getMainHandItem();
            float walk = (float) Math.min(Math.hypot(entity.getX() - entity.xo, entity.getZ() - entity.zo) * 4.0, 1.0);
            visitors.add(new DreamRoomEntitiesPayload.Visitor(entity.getId(), BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),
                    isPlayer ? entity.getUUID().toString() : "", isPlayer ? entity.getName().getString() : "",
                    (float) (entity.getX() - bed.x), (float) (entity.getY() - bed.y), (float) (entity.getZ() - bed.z),
                    entity.getYRot(), entity.yBodyRot, entity.getYHeadRot(), entity.getXRot(), entity.getPose().name(), walk,
                    held.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(held.getItem()).toString()));
        }
        boolean moving = !visitors.isEmpty();
        Boolean wasMoving = SENT_VISITORS.get(player.getUUID());
        // An empty room is sent once, to clear the last of them away, and then only now and then, for its light.
        if (!moving && Boolean.FALSE.equals(wasMoving) && player.tickCount % EMPTY_EVERY != 0) return;
        SENT_VISITORS.put(player.getUUID(), moving);
        BlockPos at = BlockPos.containing(bed).above();
        float daylight = home.hasChunkAt(at) ? home.getBrightness(LightLayer.SKY, at) / 15.0F * (1.0F - home.getSkyDarken() / 11.0F) : 1.0F;
        WCISNetwork.sendToPlayer(player, new DreamRoomEntitiesPayload(visitors, Math.max(daylight, home.getBrightness(LightLayer.BLOCK, at) / 15.0F)));
    }
}
