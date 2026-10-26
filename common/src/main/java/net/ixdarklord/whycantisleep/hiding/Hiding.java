package net.ixdarklord.whycantisleep.hiding;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.IntFunction;

/**
 * Who is hiding, and how, asked from code that runs on both sides (the mixins): the server's own record, or on a
 * client what the server told it.
 */
public final class Hiding {
    /** Set on clients ({@code HidingClient}): how the player with this network id hides, as last told, or null. */
    public static IntFunction<@Nullable HidingView> onClient = id -> null;
    /**
     * The heartbeat minigame's overtime: a player hiding hunted who missed the last beat when the hide time is up must
     * catch one more, and gets OVERTIME_TICKS more for it (a beat's crossing and the longest gap before it), at most
     * MAX_OVERTIME times.
     */
    public static final int OVERTIME_TICKS = 64;
    public static final int MAX_OVERTIME = 2;

    private Hiding() {
    }

    public static boolean isHiding(Player player) {
        return view(player) != null;
    }

    /** How a player hides, or null when they don't. */
    public static @Nullable HidingView view(Player player) {
        return player.level().isClientSide() ? onClient.apply(player.getId()) : HidingServer.view(player);
    }

    /**
     * Whether a bed has a floor under it to lie on: a solid top on the block under each of its halves. A bed over
     * air has nothing under it to hide on.
     *
     * @param head the bed's head
     */
    public static boolean hasFloor(Level level, BlockPos head) {
        BlockState state = level.getBlockState(head);
        if (!(state.getBlock() instanceof BedBlock)) return false;
        BlockPos foot = head.relative(state.getValue(BedBlock.FACING).getOpposite());
        return level.getBlockState(head.below()).isFaceSturdy(level, head.below(), Direction.UP)
                && level.getBlockState(foot.below()).isFaceSturdy(level, foot.below(), Direction.UP);
    }

    /**
     * Whether a hiding player is out of sight of something looking for a target: anything but bosses and the like
     * ({@code #whycantisleep:ignores_hiding}), which find a hiding player as easily as anywhere.
     *
     * @param looker what is looking, or null when the game doesn't say
     */
    public static boolean hiddenFrom(@Nullable LivingEntity looker, LivingEntity target) {
        return target instanceof Player player && isHiding(player) && (looker == null || !looker.is(HidingServer.IGNORES_HIDING));
    }

    /**
     * Holds a hiding player's body still, facing the way the spot has them (along a bed with their head to its pillow,
     * towards a closet's door), whichever way they look: left to the game, the body turns after the head, and so swung
     * round under the frame. Called after each of their ticks, on both sides; the old and new rotations are both set so
     * nothing is interpolated between them.
     */
    public static void holdBody(Player player) {
        HidingView view = view(player);
        if (view == null) return;
        player.yBodyRot = view.yaw();
        player.yBodyRotO = view.yaw();
    }
}
