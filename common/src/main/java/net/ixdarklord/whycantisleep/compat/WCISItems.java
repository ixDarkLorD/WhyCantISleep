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
 * The mod's items, in a creative tab of its own ({@link #TAB}): the bed check's two tools ({@link #DREAMCATCHER},
 * {@link #BEDSIDE_LANTERN}) and its guide book, a Folios {@link FoliosBookItem} that always opens {@link GuideBookGift#BOOK},
 * named "Insomniac's Handbook". Its stacks are given with the book in their {@code folios:guide_book} component too
 * ({@link GuideBookGift#stack}), which Folios draws the cover from.
 */
public final class WCISItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(WhyCantISleep.MOD_ID, Registries.ITEM);

    public static final RegistryEntry<FoliosBookItem> GUIDE_BOOK = ITEMS.register("guide_book",
            () -> new Handbook(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    /**
     * The bed check's tools, each read by sneaking and using a bed with it in hand: a Dreamcatcher tells how a night in
     * the bed would go; a Bedside Lantern marks where in its room a monster could spawn ({@code sleep/BedCheck}).
     */
    public static final RegistryEntry<Item> DREAMCATCHER = ITEMS.register("dreamcatcher",
            () -> new Item(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryEntry<Item> BEDSIDE_LANTERN = ITEMS.register("bedside_lantern",
            () -> new Item(new Item.Properties().stacksTo(1)));

    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(WhyCantISleep.MOD_ID, Registries.CREATIVE_MODE_TAB);

    /**
     * The mod's creative tab, holding the guide book as it is given (its book named, so its cover is drawn), which is
     * also where JEI and the like find it.
     */
    public static final RegistryEntry<CreativeModeTab> TAB = TABS.register("main", () -> CreativeTabs.builder()
            .title(Component.translatable("itemGroup.whycantisleep"))
            .icon(GuideBookGift::stack)
            .displayItems((parameters, output) -> {
                output.accept(GuideBookGift.stack());
                output.accept(new ItemStack(DREAMCATCHER.get()));
                output.accept(new ItemStack(BEDSIDE_LANTERN.get()));
            })
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
