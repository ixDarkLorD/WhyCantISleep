package net.ixdarklord.whycantisleep.compat;

import net.ixdarklord.coolcatcore.api.registry.DeferredRegister;
import net.ixdarklord.coolcatcore.api.registry.RegistryEntry;
import net.ixdarklord.folios.api.GuideBookItem;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.ixdarklord.folios.api.GuideBooks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * The mod's one item: its guide book, a Folios {@link GuideBookItem} that always opens {@link GuideBookGift#BOOK},
 * named "Insomniac's Handbook". Its stacks carry the book in their NBT too ({@code folios_book}, {@link GuideBookGift#stack}),
 * which Folios draws the cover from; on 1.20.1 a recipe's result can't carry NBT, so a crafted one is given it as it is
 * made, and any stack without it as it sits in an inventory.
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

        @Override
        public ItemStack getDefaultInstance() {
            return GuideBookGift.stack();
        }

        @Override
        public void onCraftedBy(ItemStack stack, Level level, Player player) {
            super.onCraftedBy(stack, level, player);
            nameBook(stack);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity owner, int slot, boolean selected) {
            super.inventoryTick(stack, level, owner, slot, selected);
            if (!level.isClientSide()) nameBook(stack);
        }

        // The key Folios reads a stack's book from (GuideBooks#bookOf).
        private static void nameBook(ItemStack stack) {
            if (GuideBooks.bookOf(stack) == null) stack.getOrCreateTag().putString("folios_book", GuideBookGift.BOOK.toString());
        }
    }
}
