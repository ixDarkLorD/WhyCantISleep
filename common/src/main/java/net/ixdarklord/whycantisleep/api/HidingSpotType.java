package net.ixdarklord.whycantisleep.api;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A kind of hiding spot: what a player can crawl under, climb into or stand behind, by sneaking and using it with both
 * hands empty. Register one with {@link HidingSpots#register}; or, with no code, describe one in a data pack (see
 * {@link HidingSpots}).
 * <pre>{@code
 * HidingSpots.register(new HidingSpotType() {
 *     public Identifier id() { return MyMod.id("wardrobe"); }
 *     public boolean matches(BlockState state) { return state.is(MyBlocks.WARDROBE); }
 *     public Lookup find(Level level, BlockPos pos, BlockState state, Player player) {
 *         Direction facing = state.getValue(WardrobeBlock.FACING);
 *         return Lookup.found(new HidingSpot(id(), pos, Vec3.atBottomCenterOf(pos), facing.toYRot(), HidingPosture.STANDING, -1.0F,
 *                 HidingSpot.DEFAULT_SQUINT, 0.8F, new ItemStack(MyBlocks.WARDROBE), Component.translatable("mymod.hiding.wardrobe")));
 *     }
 * });
 * }</pre>
 * {@link #matches} runs on both sides (it keeps the client from predicting the block's own use instead); {@link #find}
 * and {@link #stillValid} run on the server.
 */
public interface HidingSpotType {
    Identifier id();

    /** Whether this type is about the block at all: a cheap check by block, before {@link #find}. */
    boolean matches(BlockState state);

    /**
     * The spot at a block the player sneak-used, or a reason they can't hide there ("there's no floor under this bed"),
     * or {@link Lookup#NONE} to let the block be used as it would be.
     */
    Lookup find(Level level, BlockPos pos, BlockState state, Player player);

    /** Whether a spot a player hides in still stands, checked each tick; when it doesn't, they are brought out. */
    default boolean stillValid(Level level, HidingSpot spot, Player player) {
        BlockState state = level.getBlockState(spot.anchor());
        return this.matches(state) && this.find(level, spot.anchor(), state, player).spot() != null;
    }

    /** What {@link #find} found: a spot, a refusal to show the player, or neither. */
    record Lookup(@Nullable HidingSpot spot, @Nullable Component refusal) {
        public static final Lookup NONE = new Lookup(null, null);

        public static Lookup found(HidingSpot spot) {
            return new Lookup(spot, null);
        }

        public static Lookup refused(Component reason) {
            return new Lookup(null, reason);
        }
    }
}
