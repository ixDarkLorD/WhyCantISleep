package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Things by a bed that change how well it is slept in: soothing ones (flowers, amethyst, candles) help, unsettling ones
 * (skulls, cobwebs, sculk) keep a sleeper up. Each placed within {@link #RANGE} blocks of the bed counts, up to a most;
 * a flower in a pot counts as the flower. Which items are which are item tags, so a data pack can change them, and the
 * tooltips of those items say so.
 */
public final class SleepDecor {
    /** Items that soothe a sleeper when placed by the bed. */
    public static final TagKey<Item> SOOTHING = TagKey.create(Registries.ITEM, WhyCantISleep.id("soothing"));
    /** Items that unsettle a sleeper when placed by the bed. */
    public static final TagKey<Item> UNSETTLING = TagKey.create(Registries.ITEM, WhyCantISleep.id("unsettling"));
    /** How far round the bed they count, across and up and down. */
    public static final int RANGE = 6;
    public static final int VERTICAL_RANGE = 3;
    /** What each is worth, and the most all of them together are worth. */
    public static final int SOOTHING_EACH = 2;
    public static final int SOOTHING_MOST = 10;
    public static final int UNSETTLING_EACH = -3;
    public static final int UNSETTLING_MOST = -15;

    private SleepDecor() {
    }

    /** What one of an item is worth by a bed: positive soothing, negative unsettling, 0 for neither. Unsettling wins. */
    public static int points(ItemStack stack) {
        if (stack.is(UNSETTLING)) return UNSETTLING_EACH;
        if (stack.is(SOOTHING)) return SOOTHING_EACH;
        return 0;
    }

    /** How many soothing and unsettling things are placed round a bed. */
    public static Count count(Level level, BlockPos bed) {
        int soothing = 0;
        int unsettling = 0;
        for (BlockPos pos : BlockPos.betweenClosed(bed.offset(-RANGE, -VERTICAL_RANGE, -RANGE), bed.offset(RANGE, VERTICAL_RANGE, RANGE))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            int points = points(item(state));
            if (points > 0) soothing++;
            else if (points < 0) unsettling++;
        }
        return new Count(soothing, unsettling);
    }

    // The item a placed block stands for: a potted plant's own, else the block's.
    private static ItemStack item(BlockState state) {
        if (state.getBlock() instanceof FlowerPotBlock pot) return new ItemStack(pot.getPotted().asItem());
        return new ItemStack(state.getBlock().asItem());
    }

    /** Soothing and unsettling things round a bed. */
    public record Count(int soothing, int unsettling) {
        public static final Count NONE = new Count(0, 0);

        public int soothingPoints() {
            return Math.min(SOOTHING_MOST, this.soothing * SOOTHING_EACH);
        }

        public int unsettlingPoints() {
            return Math.max(UNSETTLING_MOST, this.unsettling * UNSETTLING_EACH);
        }
    }
}
