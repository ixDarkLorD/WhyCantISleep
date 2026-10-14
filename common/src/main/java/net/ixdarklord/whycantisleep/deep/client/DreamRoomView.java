package net.ixdarklord.whycantisleep.deep.client;

import net.ixdarklord.coolcatcanvas.api.client.viewport.SceneViewport;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.deep.DreamRoom;
import net.ixdarklord.whycantisleep.network.DreamRoomBlocksPayload;
import net.ixdarklord.whycantisleep.network.DreamRoomEntitiesPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The dreamer's room, live, as their Window shows it: a small scene of the blocks round their bed and whatever is
 * moving there ({@link DreamRoom} on the server sends both), drawn by CoolCatLib's {@link SceneViewport} into a texture
 * the Window in the sky wears ({@link DeepClient}). Kept cheap: a small texture, redrawn a few times a second at most and only while something in it
 * moves, and not at all while no Window is in sight.
 */
public final class DreamRoomView {
    /** The texture the room is drawn into, which the Window in the sky wears. */
    public static final Identifier TEXTURE = WhyCantISleep.id("dream/room");
    private static final int SIZE = 192;
    private static final int REFRESH_RATE = 6;
    // The camera stands at the foot of the bed, a little above it, looking into the room.
    private static final float PITCH = 28.0F;

    private static @Nullable SceneViewport view;
    private static final List<BlockState> BLOCKS = new ArrayList<>();
    private static final Map<Integer, Entity> VISITORS = new HashMap<>();
    private static List<DreamRoomEntitiesPayload.Visitor> present = List.of();
    private static boolean hasRoom;
    private static float daylight = 1.0F;

    private DreamRoomView() {
    }

    public static void init() {
        ClientPlayerEvents.LEAVE.register(player -> forget());
    }

    /** Whether there is a room to show yet. */
    public static boolean has() {
        return hasRoom && view != null;
    }

    /** Called each frame a Window is drawn: keeps the scene being drawn. */
    public static @Nullable SceneViewport use() {
        if (view == null) return null;
        view.use();
        return view;
    }

    /** How much light there is in the room, 0 to 1: the Window is as dark as the room is. */
    public static float daylight() {
        return daylight;
    }

    public static void onBlocks(DreamRoomBlocksPayload payload) {
        if (payload.states().length != DreamRoom.SIDE * DreamRoom.SIDE * DreamRoom.HEIGHT) return;
        if (view == null) view = SceneViewport.create(TEXTURE, SIZE, SIZE).refreshRate(REFRESH_RATE);
        BLOCKS.clear();
        for (int id : payload.states()) BLOCKS.add(Block.stateById(id));
        view.camera(payload.facing() + 180.0F, PITCH);
        hasRoom = true;
        rebuild();
    }

    public static void onVisitors(DreamRoomEntitiesPayload payload) {
        daylight = payload.daylight();
        present = payload.visitors();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        // Things that left the room go; things still in it are kept, so each stays the same one from update to update.
        VISITORS.keySet().removeIf(id -> present.stream().noneMatch(visitor -> visitor.id() == id));
        for (DreamRoomEntitiesPayload.Visitor visitor : present) {
            Entity entity = VISITORS.computeIfAbsent(visitor.id(), id -> EntityType.byString(visitor.type())
                    .map(type -> (Entity) type.create(minecraft.level, EntitySpawnReason.LOAD)).orElse(null));
            if (entity == null) continue;
            entity.setYRot(visitor.yaw());
            entity.setYHeadRot(visitor.yaw());
            if (entity instanceof LivingEntity living) living.yBodyRot = visitor.yaw();
        }
        rebuild();
    }

    // The scene again from what is known: every block of the box, then each thing in the room where it stands.
    private static void rebuild() {
        if (view == null || !hasRoom) return;
        view.clear();
        int i = 0;
        for (int y = -DreamRoom.BELOW; y <= DreamRoom.ABOVE; y++) {
            for (int z = -DreamRoom.RADIUS; z <= DreamRoom.RADIUS; z++) {
                for (int x = -DreamRoom.RADIUS; x <= DreamRoom.RADIUS; x++) {
                    BlockState state = BLOCKS.get(i++);
                    if (!state.isAir()) view.block(new BlockPos(x, y, z), state);
                }
            }
        }
        for (DreamRoomEntitiesPayload.Visitor visitor : present) {
            Entity entity = VISITORS.get(visitor.id());
            if (entity != null) view.entity(entity, visitor.x(), visitor.y(), visitor.z(), visitor.yaw());
        }
    }

    private static void forget() {
        BLOCKS.clear();
        VISITORS.clear();
        present = List.of();
        hasRoom = false;
        if (view != null) {
            view.close();
            view = null;
        }
    }
}
