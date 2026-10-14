package net.ixdarklord.whycantisleep.deep.client;

import net.ixdarklord.coolcatcanvas.api.client.viewport.SceneViewport;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.deep.DreamRoom;
import net.ixdarklord.whycantisleep.network.DreamRoomBlocksPayload;
import net.ixdarklord.whycantisleep.network.DreamRoomEntitiesPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The dreamer's room, live, as their Window shows it: looking down at an angle, from beyond a corner at the foot of
 * their bed, at themselves lying asleep in it, with the room round them and whatever is moving there ({@link DreamRoom} on the server sends the blocks
 * and the moving things; the sleeper is the dreamer's own likeness, made here). It is drawn by CoolCatLib's
 * {@link SceneViewport} into a texture the Window in the sky wears ({@link DeepClient}). Kept cheap: a small texture,
 * redrawn a few times a second at most and only while something in it moves, and not at all while no Window is in
 * sight.
 */public final class DreamRoomView {
    /** The texture the room is drawn into, which the Window in the sky wears. */
    public static final Identifier TEXTURE = WhyCantISleep.id("dream/room");
    private static final int SIZE = 192;
    private static final int REFRESH_RATE = 6;
    // The camera looks down at the sleeper from beyond a corner at the foot of the bed: turned this far off the bed's
    // line, this steeply, with this much of the room across the picture, centred this far along the bed from its head.
    private static final float CORNER = 40.0F;
    private static final float PITCH = 38.0F;
    private static final double SPAN = 5.5;
    private static final double LOOK_ALONG = 0.6;
    // Where the sleeper lies: this far from the middle of the bed's head towards its foot, so that, laid out as a
    // sleeper is, their head is on the pillow and their feet at the end of the bed.
    private static final double SLEEPER_ALONG = 1.85;
    private static final double SLEEPER_HEIGHT = 0.5625;
    private static final float SLEEPER_TURN = 90.0F;

    private static @Nullable SceneViewport view;
    private static final List<BlockState> BLOCKS = new ArrayList<>();
    private static final Map<Integer, Entity> VISITORS = new HashMap<>();
    private static List<DreamRoomEntitiesPayload.Visitor> present = List.of();
    private static boolean hasRoom;
    private static float daylight = 1.0F;
    private static float bedYaw;
    private static @Nullable RemotePlayer sleeper;

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
        bedYaw = payload.facing();
        // Over the foot of the bed, looking along it towards the head.
        Vec3 toFoot = Vec3.directionFromRotation(0.0F, bedYaw).scale(-1.0);
        view.camera(bedYaw + 180.0F + CORNER, PITCH).frame(SPAN, 0.5 + toFoot.x * LOOK_ALONG, 0.4, 0.5 + toFoot.z * LOOK_ALONG);
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
        RemotePlayer body = sleeper();
        if (body != null) {
            Vec3 toFoot = Vec3.directionFromRotation(0.0F, bedYaw).scale(-1.0);
            // A sleeper is turned a quarter round from the way they face, to lie along the bed with their head on its pillow.
            view.entity(body, 0.5 + toFoot.x * SLEEPER_ALONG, SLEEPER_HEIGHT, 0.5 + toFoot.z * SLEEPER_ALONG, bedYaw + SLEEPER_TURN);
        }
    }

    // The dreamer as they lie in their bed back home: a likeness of them, with their own skin, asleep.
    private static @Nullable RemotePlayer sleeper() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return null;
        if (sleeper == null || sleeper.level() != minecraft.level) {
            sleeper = new RemotePlayer(minecraft.level, minecraft.player.getGameProfile());
            sleeper.setPose(Pose.SLEEPING);
        }
        // Its own rotation stays at nothing: the viewport turns it into place, and would otherwise turn it twice.
        sleeper.setYRot(0.0F);
        sleeper.setYHeadRot(0.0F);
        sleeper.yBodyRot = 0.0F;
        return sleeper;
    }

    private static void forget() {
        BLOCKS.clear();
        VISITORS.clear();
        present = List.of();
        hasRoom = false;
        sleeper = null;
        if (view != null) {
            view.close();
            view = null;
        }
    }
}
