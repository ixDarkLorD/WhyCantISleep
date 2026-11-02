package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.whycantisleep.compat.WCISItems;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What a player may read of a bed with the bed check, on either side: how a night in it would go with a Dreamcatcher,
 * the dark spots of its room with a Bedside Lantern, each in either hand. With the server's {@code bedCheckNeedsTools} off, both need nothing. A bed is checked by sneaking and using it
 * with either tool in hand ({@link #checksByClick}).
 */
public final class BedTools {
    private BedTools() {
    }

    /** Whether the player may read how a night in this bed (its head) would go. */
    public static boolean canForecast(Player player, BlockPos head) {
        if (!WCISServerConfig.BED_CHECK_NEEDS_TOOLS.get()) return true;
        return holding(player, WCISItems.DREAMCATCHER.get());
    }

    /** Whether the player may see where in a bed's room a monster could spawn. */
    public static boolean canSeeDarkSpots(Player player) {
        return !WCISServerConfig.BED_CHECK_NEEDS_TOOLS.get() || holding(player, WCISItems.BEDSIDE_LANTERN.get());
    }

    /** Whether the player can read anything of this bed at all. */
    public static boolean canCheck(Player player, BlockPos head) {
        return canSeeDarkSpots(player) || canForecast(player, head);
    }

    /**
     * Whether using a bed now checks it: sneaking, with a Dreamcatcher or a Bedside Lantern in the main hand, or in the off
     * hand with the main hand empty (what the main hand holds is what a use goes by). The only way a bed is checked.
     */
    public static boolean checksByClick(Player player) {
        if (!player.isSecondaryUseActive()) return false;
        ItemStack main = player.getMainHandItem();
        return isTool(main) || main.isEmpty() && isTool(player.getOffhandItem());
    }

    private static boolean isTool(ItemStack stack) {
        return stack.is(WCISItems.DREAMCATCHER.get()) || stack.is(WCISItems.BEDSIDE_LANTERN.get());
    }

    private static boolean holding(Player player, Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }
}
