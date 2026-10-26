package net.ixdarklord.whycantisleep.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.ixdarklord.coolcatcanvas.api.utils.Easing;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.whycantisleep.api.HidingPosture;
import net.ixdarklord.whycantisleep.client.hud.Explanations;
import net.ixdarklord.whycantisleep.client.hud.SleepNotice;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.hiding.Hiding;
import net.ixdarklord.whycantisleep.hiding.HidingView;
import net.ixdarklord.whycantisleep.network.HidingPayload;
import net.ixdarklord.whycantisleep.network.LeaveHidingPayload;
import net.ixdarklord.whycantisleep.network.NoisePayload;
import net.ixdarklord.whycantisleep.network.OvertimePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The client's side of hiding: who is hiding and how (the server says, for every player in the level, so they are
 * drawn in their spots) and who of them trembles with monsters about; and for the local player the view from the
 * spot, eyes narrowed to a squint, a card saying whether anything is out there, the heartbeat to keep slow if they ran
 * in hunted ({@link HidingMinigame}), and sneaking to come out. A chest someone hides in has its lid raised a crack.
 */
public final class HidingClient {
    // A card that stays up for as long as the player hides; it is taken down when they come out.
    private static final int CARD_TICKS = 20 * 60 * 60;
    // How near a monster must be for a hiding player to tremble, and for their card to warn; and how often to look.
    private static final double DANGER_RANGE = 12.0;
    private static final int DANGER_CHECK_TICKS = 10;
    private static final Int2ObjectMap<HidingView> HIDING = new Int2ObjectOpenHashMap<>();
    private static final IntSet TREMBLING = new IntOpenHashSet();
    // How long the eyes take to narrow to the spot's squint going in, and to open coming out.
    private static final float SQUINT_TICKS = 14.0F;
    // A chest's lid with someone in it: raised this far (of fully open: the game eases it, so about 21 degrees, room
    // for the eyes and the face under them), over LID_TICKS, and let down again as they climb out.
    static final float LID_AJAR = 0.085F;
    private static final float LID_TICKS = 8.0F;
    // How far the view turns peering out of a chest: either side of its front, up, and down.
    private static final float PEEK_YAW = 50.0F;
    private static final float PEEK_UP = 25.0F;
    private static final float PEEK_DOWN = 35.0F;
    private static final Map<BlockPos, Lid> LIDS = new HashMap<>();
    // Where each player crouched in a chest went in, for their eyes to rise into its gap as the lid lifts.
    private static final Int2ObjectMap<BlockPos> TUCKED_AT = new Int2ObjectOpenHashMap<>();
    // Sneaking took them in, so the key must be let go before pressing it again counts as coming out.
    private static boolean sneakReleased;
    private static boolean sneakWasDown;
    private static long tick;
    // How much of the crosshair shows (it fades out over CROSSHAIR_TICKS while hiding): now, and a tick ago.
    private static final float CROSSHAIR_TICKS = 10.0F;
    private static float crosshair = 1.0F;
    private static float crosshairO = 1.0F;
    private static long hidAt;
    // The noise the local player makes while hidden, 0 to 1: trying to walk or jump, or swinging the view about,
    // raises it; keeping still lets it fall. With monsters near, past RESTLESS_FROM the card says to hold still, and it
    // stays saying so until the noise is down to CALM_BELOW. At 1, with a monster near, the server has them found.
    // While the heartbeat minigame runs, their moves make none: it is their heart that can give them away then.
    private static final float NOISE_PER_KEY = 0.05F;
    private static final float NOISE_PER_DEGREE = 0.0025F;
    private static final float NOISE_MAX_LOOK = 0.06F;
    private static final float NOISE_FALL = 0.02F;
    private static final float RESTLESS_FROM = 0.12F;
    private static final float CALM_BELOW = 0.04F;
    // The local player's card while hiding: what it shows, where it says they are, and which card it is.
    private static ItemStack icon = ItemStack.EMPTY;
    private static Component where = Component.empty();
    private static int hideTicks = 1;
    private static Card card = Card.QUIET;
    private static float noise;
    private static float sentNoise;
    private static float lastYaw;
    private static float lastPitch;

    private HidingClient() {
    }

    private enum Card {
        QUIET,
        DANGER,
        RESTLESS
    }

    public static void init() {
        Hiding.onClient = HIDING::get;
        ClientTickEvents.END.register(HidingClient::tick);
        // Hidden, a left click is the heartbeat minigame's, never an attack: taken before the game handles its keys.
        ClientTickEvents.START.register(minecraft -> {
            if (!isLocalHiding()) return;
            boolean clicked = false;
            while (minecraft.options.keyAttack.consumeClick()) clicked = true;
            if (clicked) HidingMinigame.click();
        });
        ClientPlayerEvents.LEAVE.register(player -> {
            HIDING.clear();
            TREMBLING.clear();
            LIDS.clear();
            TUCKED_AT.clear();
        });
        HidingAmbience.init();
    }

    /** Whether the local player is hiding. */
    public static boolean isLocalHiding() {
        return localView() != null;
    }

    /** How much of the crosshair shows, 0 to 1: it fades out while the local player hides. */
    public static float crosshair(float partialTicks) {
        return Mth.lerp(partialTicks, crosshairO, crosshair);
    }

    /** How the local player hides, or null when they don't. */
    public static @Nullable HidingView localView() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null : HIDING.get(minecraft.player.getId());
    }

    /** The chest the local player is crouched in, or null when they aren't in one. */
    public static @Nullable BlockPos localChest() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? null : TUCKED_AT.get(minecraft.player.getId());
    }

    /**
     * Peering out of a chest's gap, the local player can only turn so far: PEEK_YAW either side of the way the chest
     * faces, PEEK_UP up and PEEK_DOWN down. Called after each turn of the view ({@code EntityMixin}).
     */
    public static void limitLook(Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity != minecraft.player) return;
        HidingView view = localView();
        if (view == null || view.posture() != HidingPosture.TUCKED) return;
        float off = Mth.wrapDegrees(entity.getYRot() - view.yaw());
        float yaw = view.yaw() + Mth.clamp(off, -PEEK_YAW, PEEK_YAW);
        // The turn is kept relative to where the view was, so it stays continuous (no snap through 360).
        float shift = yaw - (view.yaw() + off);
        entity.setYRot(entity.getYRot() + shift);
        entity.yRotO += shift;
        entity.setXRot(Mth.clamp(entity.getXRot(), -PEEK_UP, PEEK_DOWN));
        entity.xRotO = Mth.clamp(entity.xRotO, -PEEK_UP, PEEK_DOWN);
    }

    /** Whether the hiding player with this network id has monsters near enough to tremble at. */
    public static boolean isTrembling(int playerId) {
        return TREMBLING.contains(playerId);
    }

    /**
     * How far the lid of the chest at a position is raised by someone hiding in it, as the game counts a lid's
     * openness (0 shut, 1 open); 0 for any other. Read by the chest's own openness ({@code ChestBlockEntityMixin}).
     */
    public static float lidAjar(BlockPos pos, float partialTicks) {
        Lid lid = LIDS.get(pos);
        if (lid == null) return 0.0F;
        float progress = Mth.clamp((tick - lid.since + partialTicks) / LID_TICKS, 0.0F, 1.0F);
        return LID_AJAR * (lid.raising ? Easing.QUAD_OUT.apply(progress) : 1.0F - Easing.QUAD_IN.apply(progress));
    }

    /**
     * How far the eyes of a player crouched in a chest have risen into the gap under its lid, 0 (still down inside it)
     * to 1, following the lid as it lifts; 0 for anyone else.
     */
    public static float peekRise(int playerId, float partialTicks) {
        BlockPos at = TUCKED_AT.get(playerId);
        return at == null ? 0.0F : lidAjar(at, partialTicks) / LID_AJAR;
    }

    public static void on(HidingPayload payload) {
        HidingView before = HIDING.get(payload.player());
        if (payload.hiding()) {
            HidingView view = new HidingView(payload.posture(), payload.yaw(), payload.eyeHeight(), payload.squint());
            HIDING.put(payload.player(), view);
            if (view.posture() == HidingPosture.TUCKED) {
                LIDS.put(payload.anchor().immutable(), new Lid(tick, true));
                TUCKED_AT.put(payload.player(), payload.anchor().immutable());
            }
        } else {
            HIDING.remove(payload.player());
            TREMBLING.remove(payload.player());
            TUCKED_AT.remove(payload.player());
            // The lid comes down once nobody is left in the chest (two can hide in a double one).
            if (before != null && before.posture() == HidingPosture.TUCKED && !TUCKED_AT.containsValue(payload.anchor())) {
                LIDS.put(payload.anchor().immutable(), new Lid(tick, false));
            }
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || payload.player() != minecraft.player.getId()) return;
        // The eyes narrow to the spot's squint going in, and open coming out, from wherever the lids are.
        if (payload.hiding()) Eyelids.ease(payload.squint(), SQUINT_TICKS, Eyelids.Mood.ALERT);
        else Eyelids.ease(0.0F, SQUINT_TICKS, Eyelids.Mood.STILL);
        if (payload.hiding()) {
            // Whatever speed they came in with stops here: the server holds them still, and so must the client.
            minecraft.player.setDeltaMovement(Vec3.ZERO);
            minecraft.player.setSprinting(false);
            sneakReleased = false;
            hidAt = tick;
            icon = payload.icon();
            where = payload.where();
            hideTicks = Math.max(1, payload.hideTicks());
            noise = 0.0F;
            sentNoise = 0.0F;
            lastYaw = minecraft.player.getYRot();
            lastPitch = minecraft.player.getXRot();
            card = dangerNear(minecraft.player) ? Card.DANGER : Card.QUIET;
            SleepNotices.show(card(card), CARD_TICKS);
            if (payload.hunted()) HidingMinigame.begin();
        } else {
            HidingMinigame.end(false);
            SleepNotices.clear(SleepNotice.Kind.HIDING);
            SleepNotices.clear(SleepNotice.Kind.HIDING_DANGER);
        }
    }

    // The hiding card: all quiet; a warning that something is out there; moving about while it is, a call to hold
    // still. It shows what they hide in. All quiet, it has no timer; with something out there, its timer runs down the
    // time left to hold out until it gives up; and on the call to hold still, it is the noise being made.
    private static SleepNotice card(Card shown) {
        SleepNotice notice = switch (shown) {
            case QUIET -> Explanations.notice(SleepNotice.Kind.HIDING, "hiding", where);
            case DANGER -> Explanations.notice(SleepNotice.Kind.HIDING_DANGER, "hiding_danger").withProgress(() -> 1.0 - hideProgress());
            case RESTLESS -> Explanations.notice(SleepNotice.Kind.HIDING_DANGER, "hiding_restless").withProgress(() -> noise);
        };
        return icon.isEmpty() ? notice : notice.withIcon(icon);
    }

    // The noise made this tick: a walking or jumping key held (their moves are blocked, but the try is heard), and how
    // far the view swung.
    private static void listen(Minecraft minecraft) {
        var options = minecraft.options;
        boolean trying = options.keyUp.isDown() || options.keyDown.isDown() || options.keyLeft.isDown() || options.keyRight.isDown() || options.keyJump.isDown();
        float yaw = minecraft.player.getYRot();
        float pitch = minecraft.player.getXRot();
        float swing = Math.abs(Mth.wrapDegrees(yaw - lastYaw)) + Math.abs(pitch - lastPitch);
        lastYaw = yaw;
        lastPitch = pitch;

        float made = (trying ? NOISE_PER_KEY : 0.0F) + Math.min(NOISE_MAX_LOOK, swing * NOISE_PER_DEGREE);
        // While the heartbeat minigame runs it is their heart that gives them away, not their moves.
        if (HidingMinigame.running()) made = 0.0F;
        noise = Mth.clamp(noise + made - NOISE_FALL, 0.0F, 1.0F);
        // The server hears of it in tenths, when that changes.
        float rounded = Math.round(noise * 10.0F) / 10.0F;
        if (rounded != sentNoise) {
            sentNoise = rounded;
            Network.sendToServer(new NoisePayload(rounded));
        }
    }

    // How far through the hide time the local player is, 0 to 1.
    private static double hideProgress() {
        return Math.min(1.0, (tick - hidAt) / (double) hideTicks);
    }

    // Whether any monster is near a player: what a client can see, which is what a hiding player would hear.
    private static boolean dangerNear(Player player) {
        return !player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(DANGER_RANGE), Monster::isAlive).isEmpty();
    }

    private static void tick(Minecraft minecraft) {
        // Paused, hiding stands still with the world: the hide time, the card, the noise and the heartbeat all wait (the
        // client ticks on behind the pause screen; in singleplayer the server waits too).
        if (minecraft.isPaused()) return;
        tick++;
        // The crosshair fades out while hiding, and back in coming out.
        crosshairO = crosshair;
        crosshair = Mth.approach(crosshair, isLocalHiding() ? 0.0F : 1.0F, 1.0F / CROSSHAIR_TICKS);
        LIDS.values().removeIf(lid -> !lid.raising && tick - lid.since > LID_TICKS);
        if (minecraft.level != null && !HIDING.isEmpty()) {
            boolean check = tick % DANGER_CHECK_TICKS == 0;
            for (Player player : minecraft.level.players()) {
                if (!HIDING.containsKey(player.getId())) continue;
                // Every hiding player this client can see is held still in their spot (after the level's tick has
                // turned them), and trembles while monsters are about.
                Hiding.holdBody(player);
                if (!check) continue;
                if (dangerNear(player)) TREMBLING.add(player.getId());
                else TREMBLING.remove(player.getId());
            }
        }
        // The eyes squint tighter with monsters about.
        Eyelids.tense(isLocalHiding() && minecraft.player != null && TREMBLING.contains(minecraft.player.getId()));
        if (!isLocalHiding()) return;
        // The card follows what is out there, and what the player is doing about it, without starting over.
        listen(minecraft);
        boolean danger = TREMBLING.contains(minecraft.player.getId());
        // Hunted in, they keep their heart slow until the hide time is up, and a beat longer if they missed the last
        // (overtime, at most Hiding.MAX_OVERTIME beats, each its own time); a beat caught in overtime ends it at once.
        // The server hears which. At the third strike they gasp.
        if (HidingMinigame.running() && (hideProgress() >= 1.0 || HidingMinigame.settled())) {
            boolean more = !HidingMinigame.settled() && HidingMinigame.overtime(danger);
            if (more) hideTicks += Hiding.OVERTIME_TICKS;
            else HidingMinigame.end(true);
            Network.sendToServer(new OvertimePayload(more));
        }
        if (HidingMinigame.tick(minecraft, danger)) {
            noise = 1.0F;
            sentNoise = 1.0F;
            Network.sendToServer(new NoisePayload(1.0F));
        }
        Card next = !danger ? Card.QUIET
                : card == Card.RESTLESS ? (noise > CALM_BELOW ? Card.RESTLESS : Card.DANGER)
                : (noise > RESTLESS_FROM ? Card.RESTLESS : Card.DANGER);
        SleepNotice.Kind shown = SleepNotices.shownKind();
        if (shown == null) {
            // Another card (the monsters giving up, say) took its place and has gone: the hiding card comes back.
            card = next;
            SleepNotices.showQuietly(card(next), CARD_TICKS);
        } else if (next != card && (shown == SleepNotice.Kind.HIDING || shown == SleepNotice.Kind.HIDING_DANGER)) {
            card = next;
            SleepNotices.replace(card(next));
        }
        boolean down = minecraft.options.keyShift.isDown();
        if (!down) sneakReleased = true;
        else if (sneakReleased && !sneakWasDown) {
            sneakReleased = false;
            Network.sendToServer(new LeaveHidingPayload(true));
        }
        sneakWasDown = down;
    }

    // A chest lid raised, or being let down, and since when.
    private record Lid(long since, boolean raising) {
    }
}
