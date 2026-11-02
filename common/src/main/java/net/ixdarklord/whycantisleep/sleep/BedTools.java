package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.whycantisleep.compat.WCISItems;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * What a player may read of a bed with the bed check, on either side: how a night in it would go with a Dreamcatcher
 * (in either hand, or in an item frame near the bed), the dark spots of its room with a Bedside Lantern (in either
 * hand). With the server's {@code bedCheckNeedsTools} off, both need nothing.
 */
public final class BedTools {
    // How near the bed a Dreamcatcher hung in an item frame counts: the room round it.
    private static final double FRAME_RANGE = 6.0;

    private BedTools() {
    }

    /** Whether the player may read how a night in this bed (its head) would go. */
    public static boolean canForecast(Player player, BlockPos head) {
        if (!WCISServerConfig.BED_CHECK_NEEDS_TOOLS.get()) return true;
        return holding(player, WCISItems.DREAMCATCHER.get()) || hungNear(player.level(), head);
    }

    /** Whether the player may see where in a bed's room a monster could spawn. */
    public static boolean canSeeDarkSpots(Player player) {
        return !WCISServerConfig.BED_CHECK_NEEDS_TOOLS.get() || holding(player, WCISItems.BEDSIDE_LANTERN.get());
    }

    /** Whether the player can read anything of this bed at all. */
    public static boolean canCheck(Player player, BlockPos head) {
        return canSeeDarkSpots(player) || canForecast(player, head);
    }

    private static boolean holding(Player player, Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    // A Dreamcatcher in an item frame near the bed: it watches over the bed for whoever looks at it.
    private static boolean hungNear(Level level, BlockPos head) {
        return !level.getEntitiesOfClass(ItemFrame.class, new AABB(head).inflate(FRAME_RANGE),
                frame -> frame.getItem().is(WCISItems.DREAMCATCHER.get())).isEmpty();
    }
}
