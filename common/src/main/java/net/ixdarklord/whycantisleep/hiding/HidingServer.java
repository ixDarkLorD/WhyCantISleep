package net.ixdarklord.whycantisleep.hiding;

import net.ixdarklord.coolcatcore.api.event.v2.common.InteractionEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.LivingEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.coolcatcore.api.event.v2.core.EventResult;
import net.ixdarklord.coolcatcore.api.registry.ReloadListeners;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.HidingEvents;
import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.whycantisleep.api.HidingSpot;
import net.ixdarklord.whycantisleep.api.HidingSpotType;
import net.ixdarklord.whycantisleep.api.HidingSpots;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.HidingPayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hiding, on the server. A player who sneaks and uses a hiding spot with both hands empty ({@link HidingSpots}: a bed,
 * a table, a double chest, a closet door, or what a mod or data pack adds) gets in: they are held there, still, and
 * nothing can take them as a target ({@code PlayerMixin}). Whatever was after them loses them; once they have stayed
 * hidden for the hide time, it gives up and is gone, along with the monsters prowling round the spot
 * ({@link HidingEvents#SHAKEN_OFF}). Sneaking again, being hurt, or being heard brings them out where they went in.
 */
public final class HidingServer {
    /** Entity types that see through hiding and never give up on a hiding player: bosses, and anything a mod or pack adds. */
    public static final TagKey<EntityType<?>> IGNORES_HIDING = TagKey.create(Registries.ENTITY_TYPE, WhyCantISleep.id("ignores_hiding"));
    // How far round the player a mob that was after them is still counted, and how far round the spot one that prowls.
    private static final double HUNTER_RANGE = 48.0;
    private static final double PROWLER_RANGE = 12.0;
    // How near the spot another player out in the open keeps the monsters from vanishing.
    private static final double OTHERS_RANGE = 32.0;
    // How fast what heard a hiding player comes for them.
    private static final double HEARD_SPEED = 1.2;
    // Another hider this near one of a spot's places is in it.
    private static final double PLACE_TAKEN = 0.4;
    // How far round a place a monster in it is looked for; the step along the way out a critter is fitted at, and how
    // hard it leaps at them.
    private static final double PLACE_ROOM = 0.45;
    private static final double MOUTH_STEP = 0.2;
    private static final double CRITTER_LEAP = 0.4;
    // The hair between a player coming out and the face they come out of, so they don't start inside it.
    private static final double AGAINST_GAP = 0.01;
    // A hunter's search: how fast it walks, how near counts as there, how often its path is asked again, how long it
    // may take to come, how long it looks about and how often it glances, how far it walks off and for how long, and
    // how much the noise made adds to the chance it finds them.
    private static final double SEARCH_SPEED = 0.9;
    private static final double SEARCH_REACH = 1.4;
    private static final int SEARCH_REPATH = 3;
    private static final int SEARCH_APPROACH_TICKS = 160;
    private static final int SEARCH_LOOK_TICKS = 70;
    private static final int SEARCH_GLANCE = 14;
    private static final double SEARCH_LEAVE_DISTANCE = 14.0;
    private static final int SEARCH_LEAVE_TICKS = 120;
    private static final float SEARCH_NOISE = 0.5F;
    // Each wall close around a spot takes this much off the hide time.
    private static final float COVER_PER_WALL = 0.1F;

    private static final Map<UUID, Hide> HIDING = new HashMap<>();

    private HidingServer() {
    }

    public static void init() {
        BuiltinHidingSpots.register();
        ReloadListeners.registerServer(WhyCantISleep.id("hiding_spots"), DataHidingSpots.INSTANCE);
        // Both sides answer the same for the spots both know of, so the client doesn't predict a sleep (or a door
        // swinging, a chest opening) instead. Data pack spots only the server knows: the client predicts the block's
        // own use, and the server's answer wins.
        InteractionEvents.USE_BLOCK.register((player, hand, hit) -> {
            // Hidden, nothing within reach can be used: above all a bed overhead, which would be a try to sleep in it.
            if (Hiding.isHiding(player)) return InteractionResult.FAIL;
            if (!canHide(player)) {
                // Opening a chest or closet door someone hides behind finds them: they come out, and it opens as ever.
                if (player instanceof ServerPlayer opener) discover(opener, hit.getBlockPos());
                return InteractionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            BlockState state = player.level().getBlockState(pos);
            // Crouched in front of a table, the look goes in under it, to the floor there or just past it: that is
            // reaching for the table.
            if (hit.getDirection() == Direction.UP && HidingSpots.typeFor(state) == null) {
                for (BlockPos over : List.of(pos.above(), pos.above().relative(player.getDirection().getOpposite()))) {
                    BlockState above = player.level().getBlockState(over);
                    if (HidingSpots.typeFor(above) != null) {
                        pos = over;
                        state = above;
                        break;
                    }
                }
            }
            if (player.level().isClientSide()) return HidingSpots.typeFor(state) != null ? InteractionResult.SUCCESS : InteractionResult.PASS;
            if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            HidingSpotType.Lookup lookup = HidingSpots.find(player.level(), pos, state, player);
            if (lookup.spot() != null) return start(serverPlayer, lookup.spot()) ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            if (lookup.refusal() != null) {
                serverPlayer.sendSystemMessage(lookup.refusal(), true);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        });
        // Nor broken: knocking the spot out from round them would give them away.
        InteractionEvents.ATTACK_BLOCK.register((player, hand, pos, face) -> Hiding.isHiding(player) ? EventResult.INTERRUPT : EventResult.PASS);
        PlayerEvents.END_TICK.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) tick(serverPlayer);
        });
        LivingEvents.DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player && amount > 0.0F) stop(player, HidingEvents.Reason.HURT);
            return EventResult.PASS;
        });
        PlayerEvents.JOIN.register(HidingServer::tellOfHiders);
        PlayerEvents.CHANGE_DIMENSION.register((player, from, to) -> tellOfHiders(player));
        PlayerEvents.LEAVE.register(player -> stop(player, HidingEvents.Reason.INTERRUPTED));
        ServerLifecycleEvents.STOPPED.register(server -> HIDING.clear());
    }

    /** Whether the player, with what they hold and what they are doing, may hide in a spot they use. Both sides. */
    public static boolean canHide(Player player) {
        return WCISServerConfig.ALLOW_HIDING.get() && player.isSecondaryUseActive() && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()
                && !player.isSleeping() && !player.isPassenger() && !player.isSpectator() && player.isAlive() && !Hiding.isHiding(player);
    }

    public static boolean isHiding(Player player) {
        return HIDING.containsKey(player.getUUID());
    }

    public static @Nullable HidingView view(Player player) {
        Hide hide = HIDING.get(player.getUUID());
        return hide == null ? null : hide.view;
    }

    public static @Nullable HidingSpot spot(ServerPlayer player) {
        Hide hide = HIDING.get(player.getUUID());
        return hide == null ? null : hide.spot;
    }

    public static int hiddenTicks(ServerPlayer player) {
        Hide hide = HIDING.get(player.getUUID());
        return hide == null ? 0 : hide.ticks;
    }

    /** How long a player must stay hidden in their spot, in ticks; 0 when they aren't hiding. */
    public static int hideTicks(ServerPlayer player) {
        Hide hide = HIDING.get(player.getUUID());
        return hide == null ? 0 : hide.hideTicks;
    }

    /** The noise a hiding player makes, 0 to 1, as their client last reported it. */
    public static float noise(ServerPlayer player) {
        Hide hide = HIDING.get(player.getUUID());
        return hide == null ? 0.0F : hide.noise;
    }

    /** Puts a player in a spot at a block, if there is one there. Returns false when they can't hide there. */
    public static boolean start(ServerPlayer player, BlockPos pos) {
        HidingSpotType.Lookup lookup = HidingSpots.find(player.level(), pos, player.level().getBlockState(pos), player);
        return lookup.spot() != null && start(player, lookup.spot());
    }

    /** Puts a player in a spot. Returns false when they can't hide (they already are, or a listener said no). */
    public static boolean start(ServerPlayer player, HidingSpot spot) {
        ServerLevel level = player.level();
        if (isHiding(player)) return false;
        HidingSpotType type = HidingSpots.get(spot.type());
        if (type == null) return false;
        HidingEvents.HideStart event = new HidingEvents.HideStart(player, spot);
        HidingEvents.START.invoke(event);
        if (event.isCancelled()) return false;
        spot = event.spot();
        // A spot holds as many as it has places (two in a double chest or a closet two wide, else one): each hider
        // takes the first place nobody else is in, and with none left, the spot is taken.
        Vec3 place = freePlace(level, spot, player);
        if (place == null) {
            // Taken: by another hider, or by a monster that got in there (one a startle let loose that didn't come out, say).
            boolean monster = BuiltinHidingSpots.places(level, spot, player).stream().anyMatch(at -> monsterIn(level, at));
            player.sendSystemMessage(monster ? Component.translatableWithFallback("gui.whycantisleep.hide.monster_in", "Something's in there.")
                    : Component.translatableWithFallback("gui.whycantisleep.hide.taken", "Someone's already hiding there."), true);
            return false;
        }
        spot = spot.withPosition(place);

        // Now and then something got there first.
        if (level.getRandom().nextInt(100) < WCISServerConfig.CRITTER_CHANCE.get() && startle(level, player, spot)) return true;

        int hideTicks = Math.max(20, Mth.floor(WCISServerConfig.HIDE_TIME.get() * 20 * spot.hideTimeScale() * coverScale(level, spot, type)));
        Hide hide = new Hide(type, spot, level.dimension(), player.position(), hideTicks);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(HUNTER_RANGE), mob -> mob.getTarget() == player && !mob.is(IGNORES_HIDING))) {
            hide.hunters.add(mob.getUUID());
            mob.setTarget(null);
        }
        // Run to ground with something on their heels, they must keep their heart slow (the client's minigame), and
        // the nearest of what chased them comes looking.
        hide.hunted = !hide.hunters.isEmpty() && WCISServerConfig.HEARTBEAT_MINIGAME.get();
        if (!hide.hunters.isEmpty() && WCISServerConfig.HUNTER_SEARCH.get()) hide.search = Search.of(level, player, hide);
        HIDING.put(player.getUUID(), hide);
        player.stopUsingItem();
        player.setDeltaMovement(Vec3.ZERO);
        Vec3 at = spot.position();
        player.teleportTo(at.x, at.y, at.z);
        // Standing in a closet, they face its door, and in a chest its front (their look kept near it by the client);
        // under a bed, the view is theirs to turn.
        if (spot.posture() != HidingPosture.LYING) player.forceSetRotation(spot.yaw(), false, 0.0F, false);
        enterSound(level, spot, player, true);
        broadcast(level, player, hide);
        return true;
    }

    /** Brings a hiding player out where they went in, or beside the spot if that place is taken. Returns false when they weren't hiding. */
    public static boolean stop(ServerPlayer player, HidingEvents.Reason reason) {
        Hide hide = HIDING.remove(player.getUUID());
        if (hide == null) return false;
        ServerLevel level = player.level();
        if (level.dimension() == hide.dimension) {
            Vec3 out = exit(level, player, hide);
            player.teleportTo(out.x, out.y, out.z);
            enterSound(level, hide.spot, player, false);
        }
        broadcastOut(level, player, hide.spot.anchor());
        HidingEvents.STOP.invoke(new HidingEvents.HideStop(player, hide.spot, reason, hide.ticks));
        return true;
    }

    /** The player's client asked to come out (they sneaked). */
    public static void onLeaveRequest(ServerPlayer player) {
        stop(player, HidingEvents.Reason.LEFT);
    }

    /**
     * The player's client reports the noise they make (at its loudest when they gasp, failing the heartbeat minigame).
     * At its loudest, with something to hear it (what chased them in, or a monster near the spot), they are found:
     * pulled out, with all of those after them again at once.
     */
    public static void onNoise(ServerPlayer player, float reported) {
        Hide hide = HIDING.get(player.getUUID());
        if (hide == null) return;
        float noise = Mth.clamp(reported, 0.0F, 1.0F);
        float previous = hide.noise;
        if (noise == previous) return;
        hide.noise = noise;
        HidingEvents.NOISE_CHANGED.invoke(new HidingEvents.NoiseChanged(player, hide.spot, noise, previous));
        if (noise < 1.0F || !WCISServerConfig.NOISE_GIVES_AWAY.get()) return;
        ServerLevel level = player.level();
        // Whatever chased them in hears it at once, wherever it has got to, and so does the nearest monster about the
        // spot: they all turn on them again.
        List<Mob> heard = new ArrayList<>();
        for (UUID id : hide.hunters) {
            if (level.getEntity(id) instanceof Mob hunter && hunter.isAlive() && hunter.distanceToSqr(player) < HUNTER_RANGE * HUNTER_RANGE) heard.add(hunter);
        }
        level.getEntitiesOfClass(Monster.class, new AABB(hide.spot.anchor()).inflate(PROWLER_RANGE), Monster::isAlive).stream()
                .min(Comparator.comparingDouble(monster -> monster.distanceToSqr(player))).filter(monster -> !heard.contains(monster)).ifPresent(heard::add);
        if (heard.isEmpty()) return;
        stop(player, HidingEvents.Reason.HEARD);
        for (Mob mob : heard) {
            mob.setTarget(player);
            mob.getNavigation().moveTo(player, HEARD_SPEED);
            mob.playAmbientSound();
        }
        player.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.hide.heard", "It heard you!"), true);
    }

    private static void tick(ServerPlayer player) {
        Hide hide = HIDING.get(player.getUUID());
        if (hide == null) return;
        ServerLevel level = player.level();
        // The server turning hiding off brings out whoever is hidden; so does the spot being broken, the moment it is
        // (checked every tick, however it went: broken by a player or a mob, blown up, or set by a command).
        if (!WCISServerConfig.ALLOW_HIDING.get() || !player.isAlive() || player.isSleeping() || player.isSpectator() || level.dimension() != hide.dimension
                || !hide.type.stillValid(level, hide.spot, player)) {
            stop(player, HidingEvents.Reason.INTERRUPTED);
            return;
        }
        // They stay put, whatever pushes them.
        player.setDeltaMovement(Vec3.ZERO);
        Vec3 at = hide.spot.position();
        if (player.position().distanceToSqr(at) > 0.01) player.teleportTo(at.x, at.y, at.z);
        Hiding.holdBody(player);
        // Anything that had them in its sights loses them, and is remembered as having been after them.
        if (player.tickCount % 5 == 0) {
            for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(HUNTER_RANGE), mob -> mob.getTarget() == player && !mob.is(IGNORES_HIDING))) {
                hide.hunters.add(mob.getUUID());
                mob.setTarget(null);
            }
        }
        if (hide.search != null && search(level, player, hide)) return;
        // The hide time waits for a search under way to end: what searched gives up only once it has walked off.
        if (hide.search != null && hide.search.phase != Search.Phase.LEAVE && hide.ticks >= hide.hideTicks - 1) return;
        if (++hide.ticks == hide.hideTicks) shakeOff(level, player, hide);
    }

    // The first of a spot's places that no other hider, and no monster, is in; or null when they all are.
    private static @Nullable Vec3 freePlace(ServerLevel level, HidingSpot spot, ServerPlayer player) {
        for (Vec3 place : BuiltinHidingSpots.places(level, spot, player)) {
            boolean taken = monsterIn(level, place);
            for (Map.Entry<UUID, Hide> entry : HIDING.entrySet()) {
                if (taken) break;
                Hide other = entry.getValue();
                if (other.dimension == level.dimension() && other.spot.position().distanceToSqr(place) < PLACE_TAKEN * PLACE_TAKEN) {
                    taken = true;
                    break;
                }
            }
            if (!taken) return place;
        }
        return null;
    }

    // Walls close around the spot hide it better: each side of it closed off by a full block, not counting the spot's
    // own blocks (a bed's other half, a table's), takes some time off.
    private static float coverScale(Level level, HidingSpot spot, HidingSpotType type) {
        if (!WCISServerConfig.COVER.get()) return 1.0F;
        BlockPos at = BlockPos.containing(spot.position());
        int walls = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos next = at.relative(side);
            BlockState state = level.getBlockState(next);
            if (!type.matches(state) && state.isCollisionShapeFullBlock(level, next)) walls++;
        }
        return 1.0F - Math.min(3, walls) * COVER_PER_WALL;
    }

    // Whether a living monster is in a spot's place: its body within the room a hider would take there.
    private static boolean monsterIn(ServerLevel level, Vec3 place) {
        AABB room = new AABB(place.x - PLACE_ROOM, place.y, place.z - PLACE_ROOM, place.x + PLACE_ROOM, place.y + 1.0, place.z + PLACE_ROOM);
        return !level.getEntitiesOfClass(Monster.class, room, Monster::isAlive).isEmpty();
    }

    // A silverfish or cave spider was already in there: it scurries out at them, and they don't get in. It comes out
    // at the spot's mouth (the spot itself may be in a block, as a bed or chest is), leaping at them, after them.
    private static boolean startle(ServerLevel level, ServerPlayer player, HidingSpot spot) {
        boolean spider = level.getRandom().nextBoolean();
        Mob critter = (spider ? EntityTypes.CAVE_SPIDER : EntityTypes.SILVERFISH).create(level, EntitySpawnReason.EVENT);
        if (critter == null) return false;
        Vec3 at = mouth(level, critter, spot.position(), player.position());
        Vec3 toward = player.position().subtract(at);
        float yaw = (float) (Mth.atan2(toward.z, toward.x) * Mth.RAD_TO_DEG) - 90.0F;
        critter.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        Vec3 leap = toward.horizontalDistanceSqr() > 1.0E-4 ? toward.multiply(1.0, 0.0, 1.0).normalize().scale(CRITTER_LEAP) : Vec3.ZERO;
        critter.setDeltaMovement(leap.x, 0.25, leap.z);
        level.addFreshEntity(critter);
        critter.setTarget(player);
        level.playSound(null, spot.anchor(), spider ? SoundEvents.SPIDER_AMBIENT : SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.2F);
        player.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.hide.startled", "Something was already in there!"), true);
        return true;
    }

    /**
     * A tick of the search a hunter makes of a hunted player's spot: after a moment it comes to the spot's front, stops
     * and looks about (now and then at the spot itself), and then either finds them, by a chance the difficulty, the
     * server's search chance and the noise they make decide, and drags them out, or walks away. Returns true when it
     * found them (they are out).
     */
    private static boolean search(ServerLevel level, ServerPlayer player, Hide hide) {
        Search search = hide.search;
        if (!(level.getEntity(search.hunter) instanceof Mob mob) || !mob.isAlive()) {
            hide.search = null;
            return false;
        }
        search.ticks++;
        switch (search.phase) {
            case WAIT -> {
                if (search.ticks >= search.wait) search.next(Search.Phase.APPROACH);
            }
            case APPROACH -> {
                // Asked again and again, so its own idle wandering doesn't lead it off.
                walk(mob, search.front, search.ticks);
                if (mob.position().distanceToSqr(search.front) < SEARCH_REACH * SEARCH_REACH || search.ticks > SEARCH_APPROACH_TICKS) {
                    mob.getNavigation().stop();
                    search.next(Search.Phase.LOOK);
                }
            }
            case LOOK -> {
                mob.getNavigation().stop();
                if (search.ticks % SEARCH_GLANCE == 1) {
                    // A glance about; every other one at the spot itself.
                    boolean atSpot = (search.ticks / SEARCH_GLANCE) % 2 == 1;
                    Vec3 spot = Vec3.atCenterOf(hide.spot.anchor());
                    search.look = atSpot ? spot : mob.getEyePosition().add((level.getRandom().nextDouble() - 0.5) * 8.0, (level.getRandom().nextDouble() - 0.5) * 2.0,
                            (level.getRandom().nextDouble() - 0.5) * 8.0);
                    if (atSpot) mob.playAmbientSound();
                }
                mob.getLookControl().setLookAt(search.look.x, search.look.y, search.look.z);
                if (search.ticks < SEARCH_LOOK_TICKS) return false;
                if (level.getRandom().nextFloat() < findChance(level, hide)) {
                    hide.search = null;
                    stop(player, HidingEvents.Reason.FOUND);
                    mob.setTarget(player);
                    mob.playAmbientSound();
                    player.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.hide.found", "It found you!"), true);
                    return true;
                }
                // Nothing: it walks off the way it came, and on.
                Vec3 off = mob.position().subtract(hide.spot.position()).multiply(1.0, 0.0, 1.0);
                if (off.lengthSqr() < 1.0E-4) off = new Vec3(1.0, 0.0, 0.0);
                search.away = hide.spot.position().add(off.normalize().scale(SEARCH_LEAVE_DISTANCE));
                search.next(Search.Phase.LEAVE);
            }
            case LEAVE -> {
                walk(mob, search.away, search.ticks);
                if (search.ticks > SEARCH_LEAVE_TICKS || mob.position().distanceToSqr(search.away) < SEARCH_REACH * SEARCH_REACH) hide.search = null;
            }
        }
        return false;
    }

    // Keeps a searching mob walking to a place: asked again every few ticks whenever its own idle goals have sent it
    // somewhere else, or it has stopped short.
    private static void walk(Mob mob, Vec3 to, int ticks) {
        if (ticks % SEARCH_REPATH != 1) return;
        BlockPos heading = mob.getNavigation().getTargetPos();
        boolean onTheWay = !mob.getNavigation().isDone() && heading != null && heading.closerToCenterThan(to, SEARCH_REACH + 1.0);
        if (!onTheWay) mob.getNavigation().moveTo(to.x, to.y, to.z, SEARCH_SPEED);
    }

    // The chance a search finds them: the server's search chance, by the difficulty (none on peaceful, half on easy,
    // half as much again on hard), and the noise they make on top.
    private static float findChance(ServerLevel level, Hide hide) {
        float base = WCISServerConfig.SEARCH_CHANCE.get() / 100.0F;
        float difficulty = switch (level.getDifficulty()) {
            case PEACEFUL -> 0.0F;
            case EASY -> 0.5F;
            case NORMAL -> 1.0F;
            case HARD -> 1.5F;
        };
        if (difficulty == 0.0F) return 0.0F;
        return Mth.clamp(base * difficulty + hide.noise * SEARCH_NOISE, 0.0F, 1.0F);
    }

    // Whoever hides in a chest or closet whose block another player just opened is found and comes out.
    private static void discover(ServerPlayer opener, BlockPos pos) {
        ServerLevel level = opener.level();
        List<ServerPlayer> found = new ArrayList<>();
        for (Map.Entry<UUID, Hide> entry : HIDING.entrySet()) {
            Hide hide = entry.getValue();
            if (hide.dimension != level.dimension() || entry.getKey().equals(opener.getUUID())) continue;
            if (!(level.getEntity(entry.getKey()) instanceof ServerPlayer hider)) continue;
            if (openings(level, hide.spot, hider).contains(pos)) found.add(hider);
        }
        for (ServerPlayer hider : found) {
            stop(hider, HidingEvents.Reason.DISCOVERED);
            hider.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.hide.discovered", "Someone opened it and found you!"), true);
            opener.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.hide.found_someone", "You found %s hiding!", hider.getDisplayName()), true);
        }
    }

    // The blocks that open a spot: both halves of a chest, and both halves of each of a closet's doors. Others (a bed,
    // a table) have nothing to open.
    private static List<BlockPos> openings(ServerLevel level, HidingSpot spot, ServerPlayer hider) {
        BlockPos anchor = spot.anchor();
        BlockState state = level.getBlockState(anchor);
        if (spot.type().equals(BuiltinHidingSpots.CHEST) && state.getBlock() instanceof ChestBlock) {
            if (state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return List.of(anchor);
            return List.of(anchor, anchor.relative(ChestBlock.getConnectedDirection(state)));
        }
        if (spot.type().equals(BuiltinHidingSpots.CLOSET)) {
            List<BlockPos> doors = new ArrayList<>();
            for (BlockPos door : BuiltinHidingSpots.closetDoors(level, spot, hider)) {
                doors.add(door);
                doors.add(door.above());
            }
            return doors;
        }
        return List.of();
    }

    // Where something coming out of a spot appears: the first place it fits on the way from the spot to the player.
    private static Vec3 mouth(ServerLevel level, Mob mob, Vec3 spot, Vec3 player) {
        Vec3 way = player.subtract(spot);
        int steps = Math.max(1, Mth.ceil(way.length() / MOUTH_STEP));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = spot.add(way.scale(i / (double) steps));
            if (level.noCollision(mob, mob.getDimensions(Pose.STANDING).makeBoundingBox(at))) return at;
        }
        return player;
    }

    // Out of a chest or a closet, a player comes out right up against the face they came out of: in front of the half
    // of the chest they were in, or of the door before their nook; null for any other spot.
    private static @Nullable Vec3 front(ServerLevel level, ServerPlayer player, HidingSpot spot) {
        BlockState state = level.getBlockState(spot.anchor());
        Vec3 place = spot.position();
        if (spot.type().equals(BuiltinHidingSpots.CHEST) && state.getBlock() instanceof ChestBlock) {
            BlockPos half = BlockPos.containing(place.x, spot.anchor().getY(), place.z);
            return against(level, player, half, state.getValue(ChestBlock.FACING));
        }
        if (spot.type().equals(BuiltinHidingSpots.CLOSET) && state.getBlock() instanceof DoorBlock) {
            Direction facing = state.getValue(DoorBlock.FACING);
            return against(level, player, BlockPos.containing(place).relative(facing), facing);
        }
        return null;
    }

    // Standing against a block's face on one side, by the block's own shape (a chest's narrower box, a shut door's thin
    // panel), so they touch it however far in it is.
    private static Vec3 against(ServerLevel level, ServerPlayer player, BlockPos pos, Direction side) {
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        Direction.Axis axis = side.getAxis();
        double face = shape.isEmpty() ? -0.5
                : side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? shape.max(axis) - 0.5 : 0.5 - shape.min(axis);
        double out = face + player.getBbWidth() / 2.0 + AGAINST_GAP;
        return Vec3.atBottomCenterOf(pos).add(side.getStepX() * out, 0.0, side.getStepZ() * out);
    }

    // Where a player comes out: against the face of a chest or door they hid behind; else where they went in; else the
    // first free place beside the spot.
    private static Vec3 exit(ServerLevel level, ServerPlayer player, Hide hide) {
        Vec3 front = front(level, player, hide.spot);
        if (front != null && fits(level, player, front)) return front;
        if (fits(level, player, hide.exit)) return hide.exit;
        BlockPos anchor = hide.spot.anchor();
        BlockState state = level.getBlockState(anchor);
        if (state.getBlock() instanceof BedBlock) {
            return BedBlock.findStandUpPosition(EntityTypes.PLAYER, level, anchor, state.getValue(BedBlock.FACING), player.getYRot())
                    .orElse(Vec3.atBottomCenterOf(anchor.above()));
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int dy = 0; dy <= 1; dy++) {
                Vec3 out = Vec3.atBottomCenterOf(anchor.relative(side).above(dy));
                if (fits(level, player, out)) return out;
            }
        }
        return Vec3.atBottomCenterOf(anchor.above());
    }

    private static boolean fits(ServerLevel level, ServerPlayer player, Vec3 at) {
        return level.noCollision(player, player.getDimensions(Pose.STANDING).makeBoundingBox(at));
    }

    // Getting in and out is heard: a chest's lid, a closet's door shut behind them, cloth brushed under a bed.
    private static void enterSound(ServerLevel level, HidingSpot spot, ServerPlayer player, boolean in) {
        BlockPos anchor = spot.anchor();
        BlockState state = level.getBlockState(anchor);
        if (spot.posture() == HidingPosture.TUCKED) {
            level.playSound(null, anchor, in ? SoundEvents.CHEST_CLOSE : SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 0.9F);
        } else if (state.getBlock() instanceof DoorBlock) {
            // Shut behind them going in, both of a double door; left as they are coming out.
            if (!in) return;
            for (BlockPos pos : BuiltinHidingSpots.closetDoors(level, spot, player)) {
                BlockState doorState = level.getBlockState(pos);
                if (doorState.getBlock() instanceof DoorBlock door && door.isOpen(doorState)) door.setOpen(player, level, doorState, pos, false);
            }
        } else {
            level.playSound(null, anchor, SoundEvents.WOOL_STEP, SoundSource.PLAYERS, 0.8F, in ? 0.7F : 0.9F);
        }
    }

    // Stayed hidden long enough: what was after them, and what prowls round the spot, gives up and is gone.
    private static void shakeOff(ServerLevel level, ServerPlayer player, Hide hide) {
        // They vanish only if the server lets them, and only if nobody near is out in the open to see it: everyone
        // about has to be hiding too. Else they just lose the player (they lost them going in), and stay.
        if (!WCISServerConfig.HIDING_CLEARS_MONSTERS.get()) {
            player.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.hide.lost_you", "They lost you, but they're still out there."), true);
            return;
        }
        if (someoneOut(level, player, hide)) {
            player.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.hide.someone_out",
                    "They lost you, but they're still out there: everyone nearby must hide too."), true);
            return;
        }
        List<Entity> gone = new ArrayList<>();
        for (UUID id : hide.hunters) {
            Entity hunter = level.getEntity(id);
            if (hunter != null && hunter.isAlive() && hunter.distanceToSqr(player) < HUNTER_RANGE * HUNTER_RANGE && mayVanish(hunter)) gone.add(hunter);
        }
        for (Monster prowler : level.getEntitiesOfClass(Monster.class, new AABB(hide.spot.anchor()).inflate(PROWLER_RANGE), HidingServer::mayVanish)) {
            if (!gone.contains(prowler)) gone.add(prowler);
        }
        HidingEvents.ShakenOff event = new HidingEvents.ShakenOff(player, hide.spot, gone);
        HidingEvents.SHAKEN_OFF.invoke(event);
        for (Entity entity : event.hunters()) {
            if (!entity.isAlive()) continue;
            level.sendParticles(ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() / 2.0, entity.getZ(), 14,
                    entity.getBbWidth() / 2.0, entity.getBbHeight() / 3.0, entity.getBbWidth() / 2.0, 0.02);
            level.playSound(null, entity.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.4F, 0.6F);
            entity.discard();
        }
        // What went is what was after them or about the spot (a mod may have sent its own away in its own way, but it
        // was counted here first); with nothing at all, there was nothing to give up.
        if (gone.isEmpty() && event.hunters().isEmpty()) {
            player.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.hide.nothing", "All quiet. Nothing came looking for you."), true);
        } else {
            player.sendSystemMessage(Component.translatableWithFallback("gui.whycantisleep.hide.safe", "It's quiet now. Whatever was out there is gone."), true);
        }
    }

    // Whether another player near the spot is out in the open: not hiding (spectators don't count).
    private static boolean someoneOut(ServerLevel level, ServerPlayer player, Hide hide) {
        Vec3 spot = hide.spot.position();
        for (ServerPlayer other : level.players()) {
            if (other == player || other.isSpectator() || isHiding(other)) continue;
            if (other.position().distanceToSqr(spot) < OTHERS_RANGE * OTHERS_RANGE) return true;
        }
        return false;
    }

    // A mob that wouldn't despawn on its own (named, tamed, kept) stays, and so do bosses and anything in the tag.
    private static boolean mayVanish(Entity entity) {
        return !entity.is(IGNORES_HIDING) && !(entity instanceof Mob mob && mob.isPersistenceRequired()) && !(entity instanceof Player);
    }

    private static HidingPayload payload(ServerPlayer hider, Hide hide) {
        HidingSpot spot = hide.spot;
        return new HidingPayload(hider.getId(), true, spot.anchor(), spot.posture(), spot.yaw(), spot.viewHeight(), spot.squint(), hide.hideTicks, hide.hunted, spot.icon(), spot.where());
    }

    private static void broadcast(ServerLevel level, ServerPlayer hider, Hide hide) {
        HidingPayload payload = payload(hider, hide);
        for (ServerPlayer player : level.players()) WCISNetwork.sendToPlayer(player, payload);
    }

    private static void broadcastOut(ServerLevel level, ServerPlayer hider, BlockPos anchor) {
        HidingPayload payload = HidingPayload.out(hider.getId(), anchor);
        for (ServerPlayer player : level.players()) WCISNetwork.sendToPlayer(player, payload);
    }

    // A player new to a level is told who in it is hiding, and how, so they are drawn in their spots.
    private static void tellOfHiders(ServerPlayer player) {
        for (ServerPlayer other : player.level().players()) {
            Hide hide = HIDING.get(other.getUUID());
            if (other != player && hide != null) WCISNetwork.sendToPlayer(player, payload(other, hide));
        }
    }

    private static final class Hide {
        private final HidingSpotType type;
        private final HidingSpot spot;
        private final HidingView view;
        private final ResourceKey<Level> dimension;
        private final Vec3 exit;
        private final int hideTicks;
        private final Set<UUID> hunters = new HashSet<>();
        private int ticks;
        private float noise;
        private boolean hunted;
        private @Nullable Search search;

        private Hide(HidingSpotType type, HidingSpot spot, ResourceKey<Level> dimension, Vec3 exit, int hideTicks) {
            this.type = type;
            this.spot = spot;
            this.view = HidingView.of(spot);
            this.dimension = dimension;
            this.exit = exit;
            this.hideTicks = hideTicks;
        }
    }

    /** The search the nearest hunter makes of a hunted player's spot, and where it is in it. */
    private static final class Search {
        // How long the hunter waits before it comes looking, at least and at most.
        private static final int WAIT_MIN = 20;
        private static final int WAIT_MAX = 40;
        private static final double STEP_OUT = 0.9;

        private enum Phase {
            WAIT,
            APPROACH,
            LOOK,
            LEAVE
        }

        private final UUID hunter;
        private final Vec3 front;
        private final int wait;
        private Phase phase = Phase.WAIT;
        private int ticks;
        private Vec3 look = Vec3.ZERO;
        private Vec3 away = Vec3.ZERO;

        private Search(UUID hunter, Vec3 front, int wait) {
            this.hunter = hunter;
            this.front = front;
            this.wait = wait;
        }

        // The nearest hunter, coming to the spot's front: for a chest or closet the face hidden behind, else a little
        // way from the spot towards where the player went in. Null when no hunter is a mob about.
        private static @Nullable Search of(ServerLevel level, ServerPlayer player, Hide hide) {
            Mob nearest = null;
            for (UUID id : hide.hunters) {
                if (level.getEntity(id) instanceof Mob mob && mob.isAlive() && (nearest == null || mob.distanceToSqr(player) < nearest.distanceToSqr(player))) nearest = mob;
            }
            if (nearest == null) return null;
            Vec3 front = HidingServer.front(level, player, hide.spot);
            if (front == null) {
                Vec3 out = hide.exit.subtract(hide.spot.position()).multiply(1.0, 0.0, 1.0);
                front = out.lengthSqr() < 1.0E-4 ? hide.exit : hide.spot.position().add(out.normalize().scale(1.6));
            }
            // It stands a step out from there: a mob can't stand in a shut door's block, nor right up against a chest.
            Vec3 out = front.subtract(hide.spot.position()).multiply(1.0, 0.0, 1.0);
            if (out.lengthSqr() > 1.0E-4) front = front.add(out.normalize().scale(STEP_OUT));
            return new Search(nearest.getUUID(), front, WAIT_MIN + level.getRandom().nextInt(WAIT_MAX - WAIT_MIN));
        }

        private void next(Phase phase) {
            this.phase = phase;
            this.ticks = 0;
        }
    }
}
