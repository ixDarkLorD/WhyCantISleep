package net.ixdarklord.whycantisleep.between;

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

    /** Held while falling asleep with a clear mind, it takes its holder down into the Dark Between ({@link DarkBetween}). */
    public static final RegistryEntry<Item> DREAM_LENS = ITEMS.register("dream_lens",
            () -> new DreamLensItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, WhyCantISleep.id("dream_lens"))).stacksTo(1).rarity(Rarity.UNCOMMON)));

    /** A shard of a broken urn: rubbish, mostly; a haunted urn throws them. */
    public static final RegistryEntry<Item> URN_SHARD = ITEMS.register("urn_shard",
            () -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, WhyCantISleep.id("urn_shard")))));

    /** A kite of moth-wing cloth: held while falling, its holder glides (DreamGlider). */
    public static final RegistryEntry<Item> DREAM_GLIDER = ITEMS.register("dream_glider",
            () -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, WhyCantISleep.id("dream_glider"))).durability(384)));
    /** A spool of lucid thread: used, it casts a walkway out ahead (LucidSpoolItem). */
    public static final RegistryEntry<Item> LUCID_SPOOL = ITEMS.register("lucid_spool",
            () -> new LucidSpoolItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, WhyCantISleep.id("lucid_spool"))).durability(48)));

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

    // What the Dark Between's wild creatures leave (CharmItem, WildCharms): each still does what its creature did.
    public static final RegistryEntry<Item> MARES_EMBER = charm("mares_ember", props -> props.stacksTo(16).rarity(Rarity.UNCOMMON), WildCharms::maresEmber, 20, SoundEvents.BLAZE_SHOOT, 0.7F);
    public static final RegistryEntry<Item> HOLLOW_FANG = charm("hollow_fang", props -> props.durability(24).rarity(Rarity.UNCOMMON), WildCharms::hollowFang, 600, SoundEvents.FOX_SCREECH, 0.6F);
    public static final RegistryEntry<Item> LULLABY_BOX = charm("lullaby_box", props -> props.durability(12).rarity(Rarity.RARE), WildCharms::lullabyBox, 2400, SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8F);
    public static final RegistryEntry<Item> GAZERS_LENS = charm("gazers_lens", props -> props.stacksTo(16).rarity(Rarity.UNCOMMON), WildCharms::gazersLens, 20, SoundEvents.ENDER_EYE_DEATH, 1.2F);
    public static final RegistryEntry<Item> STOPWATCH = charm("stopwatch", props -> props.durability(16).rarity(Rarity.RARE), WildCharms::stopwatch, 800, SoundEvents.BELL_BLOCK, 1.6F);
    public static final RegistryEntry<Item> LIVING_THREAD = charm("living_thread", props -> props.rarity(Rarity.COMMON), WildCharms::livingThread, 10, SoundEvents.WOOL_PLACE, 1.2F);
    public static final RegistryEntry<Item> GLOOM_GEL = charm("gloom_gel", props -> props.stacksTo(16).rarity(Rarity.COMMON), WildCharms::gloomGel, 20, SoundEvents.HONEY_DRINK.value(), 0.8F);
    public static final RegistryEntry<Item> SWAP_CARD = charm("swap_card", props -> props.stacksTo(16).rarity(Rarity.UNCOMMON), WildCharms::swapCard, 40, SoundEvents.ENDERMAN_TELEPORT, 1.5F);
    public static final RegistryEntry<Item> CLOSET_KEY = charm("closet_key", props -> props.durability(8).rarity(Rarity.RARE), WildCharms::closetKey, 1200, SoundEvents.WOODEN_DOOR_CLOSE, 0.7F);
    public static final RegistryEntry<Item> HUFF_AND_PUFF = charm("huff_and_puff", props -> props.durability(16).rarity(Rarity.UNCOMMON), WildCharms::huffAndPuff, 300, SoundEvents.ENDER_DRAGON_FLAP, 1.6F);
    public static final RegistryEntry<Item> DREAM_SAND = charm("dream_sand", props -> props.stacksTo(16).rarity(Rarity.UNCOMMON), WildCharms::dreamSand, 100, SoundEvents.SAND_BREAK, 0.8F);
    public static final RegistryEntry<Item> FAIRY_DUST = charm("fairy_dust", props -> props.stacksTo(16).rarity(Rarity.COMMON), WildCharms::fairyDust, 40, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F);
    public static final RegistryEntry<Item> BROODSILK = charm("broodsilk", props -> props.durability(64).rarity(Rarity.UNCOMMON), WildCharms::broodsilk, 15, SoundEvents.FISHING_BOBBER_THROW, 0.6F);
    public static final RegistryEntry<Item> VOID_SCALE = charm("void_scale", props -> props.stacksTo(8).rarity(Rarity.RARE), WildCharms::voidScale, 100, SoundEvents.CHORUS_FRUIT_TELEPORT, 0.8F);
    public static final RegistryEntry<Item> SWARM_JAR = charm("swarm_jar", props -> props.stacksTo(16).rarity(Rarity.UNCOMMON), WildCharms::swarmJar, 40, SoundEvents.BEEHIVE_EXIT, 0.6F);
    public static final RegistryEntry<Item> SHRINKING_SKIN = charm("shrinking_skin", props -> props.stacksTo(8).rarity(Rarity.UNCOMMON), WildCharms::shrinkingSkin, 40, SoundEvents.SLIME_SQUISH_SMALL, 0.7F);
    public static final RegistryEntry<Item> HARES_FOOT = charm("hares_foot", props -> props.stacksTo(16).rarity(Rarity.COMMON), WildCharms::haresFoot, 20, SoundEvents.RABBIT_JUMP, 1.0F);

    /** A charm: its own effect when used, a line under its name saying what that is (lang: item.<mod>.<id>.desc). */
    private static RegistryEntry<Item> charm(String id, java.util.function.UnaryOperator<Item.Properties> props, CharmItem.Effect effect, int cooldown,
                                             net.minecraft.sounds.SoundEvent sound, float pitch) {
        return ITEMS.register(id, () -> new CharmItem(props.apply(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, WhyCantISleep.id(id))))
                .component(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                        Component.translatable("item." + WhyCantISleep.MOD_ID + "." + id + ".desc")
                                .withStyle(style -> style.withColor(net.minecraft.ChatFormatting.GRAY).withItalic(false))))),
                effect, cooldown, sound, pitch));
    }

    /** Whether an entity has the Weeping Veil on its head. */
    public static boolean wearsVeil(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(WEEPING_VEIL.get());
    }

    static {
        WCISBlocks.registerItems(ITEMS);
    }

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(WhyCantISleep.MOD_ID, Registries.CREATIVE_MODE_TAB);
    /** The mod's creative tab: the lens and every block of the Dark Between. */
    public static final RegistryEntry<CreativeModeTab> TAB = TABS.register("dark_between", () -> CreativeTabs.builder()
            .title(Component.translatable("itemGroup.whycantisleep.dark_between"))
            .icon(() -> new ItemStack(DREAM_LENS.get()))
            .displayItems((parameters, output) -> {
                output.accept(DREAM_LENS.get());
                output.accept(WEEPING_HEART.get());
                output.accept(WEEPING_VEIL.get());
                output.accept(DREAM_GLIDER.get());
                output.accept(LUCID_SPOOL.get());
                output.accept(URN_SHARD.get());
                output.accept(MARES_EMBER.get());
                output.accept(HOLLOW_FANG.get());
                output.accept(LULLABY_BOX.get());
                output.accept(GAZERS_LENS.get());
                output.accept(STOPWATCH.get());
                output.accept(LIVING_THREAD.get());
                output.accept(GLOOM_GEL.get());
                output.accept(HARES_FOOT.get());
                output.accept(SWAP_CARD.get());
                output.accept(CLOSET_KEY.get());
                output.accept(HUFF_AND_PUFF.get());
                output.accept(DREAM_SAND.get());
                output.accept(FAIRY_DUST.get());
                output.accept(BROODSILK.get());
                output.accept(VOID_SCALE.get());
                output.accept(SWARM_JAR.get());
                output.accept(SHRINKING_SKIN.get());
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
