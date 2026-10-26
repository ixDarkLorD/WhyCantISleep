package net.ixdarklord.whycantisleep.compat;

import net.ixdarklord.coolcatcore.api.registry.CreativeTabs;
import net.ixdarklord.coolcatcore.api.registry.DeferredRegister;
import net.ixdarklord.coolcatcore.api.registry.RegistryEntry;
import net.ixdarklord.folios.api.FoliosBookItem;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * The mod's one item, in a creative tab of its own ({@link #TAB}): its guide book, a Folios {@link FoliosBookItem} that always opens {@link GuideBookGift#BOOK},
 * named "Insomniac's Handbook". Its stacks are given with the book in their {@code folios:guide_book} component too
 * ({@link GuideBookGift#stack}), which Folios draws the cover from.
 */
public final class WCISItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(WhyCantISleep.MOD_ID, Registries.ITEM);

    public static final RegistryEntry<FoliosBookItem> GUIDE_BOOK = ITEMS.register("guide_book",
            () -> new Handbook(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(WhyCantISleep.MOD_ID, Registries.CREATIVE_MODE_TAB);

    /**
     * The mod's creative tab, holding the guide book as it is given (its book named, so its cover is drawn), which is
     * also where JEI and the like find it.
     */
    public static final RegistryEntry<CreativeModeTab> TAB = TABS.register("main", () -> CreativeTabs.builder()
            .title(Component.translatable("itemGroup.whycantisleep"))
            .icon(GuideBookGift::stack)
            .displayItems((parameters, output) -> output.accept(GuideBookGift.stack()))
            .build());

    private WCISItems() {
    }

    public static void register() {
        ITEMS.register();
        TABS.register();
    }

    /**
     * The guide book as an item of its own name, "Insomniac's Handbook", rather than the book's title Folios gives its
     * books (a custom name on the stack still wins).
     */
    private static final class Handbook extends FoliosBookItem {
        private Handbook(Properties properties) {
            super(properties, GuideBookGift.BOOK);
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.translatable(this.getDescriptionId());
        }
    }
}
