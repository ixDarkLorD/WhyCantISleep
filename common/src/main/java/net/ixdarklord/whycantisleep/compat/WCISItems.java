package net.ixdarklord.whycantisleep.compat;

import net.ixdarklord.coolcatcore.api.registry.DeferredRegister;
import net.ixdarklord.coolcatcore.api.registry.RegistryEntry;
import net.ixdarklord.folios.api.GuideBookItem;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * The mod's one item: its guide book, a Folios {@link GuideBookItem} that always opens {@link GuideBookGift#BOOK},
 * named "Insomniac's Handbook". Its stacks are given with the book in their {@code folios:guide_book} component too
 * ({@link GuideBookGift#stack}), which Folios draws the cover from.
 */
public final class WCISItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(WhyCantISleep.MOD_ID, Registries.ITEM);

    public static final RegistryEntry<GuideBookItem> GUIDE_BOOK = ITEMS.register("guide_book",
            () -> new Handbook(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    private WCISItems() {
    }

    public static void register() {
        ITEMS.register();
    }

    /**
     * The guide book as an item of its own name, "Insomniac's Handbook", rather than the book's title Folios gives its
     * books (a custom name on the stack still wins).
     */
    private static final class Handbook extends GuideBookItem {
        private Handbook(Properties properties) {
            super(properties, GuideBookGift.BOOK);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatable(this.getDescriptionId());
        }
    }
}
