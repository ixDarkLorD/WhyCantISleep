package net.ixdarklord.whycantisleep.deep;

import com.mojang.serialization.Codec;
import net.ixdarklord.coolcatcore.api.attachment.Attachment;
import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.mind.Mind;
import net.ixdarklord.whycantisleep.mind.MindServer;
import net.ixdarklord.whycantisleep.network.DreamTransitionPayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The Lucid Deep's server side: going in, being there, and waking up.
 * <ul>
 *     <li><b>Going in.</b> A player who lies down holding a {@linkplain WCISItems#DREAM_LENS Dream Lens} while their
 *     mind is clear (lucidity {@value #ENTER_LUCIDITY} or more) dreams lucidly. A second into their sleep they start to
 *     slip under: their view falls away from the bed, down into the dark ({@code DreamTransition} on the client); then,
 *     with no loading screen (the screen is black while the Deep loads), they are falling out of the sky of the Deep,
 *     face first, and land on the arrival island. High in the sky to the north hangs their Window, a great square showing
 *     the room they fell asleep in as it is now ({@link DreamRoom}).</li>
 *     <li><b>Being there.</b> Their lucidity drains while they dream, slowly: it is how long they can stay. Everything
 *     they pick up there is <i>dreamt</i>.</li>
 *     <li><b>Waking.</b> Looking up at their Window and focusing through the lens wakes them where they slept, and what
 *     they dreamt becomes real. Dying there, falling out of the Deep or running out of lucidity wakes them too, but what
 *     they dreamt fades on the way: they keep only what they took in.</li>
 * </ul>
 * Every number here is a first guess, to be tuned by play.
 */
public final class LucidDeep {
    public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, WhyCantISleep.id("lucid_deep"));
    /** The lucidity a dreamer needs to dream lucidly. */
    public static final float ENTER_LUCIDITY = 75.0F;
    /** Where every dreamer's Window hangs in their sky: due north, this many degrees up, and this many degrees across. */
    public static final float WINDOW_ELEVATION = 24.0F;
    public static final float WINDOW_SIZE = 42.0F;
    // How squarely a dreamer must look at their Window for the lens to wake them, as the cosine of the angle off it.
    private static final double WINDOW_AIM = Math.cos(Math.toRadians(WINDOW_SIZE * 0.6));
    // How many ticks into their sleep a dreamer starts to slip under, and how long that takes. The night doesn't pass
    // while they slip ({@code PlayerMixin}), however long it takes.
    private static final int DRIFT_START = 20;
    private static final int DRIFT_TICKS = 110;
    // How far above the island they come out of the sky.
    private static final int FALL_HEIGHT = 40;
    // Lucidity a dreamer loses each second in the Deep: from 75 that is a quarter of an hour.
    private static final float DRAIN_PER_SECOND = 0.08F;
    // The arrival island: a disc this wide, this high, at the middle of the Deep.
    private static final BlockPos ARRIVAL = new BlockPos(0, 150, 0);
    /** The height a dreamer lands at: the top of the arrival island. */
    public static final int ISLAND_TOP = 150;
    // How long a dreamer hangs in the sky waiting for their client, at most.
    private static final int HOLD_TICKS = 200;
    private static final int ISLAND_RADIUS = 6;
    // A dreamer who falls this far below the bottom of the Deep has fallen out of the dream.
    private static final int FALL_OUT = 16;

    /** Where an item came from, kept on the stack while its holder dreams: carried in, or dreamt (found in the Deep). */
    public static final int CARRIED = 1;
    public static final int DREAMT = 2;
    public static final Attachment<Integer> ORIGIN = Mind.ATTACHMENTS.builder("dream_origin", Codec.INT, 0).build();

    // Dreamers still falling to the island.
    // Players slipping under, and the tick each started on (the game's own sleep counter stops at 100).
    private static final Map<UUID, Integer> DRIFTING = new HashMap<>();
    private static final Set<UUID> FALLING = new HashSet<>();
    // Dreamers hanging where they came out of the sky until their client is ready to watch them fall, and since when.
    private static final Map<UUID, Integer> HELD = new HashMap<>();

    private LucidDeep() {
    }

    public static void init() {
        PlayerEvents.END_TICK.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) tick(serverPlayer);
        });
        PlayerEvents.LEAVE.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                DreamRoom.forget(serverPlayer);
                DreamCastle.forget(serverPlayer);
            }
            DRIFTING.remove(player.getUUID());
            FALLING.remove(player.getUUID());
            HELD.remove(player.getUUID());
        });
    }

    /** Whether a player is slipping under: lying in bed with the lens, on the way to the Deep. */
    public static boolean isSlipping(Player player) {
        return DRIFTING.containsKey(player.getUUID());
    }

    public static boolean isDeep(Level level) {
        return level.dimension() == LEVEL;
    }

    /** The way to a dreamer's Window from where they stand: north, and up. */
    public static Vec3 windowDirection() {
        float up = WINDOW_ELEVATION * Mth.DEG_TO_RAD;
        return new Vec3(0.0, Mth.sin(up), -Mth.cos(up));
    }

    private static void tick(ServerPlayer player) {
        // Anyone in the Deep, dreaming or not, sees the storm over the castle.
        if (isDeep(player.level())) {
            DreamCastle.tell(player, (ServerLevel) player.level());
        } else {
            DreamCastle.forget(player);
        }
        Dream dream = Dream.DREAM.get(player);
        if (!dream.dreaming()) {
            drift(player);
            return;
        }
        // Somewhere else while dreaming (sent off by a command, or the Deep is gone): the dream simply ends.
        if (!isDeep(player.level())) {
            Dream.DREAM.set(player, Dream.AWAKE);
            clearOrigins(player);
            DreamRoom.forget(player);
            DreamCastle.forget(player);
            return;
        }
        DreamRoom.tick(player, dream.from().get());
        // Hanging in the sky, waiting for their client: held still, then let go even if it never says.
        Integer held = HELD.get(player.getUUID());
        if (held != null) {
            player.setDeltaMovement(Vec3.ZERO);
            if (player.tickCount - held > HOLD_TICKS) release(player);
        }
        // Coming down out of the sky: the fall doesn't hurt; hitting the island, they are told.
        if (FALLING.contains(player.getUUID()) && held == null) {
            player.fallDistance = 0.0;
            if (player.onGround() || player.isInWater()) {
                FALLING.remove(player.getUUID());
                WCISNetwork.sendToPlayer(player, new DreamTransitionPayload(DreamTransitionPayload.LANDED, player.getX(), player.getY(), player.getZ(), player.getYRot()));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1.0F, 0.7F);
                player.sendOverlayMessage(Component.translatable("message.whycantisleep.deep.arrived").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        if (player.tickCount % 10 == 0) markDreamt(player);
        if (player.tickCount % 20 == 0 && !player.isCreative() && !player.isSpectator()) {
            MindServer.addLucidity(player, -DRAIN_PER_SECOND);
            if (Mind.get(player).lucidity() <= 0.0F) wake(player, false);
        }
        if (player.getY() < player.level().getMinY() - FALL_OUT) wake(player, false);
    }

    private static boolean holdsLens(ServerPlayer player) {
        return player.getMainHandItem().is(WCISItems.DREAM_LENS.get()) || player.getOffhandItem().is(WCISItems.DREAM_LENS.get());
    }

    // A sleeper holding the lens with a clear mind slips under: their client is told to show it, and when it has,
    // they go under. Waking first (or being woken) stops it.
    private static void drift(ServerPlayer player) {
        boolean drifting = DRIFTING.containsKey(player.getUUID());
        if (!player.isSleeping() || !holdsLens(player)) {
            if (drifting) {
                DRIFTING.remove(player.getUUID());
                WCISNetwork.sendToPlayer(player, new DreamTransitionPayload(DreamTransitionPayload.CANCEL, 0.0, 0.0, 0.0, 0.0F));
            }
            return;
        }
        int asleep = player.getSleepTimer();
        if (!drifting && asleep == DRIFT_START) {
            if (Mind.get(player).lucidity() < ENTER_LUCIDITY) {
                player.sendOverlayMessage(Component.translatable("message.whycantisleep.deep.too_clouded").withStyle(ChatFormatting.LIGHT_PURPLE));
                return;
            }
            DRIFTING.put(player.getUUID(), player.tickCount);
            Vec3 eye = player.getEyePosition();
            WCISNetwork.sendToPlayer(player, new DreamTransitionPayload(DreamTransitionPayload.DRIFT, eye.x, eye.y, eye.z, player.getYRot()));
        } else if (drifting && player.tickCount - DRIFTING.get(player.getUUID()) >= DRIFT_TICKS) {
            DRIFTING.remove(player.getUUID());
            goUnder(player);
        }
    }

    // A dreamer goes under: out of their bed and into the sky of the Deep, falling face first towards the island.
    private static void goUnder(ServerPlayer player) {
        ServerLevel deep = player.level().getServer().getLevel(LEVEL);
        if (deep == null) return;
        Dream.Place from = new Dream.Place(player.level().dimension(), player.position(), player.getYRot());
        player.stopSleepInBed(true, true);
        for (ItemStack stack : everything(player)) if (!stack.isEmpty()) LucidDeep.ORIGIN.set(stack, CARRIED);

        buildIsland(deep);
        // They come out of the sky over the middle of the island, face down, their Window to the north.
        Vec3 arrive = Vec3.atBottomCenterOf(ARRIVAL).add(0.0, FALL_HEIGHT, 1.5);
        player.teleport(new TeleportTransition(deep, arrive, Vec3.ZERO, 180.0F, 90.0F, TeleportTransition.DO_NOTHING));
        Dream.DREAM.set(player, new Dream(Optional.of(from)));
        FALLING.add(player.getUUID());
        player.setNoGravity(true);
        HELD.put(player.getUUID(), player.tickCount);
        DreamRoom.begin(player, from);
        deep.playSound(null, arrive.x, arrive.y, arrive.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.5F);
    }

    // The arrival island, built the first time anyone comes: a disc of dark stone with a rim, at the middle of the Deep,
    // with the air above it cleared in case it formed inside an island. Its lantern says it is built.
    private static void buildIsland(ServerLevel deep) {
        if (deep.getBlockState(ARRIVAL).is(Blocks.SOUL_LANTERN)) return;
        for (int dx = -ISLAND_RADIUS - 2; dx <= ISLAND_RADIUS + 2; dx++) {
            for (int dz = -ISLAND_RADIUS - 2; dz <= ISLAND_RADIUS + 2; dz++) {
                for (int dy = 0; dy <= 5; dy++) {
                    if (dx * dx + dz * dz + dy * dy * 2 <= (ISLAND_RADIUS + 2) * (ISLAND_RADIUS + 2)) deep.setBlockAndUpdate(ARRIVAL.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int dx = -ISLAND_RADIUS - 1; dx <= ISLAND_RADIUS + 1; dx++) {
            for (int dz = -ISLAND_RADIUS - 1; dz <= ISLAND_RADIUS + 1; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > ISLAND_RADIUS + 0.5) continue;
                boolean rim = distance > ISLAND_RADIUS - 0.7;
                BlockState top = rim ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
                deep.setBlockAndUpdate(ARRIVAL.offset(dx, -1, dz), top);
                // Underneath, it tapers to a point, as islands do.
                int depth = (int) Math.round((ISLAND_RADIUS + 1 - distance) * 1.3);
                for (int y = 2; y <= depth + 1; y++) deep.setBlockAndUpdate(ARRIVAL.offset(dx, -y, dz), Blocks.DEEPSLATE.defaultBlockState());
            }
        }
        // A lantern at the middle, the one warm light on it.
        deep.setBlockAndUpdate(ARRIVAL, Blocks.SOUL_LANTERN.defaultBlockState());
    }

    /** A dreamer's client is ready to watch them fall ({@code DreamReadyPayload}): they drop. */
    public static void release(ServerPlayer player) {
        if (HELD.remove(player.getUUID()) == null) return;
        player.setNoGravity(false);
    }

    /**
     * A dreamer focused through their lens ({@link DreamLensItem}): looking up at their Window, they wake with what they
     * dreamt; looking anywhere else, they are told where to look.
     */
    public static void focus(ServerPlayer player) {
        if (!Dream.DREAM.get(player).dreaming() || !isDeep(player.level())) return;
        if (player.getLookAngle().dot(windowDirection()) >= WINDOW_AIM) {
            wake(player, true);
        } else {
            player.sendOverlayMessage(Component.translatable("message.whycantisleep.deep.look_up").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /**
     * Wakes a dreamer in the place they fell asleep. Woken through their Window ({@code kept}), what they dreamt is
     * theirs; woken any other way, it fades, and they keep only what they carried in.
     */
    public static void wake(ServerPlayer player, boolean kept) {
        Dream dream = Dream.DREAM.get(player);
        if (dream.from().isEmpty()) return;
        Dream.Place from = dream.from().get();
        ServerLevel home = player.level().getServer().getLevel(from.dimension());
        if (home == null) home = player.level().getServer().overworld();
        int faded = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            if (!kept && LucidDeep.ORIGIN.get(stack) == DREAMT) {
                inventory.setItem(slot, ItemStack.EMPTY);
                faded++;
            }
        }
        clearOrigins(player);
        Dream.DREAM.set(player, Dream.AWAKE);
        DreamRoom.forget(player);
        DreamCastle.forget(player);
        player.fallDistance = 0.0;
        player.teleport(new TeleportTransition(home, from.pos(), Vec3.ZERO, from.yaw(), 0.0F, TeleportTransition.DO_NOTHING));
        String key = kept ? "message.whycantisleep.deep.woke" : faded > 0 ? "message.whycantisleep.deep.woke_faded" : "message.whycantisleep.deep.woke_lost";
        player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
        home.playSound(null, from.pos().x, from.pos().y, from.pos().z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, kept ? 0.8F : 0.5F);
    }

    /**
     * A dreamer is about to die in the Deep ({@code ServerPlayerMixin}): they wake instead, shaken, with what they
     * dreamt gone. Returns whether the death was turned aside.
     */
    public static boolean onDeath(ServerPlayer player) {
        if (!Dream.DREAM.get(player).dreaming() || !isDeep(player.level())) return false;
        player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.3F));
        player.clearFire();
        player.removeAllEffects();
        wake(player, false);
        return true;
    }

    // Anything new in a dreamer's inventory was found in the Deep.
    private static void markDreamt(ServerPlayer player) {
        for (ItemStack stack : everything(player)) {
            if (!stack.isEmpty() && LucidDeep.ORIGIN.get(stack) == 0) LucidDeep.ORIGIN.set(stack, DREAMT);
        }
    }

    private static void clearOrigins(ServerPlayer player) {
        for (ItemStack stack : everything(player)) if (!stack.isEmpty()) LucidDeep.ORIGIN.reset(stack);
    }

    private static List<ItemStack> everything(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<ItemStack> stacks = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) stacks.add(inventory.getItem(slot));
        return stacks;
    }
}
