package net.ixdarklord.whycantisleep.deep;

import net.ixdarklord.coolcatcore.api.registry.DeferredRegister;
import net.ixdarklord.coolcatcore.api.registry.RegistryEntry;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** The mod's items. */
public final class WCISItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(WhyCantISleep.MOD_ID, Registries.ITEM);

    /** Held while falling asleep with a clear mind, it takes its holder down into the Lucid Deep ({@link LucidDeep}). */
    public static final RegistryEntry<Item> DREAM_LENS = ITEMS.register("dream_lens",
            () -> new DreamLensItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, WhyCantISleep.id("dream_lens"))).stacksTo(1).rarity(Rarity.UNCOMMON)));

    private WCISItems() {
    }

    public static void register() {
        ITEMS.register();
    }
}
