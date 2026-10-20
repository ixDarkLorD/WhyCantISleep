package net.ixdarklord.whycantisleep.between;

import net.ixdarklord.coolcatcore.api.event.v2.common.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Dream Glider: a kite of moth-wing cloth on a frame of sleepwood, for crossing between the Dark Between's islands.
 * Held (in either hand) while falling, its holder glides: the fall slowed to a drift and carried forward the way they
 * look, and no harm when they land. It wears with use, a point for each second of gliding.
 * <p>
 * The glide itself is moved on the client (the player's own movement is the client's, {@link #glide}, called each
 * client tick by {@code DreamGliderClient}); the server keeps its holder from fall damage and wears the glider down.
 */
public final class DreamGlider {
    /** How fast it lets its holder fall at most, the push it gives along their look each tick, and the speed it keeps them under. */
    private static final double SINK = 0.08, PUSH = 0.04, TOP_SPEED = 0.55;

    private DreamGlider() {
    }

    public static void init() {
        ServerTickEvents.END_LEVEL.register(DreamGlider::tick);
    }

    /** The glider its holder holds, or null. */
    public static @Nullable InteractionHand held(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(WCISItems.DREAM_GLIDER.get())) return hand;
        }
        return null;
    }

    /** Whether its holder is gliding now: holding it, in the air, falling, not flying nor swimming nor on a ladder. */
    public static boolean gliding(Player player) {
        return held(player) != null && !player.onGround() && !player.isInWater() && !player.getAbilities().flying
                && !player.onClimbable() && !player.isPassenger() && !player.isFallFlying();
    }

    /** One tick of the glide (on the client, for the local player): slow the fall, carry them forward. */
    public static void glide(Player player) {
        if (!gliding(player)) return;
        Vec3 v = player.getDeltaMovement();
        if (v.y >= 0.0) return;
        Vec3 look = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (look.lengthSqr() > 1.0E-4) look = look.normalize();
        double x = v.x * 0.97 + look.x * PUSH, z = v.z * 0.97 + look.z * PUSH;
        double speed = Math.sqrt(x * x + z * z);
        if (speed > TOP_SPEED) {
            x *= TOP_SPEED / speed;
            z *= TOP_SPEED / speed;
        }
        player.setDeltaMovement(x, Math.max(v.y, -SINK), z);
        player.resetFallDistance();
    }

    private static void tick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (!gliding(player)) continue;
            player.resetFallDistance();                                 // it lands its holder gently
            if (player.tickCount % 20 == 0 && !player.isCreative()) {
                InteractionHand hand = held(player);
                ItemStack glider = player.getItemInHand(hand);
                glider.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
        }
    }
}
