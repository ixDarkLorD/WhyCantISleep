package net.ixdarklord.whycantisleep.deep;

import net.ixdarklord.coolcatcore.api.registry.DeferredRegister;
import net.ixdarklord.coolcatcore.api.registry.BlockEntityTypes;
import net.ixdarklord.coolcatcore.api.registry.RegistryEntry;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The Lucid Deep's blocks. Their models, textures, loot and tags are written by {@code tools/gen_deep_content.py}; this
 * class only says what each one is. Every block has an item of the same name.
 * <ul>
 *     <li><b>Dreamstone</b>: the dark violet stone the islands are made of, and what is built from it: bricks, cracked
 *     bricks, a chiseled block with an eye in it, engraved bricks with a band of runes, pillars, stairs, slabs and
 *     walls; polished dreamstone with its stairs and slabs, for ledges, caps and corbels; and veined dreamstone, run
 *     through with glowing red veins.</li>
 *     <li><b>Ashen soil</b> and <b>ashen moss</b>: the ground on top of the islands.</li>
 *     <li><b>Sleepwood</b>: bleached grey dead wood, the trees of the Deep and the timber of its ruins: logs, planks,
 *     stairs, slabs, fences; and rotten sleepwood and torn wallpaper, what the haunted house is made of.</li>
 *     <li><b>Withered hedge</b>: dead, thinning hedge, the walls of the Garden of Despair's maze.</li>
 *     <li><b>Light</b>: the dread lantern, burning red, and the wake crystal, which grows on veined stone.</li>
 *     <li><b>Plants</b>: the lucid bloom, a pale glowing flower; despair thorns; and hanging veins, glowing tendrils
 *     that hang under the islands.</li>
 * </ul>
 */
public final class WCISBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(WhyCantISleep.MOD_ID, Registries.BLOCK);
    /** Every block, in the order they show in the creative tab. */
    public static final List<RegistryEntry<? extends Block>> ALL = new ArrayList<>();

    public static final RegistryEntry<Block> DREAMSTONE = stone("dreamstone", Block::new);
    public static final RegistryEntry<Block> DREAMSTONE_BRICKS = stone("dreamstone_bricks", Block::new);
    public static final RegistryEntry<Block> CRACKED_DREAMSTONE_BRICKS = stone("cracked_dreamstone_bricks", Block::new);
    public static final RegistryEntry<Block> CHISELED_DREAMSTONE = stone("chiseled_dreamstone", Block::new);
    public static final RegistryEntry<RotatedPillarBlock> DREAMSTONE_PILLAR = stone("dreamstone_pillar", RotatedPillarBlock::new);
    public static final RegistryEntry<StairBlock> DREAMSTONE_BRICK_STAIRS = stone("dreamstone_brick_stairs",
            properties -> new StairBlock(DREAMSTONE_BRICKS.get().defaultBlockState(), properties) {
            });
    public static final RegistryEntry<SlabBlock> DREAMSTONE_BRICK_SLAB = stone("dreamstone_brick_slab", SlabBlock::new);
    public static final RegistryEntry<WallBlock> DREAMSTONE_BRICK_WALL = stone("dreamstone_brick_wall", WallBlock::new);
    /** A wall of the raw stone, as cobblestone has one: posts, brackets, low walls. */
    public static final RegistryEntry<WallBlock> DREAMSTONE_WALL = stone("dreamstone_wall", WallBlock::new);
    public static final RegistryEntry<Block> ENGRAVED_DREAMSTONE_BRICKS = stone("engraved_dreamstone_bricks", Block::new);
    public static final RegistryEntry<Block> POLISHED_DREAMSTONE = stone("polished_dreamstone", Block::new);
    public static final RegistryEntry<StairBlock> POLISHED_DREAMSTONE_STAIRS = stone("polished_dreamstone_stairs",
            properties -> new StairBlock(POLISHED_DREAMSTONE.get().defaultBlockState(), properties) {
            });
    public static final RegistryEntry<SlabBlock> POLISHED_DREAMSTONE_SLAB = stone("polished_dreamstone_slab", SlabBlock::new);
    public static final RegistryEntry<Block> VEINED_DREAMSTONE = block("veined_dreamstone", Block::new,
            () -> stoneProperties().lightLevel(state -> 6));  // only the veins glow (their model's emissive layer)

    /** Bricks split by glowing veins, for the Garden's walls. */
    public static final RegistryEntry<Block> VEINED_DREAMSTONE_BRICKS = block("veined_dreamstone_bricks", Block::new,
            () -> stoneProperties().lightLevel(state -> 5));  // only the veins glow (their model's emissive layer)

    /** Seals the weeping tree's heart; no tool cuts it: it breaks only when the Gardener dies. */
    public static final RegistryEntry<Block> HEARTWOOD_SEAL = block("heartwood_seal", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1.0F, 3600000.0F).noLootTable()
                    .sound(SoundType.CHERRY_WOOD));  // lit like the trunk it is part of, not glowing

    public static final RegistryEntry<Block> ASHEN_SOIL = block("ashen_soil", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.6F).sound(SoundType.SOUL_SOIL));
    public static final RegistryEntry<Block> ASHEN_MOSS = block("ashen_moss", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.7F).sound(SoundType.NYLIUM));

    public static final RegistryEntry<RotatedPillarBlock> SLEEPWOOD_LOG = wood("sleepwood_log", RotatedPillarBlock::new);
    public static final RegistryEntry<Block> SLEEPWOOD_PLANKS = wood("sleepwood_planks", Block::new);
    public static final RegistryEntry<StairBlock> SLEEPWOOD_STAIRS = wood("sleepwood_stairs",
            properties -> new StairBlock(SLEEPWOOD_PLANKS.get().defaultBlockState(), properties) {
            });
    public static final RegistryEntry<SlabBlock> SLEEPWOOD_SLAB = wood("sleepwood_slab", SlabBlock::new);
    public static final RegistryEntry<FenceBlock> SLEEPWOOD_FENCE = wood("sleepwood_fence", FenceBlock::new);
    public static final RegistryEntry<Block> ROTTEN_SLEEPWOOD = wood("rotten_sleepwood", Block::new);
    public static final RegistryEntry<Block> TORN_WALLPAPER = wood("torn_wallpaper", Block::new);
    public static final RegistryEntry<CarpetBlock> DUSTY_CARPET = block("dusty_carpet", CarpetBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.1F).sound(SoundType.WOOL));

    public static final RegistryEntry<Block> WITHERED_HEDGE = block("withered_hedge", Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.6F).sound(SoundType.AZALEA_LEAVES).noOcclusion()
                    .isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false));

    public static final RegistryEntry<DreadLanternBlock> DREAD_LANTERN = block("dread_lantern", DreadLanternBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.5F).sound(SoundType.LANTERN).lightLevel(state -> 14)
                    .noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final RegistryEntry<AmethystClusterBlock> WAKE_CRYSTAL = block("wake_crystal",
            properties -> new AmethystClusterBlock(7.0F, 3.0F, properties),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5F).sound(SoundType.AMETHYST_CLUSTER).lightLevel(state -> 7)
                    .noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final RegistryEntry<FlowerBlock> LUCID_BLOOM = plant("lucid_bloom",
            properties -> new DeepFlowerBlock(properties), 9);
    public static final RegistryEntry<FlowerBlock> DESPAIR_THORNS = plant("despair_thorns",
            properties -> new DeepFlowerBlock(properties), 0);
    public static final RegistryEntry<HangingRootsBlock> HANGING_VEINS = block("hanging_veins", properties -> new HangingRootsBlock(properties) {
            }, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollision().instabreak().sound(SoundType.HANGING_ROOTS)
                    .lightLevel(state -> 4).pushReaction(PushReaction.DESTROY));
    public static final RegistryEntry<WeepingVeinsBlock> WEEPING_VEINS = block("weeping_veins", WeepingVeinsBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).noCollision().instabreak().sound(SoundType.WEEPING_VINES)
                    .lightLevel(state -> state.getValue(WeepingVeinsBlock.TIP) ? 6 : 3).pushReaction(PushReaction.DESTROY));

    // The Garden's furnishings (DecorBlock): shapes given facing north, as their models (gen_deep_blocks.py) are drawn.
    public static final RegistryEntry<DecorBlock> DREAMSTONE_URN = block("dreamstone_urn",
            properties -> new DecorBlock(properties, Block.box(4, 0, 4, 12, 12, 12), false),
            () -> stoneProperties().noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final RegistryEntry<DecorBlock> STONE_PLANTER = block("stone_planter",
            properties -> new DecorBlock(properties, Block.box(1, 0, 3, 15, 7, 13), false),
            () -> stoneProperties().noOcclusion());
    public static final RegistryEntry<DecorBlock> DREAD_BRAZIER = block("dread_brazier",
            properties -> new DecorBlock(properties, Block.box(0.5, 0, 0.5, 15.5, 15, 15.5), true),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5F).sound(SoundType.LANTERN).lightLevel(state -> 13)
                    .noOcclusion().pushReaction(PushReaction.DESTROY));

    public static final RegistryEntry<FireLampBlock> DREAD_FIRE_LAMP = block("dread_fire_lamp", FireLampBlock::new,
            () -> stoneProperties().lightLevel(state -> state.getValue(FireLampBlock.PART) == FireLampBlock.Part.TOP ? 14 : 0).noOcclusion());

    public static final RegistryEntry<TombBlock> STONE_TOMB = block("stone_tomb", TombBlock::new, () -> stoneProperties().noOcclusion());

    /** Spikes for the floors of the Garden's pits ({@link SpikesBlock}). */
    public static final RegistryEntry<SpikesBlock> DREAMSTONE_SPIKES = block("dreamstone_spikes", SpikesBlock::new,
            () -> stoneProperties().noOcclusion().noCollision().pushReaction(PushReaction.DESTROY));
    /** A ladder of sleepwood. */
    public static final RegistryEntry<LadderBlock> SLEEPWOOD_LADDER = block("sleepwood_ladder", properties -> new LadderBlock(properties) {
            },
            () -> BlockBehaviour.Properties.of().forceSolidOff().strength(0.4F).sound(SoundType.LADDER).noOcclusion().pushReaction(PushReaction.DESTROY));

    /** The heartwood reliquary: the treasure box in the weeping tree's heart ({@link ReliquaryBlock}). */
    public static final RegistryEntry<ReliquaryBlock> HEARTWOOD_RELIQUARY = block("heartwood_reliquary", ReliquaryBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F).sound(SoundType.CHERRY_WOOD)
                    .lightLevel(state -> 3).noOcclusion());

    /** The way out of the weeping tree's heart, which wakes when the reliquary is first opened ({@link HeartPortalBlock}). */
    public static final RegistryEntry<HeartPortalBlock> HEART_PORTAL = block("heart_portal", HeartPortalBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(-1.0F, 3600000.0F).noLootTable().noOcclusion()
                    .sound(SoundType.WOOD).lightLevel(state -> state.getValue(HeartPortalBlock.ACTIVE) ? 11 : 2).pushReaction(PushReaction.BLOCK));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(WhyCantISleep.MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    public static final RegistryEntry<BlockEntityType<ReliquaryBlockEntity>> RELIQUARY_ENTITY = BLOCK_ENTITIES.register("heartwood_reliquary",
            () -> BlockEntityTypes.create(ReliquaryBlockEntity::new, HEARTWOOD_RELIQUARY.get()));
    public static final RegistryEntry<BlockEntityType<HeartPortalBlockEntity>> HEART_PORTAL_ENTITY = BLOCK_ENTITIES.register("heart_portal",
            () -> BlockEntityTypes.create(HeartPortalBlockEntity::new, HEART_PORTAL.get()));
    public static final RegistryEntry<BlockEntityType<TombBlockEntity>> TOMB_ENTITY = BLOCK_ENTITIES.register("stone_tomb",
            () -> BlockEntityTypes.create(TombBlockEntity::new, STONE_TOMB.get()));
    public static final RegistryEntry<BlockEntityType<FireLampBlockEntity>> FIRE_LAMP_ENTITY = BLOCK_ENTITIES.register("dread_fire_lamp",
            () -> BlockEntityTypes.create(FireLampBlockEntity::new, DREAD_FIRE_LAMP.get()));
    public static final RegistryEntry<BlockEntityType<DreadLanternBlockEntity>> DREAD_LANTERN_ENTITY = BLOCK_ENTITIES.register("dread_lantern",
            () -> BlockEntityTypes.create(DreadLanternBlockEntity::new, DREAD_LANTERN.get()));

    private WCISBlocks() {
    }

    public static void register() {
        BLOCKS.register();
        BLOCK_ENTITIES.register();
    }

    /** The items, one for each block. */
    public static void registerItems(DeferredRegister<Item> items) {
        for (RegistryEntry<? extends Block> block : ALL) {
            String name = block.getId().getPath();
            items.register(name, () -> new BlockItem(block.get(), new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, WhyCantISleep.id(name))).useBlockDescriptionPrefix()));
        }
    }

    private static BlockBehaviour.Properties stoneProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.8F, 6.0F).requiresCorrectToolForDrops()
                .sound(SoundType.DEEPSLATE_BRICKS);
    }

    private static <T extends Block> RegistryEntry<T> stone(String name, Function<BlockBehaviour.Properties, T> make) {
        return block(name, make, WCISBlocks::stoneProperties);
    }

    private static <T extends Block> RegistryEntry<T> wood(String name, Function<BlockBehaviour.Properties, T> make) {
        return block(name, make, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(2.0F, 3.0F)
                .sound(SoundType.CHERRY_WOOD));
    }

    private static <T extends Block> RegistryEntry<T> plant(String name, Function<BlockBehaviour.Properties, T> make, int light) {
        return block(name, make, () -> BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.ROOTS)
                .lightLevel(state -> light).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY));
    }

    private static <T extends Block> RegistryEntry<T> block(String name, Function<BlockBehaviour.Properties, T> make, Supplier<BlockBehaviour.Properties> properties) {
        RegistryEntry<T> entry = BLOCKS.register(name,
                () -> make.apply(properties.get().setId(ResourceKey.create(Registries.BLOCK, WhyCantISleep.id(name)))));
        ALL.add(entry);
        return entry;
    }

    /** A plant of the Deep: it grows on the Deep's own ground as well as on dirt. */
    static final class DeepFlowerBlock extends FlowerBlock {
        DeepFlowerBlock(BlockBehaviour.Properties properties) {
            super(MobEffects.NIGHT_VISION, 5.0F, properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(ASHEN_SOIL.get()) || state.is(ASHEN_MOSS.get()) || state.is(DREAMSTONE.get()) || state.is(BlockTags.DIRT);
        }
    }
}
