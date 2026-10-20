package net.ixdarklord.whycantisleep.between.client;

import net.ixdarklord.coolcatcanvas.api.client.viewport.SceneViewport;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.between.DreamRoom;
import net.ixdarklord.whycantisleep.network.DreamRoomBlocksPayload;
import net.ixdarklord.whycantisleep.network.DreamRoomEntitiesPayload;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The dreamer's room, live, as their Window shows it: looking down from over the foot of their bed at themselves
 * lying asleep in it, with the room round them and whatever is moving there ({@link DreamRoom} on the server sends the blocks
 * and the moving things; the sleeper is the dreamer's own likeness, made here). It is drawn by CoolCatLib's
 * {@link SceneViewport} into a texture the Window in the sky wears ({@link BetweenClient}). Live: what moves in the room
 * (living things and other players, in their own skins) comes every tick and is drawn smoothly between, up to sixty
 * times a second, walking, turning their heads, crouching, holding what they hold. Kept cheap: a small texture, drawn
 * again only while something in it moves, and not at all while no Window is in sight.
 */public final class DreamRoomView {
    /** The texture the room is drawn into, which the Window in the sky wears. */
    public static final Identifier TEXTURE = WhyCantISleep.id("dream/room");
    private static final int SIZE = 192;
    private static final int REFRESH_RATE = 60;
    // How long apart the server's updates are, which the motion is smoothed over.
    private static final float UPDATE_MILLIS = 50.0F;
    // The camera hangs over the foot of the bed looking down at the sleeper, this steeply, with this much of the room
    // across the picture, centred this far along the bed from its head.
    private static final float PITCH = 62.0F;
    private static final double SPAN = 6.0;
    private static final double LOOK_ALONG = 0.6;
    // How high the game puts a sleeper over the floor of their bed's block.
    private static final double SLEEPER_HEIGHT = 0.6875;

    private static @Nullable SceneViewport view;
    private static final List<BlockState> BLOCKS = new ArrayList<>();
    private static final Map<Integer, Entity> VISITORS = new HashMap<>();
    private static List<DreamRoomEntitiesPayload.Visitor> present = List.of();
    // Where each was at the update before, to move it smoothly to where it is now; and when the latest came.
    private static final Map<Integer, DreamRoomEntitiesPayload.Visitor> BEFORE = new HashMap<>();
    private static long arrived;
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

    /** Called each frame a Window is drawn: keeps the scene being drawn, moving what is in it on smoothly. */
    public static @Nullable SceneViewport use() {
        if (view == null) return null;
        view.use();
        if (!present.isEmpty()) rebuild();
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
        view.camera(bedYaw + 180.0F, PITCH).frame(SPAN, 0.5 + toFoot.x * LOOK_ALONG, 0.4, 0.5 + toFoot.z * LOOK_ALONG);
        hasRoom = true;
        rebuild();
    }

    public static void onVisitors(DreamRoomEntitiesPayload payload) {
        daylight = payload.daylight();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        // Each moves on from wherever it is drawn now, so a late update never makes it jump back.
        float through = smoothing();
        Map<Integer, DreamRoomEntitiesPayload.Visitor> now = new HashMap<>();
        for (DreamRoomEntitiesPayload.Visitor visitor : present) now.put(visitor.id(), blend(BEFORE.getOrDefault(visitor.id(), visitor), visitor, through));
        BEFORE.clear();
        BEFORE.putAll(now);
        present = payload.visitors();
        arrived = System.currentTimeMillis();
        // Things that left the room go; things still in it are kept, so each stays the same one from update to update.
        VISITORS.keySet().removeIf(id -> present.stream().noneMatch(visitor -> visitor.id() == id));
        BEFORE.keySet().removeIf(id -> present.stream().noneMatch(visitor -> visitor.id() == id));
        for (DreamRoomEntitiesPayload.Visitor visitor : present) {
            Entity entity = VISITORS.computeIfAbsent(visitor.id(), id -> make(minecraft, visitor));
            if (entity == null) continue;
            // A tick of its life: its walk, its idle movements.
            entity.tickCount++;
            if (entity instanceof LivingEntity living) {
                living.walkAnimation.update(visitor.walk(), 0.4F, 1.0F);
                ItemStack held = visitor.held().isEmpty() ? ItemStack.EMPTY
                        : new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(visitor.held())));
                if (!ItemStack.matches(held, living.getMainHandItem())) living.setItemSlot(EquipmentSlot.MAINHAND, held);
            }
            try {
                entity.setPose(Pose.valueOf(visitor.pose()));
            } catch (IllegalArgumentException ignored) {
                // A pose this client doesn't know: it keeps the one it has.
            }
        }
        rebuild();
    }

    // Something new in the room: one of its kind, or for a player, a likeness of them with their own skin.
    private static @Nullable Entity make(Minecraft minecraft, DreamRoomEntitiesPayload.Visitor visitor) {
        if (!visitor.player().isEmpty()) {
            UUID uuid = UUID.fromString(visitor.player());
            PlayerInfo info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(uuid);
            GameProfile profile = info != null ? info.getProfile() : new GameProfile(uuid, visitor.name());
            return new RemotePlayer(minecraft.level, profile);
        }
        return EntityType.byString(visitor.type()).map(type -> (Entity) type.create(minecraft.level, EntitySpawnReason.LOAD)).orElse(null);
    }

    // How far from the update before to the latest it is now, 0 to 1.
    private static float smoothing() {
        return Mth.clamp((System.currentTimeMillis() - arrived) / UPDATE_MILLIS, 0.0F, 1.0F);
    }

    // A visitor part way from where it was to where it is: its place and its turns, the turns the short way round.
    private static DreamRoomEntitiesPayload.Visitor blend(DreamRoomEntitiesPayload.Visitor from, DreamRoomEntitiesPayload.Visitor to, float t) {
        return new DreamRoomEntitiesPayload.Visitor(to.id(), to.type(), to.player(), to.name(),
                Mth.lerp(t, from.x(), to.x()), Mth.lerp(t, from.y(), to.y()), Mth.lerp(t, from.z(), to.z()),
                Mth.rotLerp(t, from.yaw(), to.yaw()), Mth.rotLerp(t, from.bodyYaw(), to.bodyYaw()), Mth.rotLerp(t, from.headYaw(), to.headYaw()),
                Mth.lerp(t, from.pitch(), to.pitch()), to.pose(), to.walk(), to.held());
    }

    // The scene again from what is known: every block of the box, then each thing in the room where it is now.
    private static void rebuild() {
        if (view == null || !hasRoom) return;
        view.clear();
        // Towards the camera (beyond the foot of the bed), and across the bed.
        int footX = Math.round((float) -Vec3.directionFromRotation(0.0F, bedYaw).x);
        int footZ = Math.round((float) -Vec3.directionFromRotation(0.0F, bedYaw).z);
        int i = 0;
        for (int y = -DreamRoom.BELOW; y <= DreamRoom.ABOVE; y++) {
            for (int z = -DreamRoom.RADIUS; z <= DreamRoom.RADIUS; z++) {
                for (int x = -DreamRoom.RADIUS; x <= DreamRoom.RADIUS; x++) {
                    BlockState state = BLOCKS.get(i++);
                    if (!state.isAir() && !inTheWay(x, y, z, footX, footZ)) view.block(new BlockPos(x, y, z), state);
                }
            }
        }
        float through = smoothing();
        for (DreamRoomEntitiesPayload.Visitor latest : present) {
            Entity entity = VISITORS.get(latest.id());
            if (entity == null) continue;
            DreamRoomEntitiesPayload.Visitor at = blend(BEFORE.getOrDefault(latest.id(), latest), latest, through);
            // The viewport turns it to face its way; its body and head turn on it from there, as they do in the world.
            entity.setYRot(0.0F);
            entity.setXRot(at.pitch());
            entity.xRotO = at.pitch();
            entity.setYHeadRot(at.headYaw() - at.yaw());
            if (entity instanceof LivingEntity living) {
                living.yBodyRot = at.bodyYaw() - at.yaw();
                living.yBodyRotO = living.yBodyRot;
                living.yHeadRotO = at.headYaw() - at.yaw();
            }
            view.entity(entity, at.x(), at.y(), at.z(), -at.yaw());
        }
        RemotePlayer body = sleeper();
        if (body != null) {
            // Where the game puts a sleeper: on the middle of the bed's head. Told which way the bed points, it lays the
            // body out from there along it.
            view.entity(body, 0.5, SLEEPER_HEIGHT, 0.5, 0.0F);
        }
    }

    // Whether a block would stand between the camera and the sleeper: anything higher than the bed, over it or on the
    // camera's side of it, in a strip a little wider than the bed. Those are left out, as a drawing of a room cuts away
    // its near wall.
    private static boolean inTheWay(int x, int y, int z, int footX, int footZ) {
        if (y < 1) return false;
        int along = x * footX + z * footZ;
        int across = Math.abs(x * footZ - z * footX);
        return along >= 0 && across <= 1;
    }

    // The dreamer as they lie in their bed back home: a likeness of them, with their own skin, asleep.
    private static @Nullable RemotePlayer sleeper() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return null;
        if (sleeper == null || sleeper.level() != minecraft.level) {
            // The game lays a sleeper out along the bed they are in, from its head; this likeness's bed isn't in the
            // world it lives in, so it is told which way that bed points.
            sleeper = new RemotePlayer(minecraft.level, minecraft.player.getGameProfile()) {
                @Override
                public Direction getBedOrientation() {
                    return Direction.fromYRot(bedYaw);
                }
            };
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
        BEFORE.clear();
        present = List.of();
        hasRoom = false;
        sleeper = null;
        if (view != null) {
            view.close();
            view = null;
        }
    }
}
