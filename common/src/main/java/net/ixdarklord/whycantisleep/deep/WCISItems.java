package net.ixdarklord.whycantisleep.deep;

import net.ixdarklord.coolcatcore.api.registry.CreativeTabs;
import net.ixdarklord.coolcatcore.api.registry.DeferredRegister;
import net.ixdarklord.coolcatcore.api.registry.RegistryEntry;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;

/** The mod's items. */
public final class WCISItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(WhyCantISleep.MOD_ID, Registries.ITEM);

    /** Held while falling asleep with a clear mind, it takes its holder down into the Lucid Deep ({@link LucidDeep}). */
    public static final RegistryEntry<Item> DREAM_LENS = ITEMS.register("dream_lens",
            () -> new DreamLensItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, WhyCantISleep.id("dream_lens"))).stacksTo(1).rarity(Rarity.UNCOMMON)));

    /** The heart of the Weeping Gardener, still beating: what it leaves when it falls. */
    public static final RegistryEntry<Item> WEEPING_HEART = ITEMS.register("weeping_heart",
            () -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, WhyCantISleep.id("weeping_heart"))).rarity(Rarity.EPIC).fireResistant()));

    /**
     * The Weeping Veil: a hood of the weeping tree's leaves, its veins hanging over the face, kept in the tree's sealed
     * heart. Worn, the Lurkers lose sight of whoever wears it ({@link #wearsVeil}).
     */
    public static final RegistryEntry<Item> WEEPING_VEIL = ITEMS.register("weeping_veil",
            () -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, WhyCantISleep.id("weeping_veil"))).stacksTo(1)
                    .rarity(Rarity.EPIC).component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                            .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, WhyCantISleep.id("weeping_veil")))
                            .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).build())));

    /** Whether an entity has the Weeping Veil on its head. */
    public static boolean wearsVeil(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(WEEPING_VEIL.get());
    }

    static {
        WCISBlocks.registerItems(ITEMS);
    }

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(WhyCantISleep.MOD_ID, Registries.CREATIVE_MODE_TAB);
    /** The mod's creative tab: the lens and every block of the Lucid Deep. */
    public static final RegistryEntry<CreativeModeTab> TAB = TABS.register("lucid_deep", () -> CreativeTabs.builder()
            .title(Component.translatable("itemGroup.whycantisleep.lucid_deep"))
            .icon(() -> new ItemStack(DREAM_LENS.get()))
            .displayItems((parameters, output) -> {
                output.accept(DREAM_LENS.get());
                output.accept(WEEPING_HEART.get());
                output.accept(WEEPING_VEIL.get());
                for (RegistryEntry<? extends Block> block : WCISBlocks.ALL) output.accept(block.get());
            })
            .build());

    private WCISItems() {
    }

    public static void register() {
        ITEMS.register();
        TABS.register();
    }
}
