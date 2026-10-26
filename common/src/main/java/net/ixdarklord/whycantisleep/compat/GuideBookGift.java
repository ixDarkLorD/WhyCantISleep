package net.ixdarklord.whycantisleep.compat;

import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.folios.api.FoliosBooks;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * The mod's guide book, a Folios book ({@code assets/whycantisleep/folios/guide/}) on the mod's own item
 * ({@link WCISItems#GUIDE_BOOK}), given to each player the first time they join. Who has had one is kept as a tag on
 * the player.
 */
public final class GuideBookGift {
    /** The guide book's id. */
    public static final ResourceLocation BOOK = WhyCantISleep.id("guide");
    private static final String GIVEN = "whycantisleep.guide_given";

    private GuideBookGift() {
    }

    public static void init() {
        PlayerEvents.JOIN.register(player -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !WCISServerConfig.GIVE_GUIDE_BOOK.get()) return;
            if (serverPlayer.getTags().contains(GIVEN)) return;
            give(serverPlayer);
            serverPlayer.addTag(GIVEN);
        });
    }

    /** A copy of the guide: the mod's item, naming its book for the cover Folios draws. */
    public static ItemStack stack() {
        return FoliosBooks.createStack(WCISItems.GUIDE_BOOK.get(), BOOK);
    }

    /** Puts a copy of the guide in a player's inventory, or at their feet when it is full. */
    public static void give(ServerPlayer player) {
        ItemStack book = stack();
        if (!player.getInventory().add(book)) player.drop(book, false);
    }
}
