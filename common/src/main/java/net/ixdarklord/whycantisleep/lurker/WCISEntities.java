package net.ixdarklord.whycantisleep.lurker;

import net.ixdarklord.coolcatcore.api.registry.DeferredRegister;
import net.ixdarklord.coolcatcore.api.registry.EntityAttributes;
import net.ixdarklord.coolcatcore.api.registry.RegistryEntry;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.between.GardenerEntity;
import net.ixdarklord.whycantisleep.between.mob.BriarArcherEntity;
import net.ixdarklord.whycantisleep.between.mob.DreadMantaEntity;
import net.ixdarklord.whycantisleep.between.mob.DreamEaterEntity;
import net.ixdarklord.whycantisleep.between.mob.NightMareEntity;
import net.ixdarklord.whycantisleep.between.mob.HollowHoundEntity;
import net.ixdarklord.whycantisleep.between.mob.LullabyEntity;
import net.ixdarklord.whycantisleep.between.mob.GazerEntity;
import net.ixdarklord.whycantisleep.between.mob.TickTockEntity;
import net.ixdarklord.whycantisleep.between.mob.StitchlingEntity;
import net.ixdarklord.whycantisleep.between.mob.GloomJellyEntity;
import net.ixdarklord.whycantisleep.between.mob.GrinningClownEntity;
import net.ixdarklord.whycantisleep.between.mob.LittleRedEntity;
import net.ixdarklord.whycantisleep.between.mob.MimeClownEntity;
import net.ixdarklord.whycantisleep.between.mob.JackInTheBoxEntity;
import net.ixdarklord.whycantisleep.between.mob.BalloonClownEntity;
import net.ixdarklord.whycantisleep.between.mob.FunhouseClownEntity;
import net.ixdarklord.whycantisleep.between.mob.ClosetBoogeymanEntity;
import net.ixdarklord.whycantisleep.between.mob.BigBadWolfEntity;
import net.ixdarklord.whycantisleep.between.mob.SandmanEntity;
import net.ixdarklord.whycantisleep.between.mob.ToothTakerEntity;
import net.ixdarklord.whycantisleep.between.mob.BroodmotherEntity;
import net.ixdarklord.whycantisleep.between.mob.BroodlingEntity;
import net.ixdarklord.whycantisleep.between.mob.VoidEelEntity;
import net.ixdarklord.whycantisleep.between.mob.HiveThingEntity;
import net.ixdarklord.whycantisleep.between.mob.CrawlspaceEntity;
import net.ixdarklord.whycantisleep.between.mob.DreamHareEntity;
import net.ixdarklord.whycantisleep.between.mob.FacelessEntity;
import net.ixdarklord.whycantisleep.between.mob.SomnimothEntity;
import net.ixdarklord.whycantisleep.between.mob.HauntedUrnEntity;
import net.ixdarklord.whycantisleep.between.mob.SleepwalkerEntity;
import net.ixdarklord.whycantisleep.between.mob.HedgeWardenEntity;
import net.ixdarklord.whycantisleep.between.mob.UrnShardEntity;
import net.ixdarklord.whycantisleep.between.mob.MournerEntity;
import net.ixdarklord.whycantisleep.between.mob.ThornlingEntity;
import net.ixdarklord.whycantisleep.between.mob.TombRevenantEntity;
import net.ixdarklord.whycantisleep.between.mob.WeepingWispEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** The mod's entity types. Having one is why the mod is needed on both the client and the server. */
public final class WCISEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(WhyCantISleep.MOD_ID, Registries.ENTITY_TYPE);

    /** The thing under the bed. It crawls, so it is low and wide. */
    public static final RegistryEntry<EntityType<LurkerEntity>> LURKER = ENTITIES.register("lurker",
            () -> EntityType.Builder.<LurkerEntity>of(LurkerEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 0.85F).eyeHeight(0.6F).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, WhyCantISleep.id("lurker"))));

    /** The Weeping Gardener, lord of the Garden of Despair: tall and gaunt. */
    public static final RegistryEntry<EntityType<GardenerEntity>> GARDENER = ENTITIES.register("gardener",
            () -> EntityType.Builder.<GardenerEntity>of(GardenerEntity::new, MobCategory.MONSTER)
                    .sized(1.3F, 3.6F).eyeHeight(3.3F).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, WhyCantISleep.id("gardener"))));

    // The labyrinth's creatures (between/mob): the dead, the briars, the tears, the hedge itself.
    public static final RegistryEntry<EntityType<MournerEntity>> MOURNER = mob("mourner", MournerEntity::new, 0.6F, 1.95F, 1.74F);
    public static final RegistryEntry<EntityType<BriarArcherEntity>> BRIAR_ARCHER = mob("briar_archer", BriarArcherEntity::new, 0.6F, 1.99F, 1.74F);
    public static final RegistryEntry<EntityType<ThornlingEntity>> THORNLING = mob("thornling", ThornlingEntity::new, 0.8F, 0.55F, 0.4F);
    public static final RegistryEntry<EntityType<WeepingWispEntity>> WEEPING_WISP = mob("weeping_wisp", WeepingWispEntity::new, 0.5F, 0.8F, 0.5F);
    public static final RegistryEntry<EntityType<TombRevenantEntity>> TOMB_REVENANT = mob("tomb_revenant", TombRevenantEntity::new, 0.65F, 2.45F, 2.2F);
    public static final RegistryEntry<EntityType<HedgeWardenEntity>> HEDGE_WARDEN = mob("hedge_warden", HedgeWardenEntity::new, 1.4F, 2.9F, 2.5F);
    // The open Dark Between's creatures (BetweenCreatures keeps them about its players).
    public static final RegistryEntry<EntityType<SleepwalkerEntity>> SLEEPWALKER = mob("sleepwalker", SleepwalkerEntity::new, 0.6F, 2.05F, 1.8F);
    public static final RegistryEntry<EntityType<DreadMantaEntity>> DREAD_MANTA = mob("dread_manta", DreadMantaEntity::new, 2.4F, 0.7F, 0.4F);
    public static final RegistryEntry<EntityType<DreamEaterEntity>> DREAM_EATER = mob("dream_eater", DreamEaterEntity::new, 1.2F, 1.6F, 1.3F);
    public static final RegistryEntry<EntityType<SomnimothEntity>> SOMNIMOTH = mob("somnimoth", SomnimothEntity::new, 0.5F, 0.4F, 0.25F);
    public static final RegistryEntry<EntityType<FacelessEntity>> FACELESS = mob("faceless", FacelessEntity::new, 0.6F, 2.6F, 2.35F);
    // Its wild creatures, of no structure (BetweenCreatures keeps them about its players too); the jelly and the hare
    // are no monsters.
    public static final RegistryEntry<EntityType<NightMareEntity>> NIGHT_MARE = mob("night_mare", NightMareEntity::new, 0.9F, 1.9F, 1.7F);
    public static final RegistryEntry<EntityType<HollowHoundEntity>> HOLLOW_HOUND = mob("hollow_hound", HollowHoundEntity::new, 0.6F, 0.9F, 0.75F);
    public static final RegistryEntry<EntityType<LullabyEntity>> LULLABY = mob("lullaby", LullabyEntity::new, 0.6F, 1.8F, 1.55F);
    public static final RegistryEntry<EntityType<GazerEntity>> GAZER = mob("gazer", GazerEntity::new, 0.8F, 1.2F, 0.9F);
    public static final RegistryEntry<EntityType<TickTockEntity>> TICK_TOCK = mob("tick_tock", TickTockEntity::new, 0.7F, 0.6F, 0.4F);
    public static final RegistryEntry<EntityType<StitchlingEntity>> STITCHLING = mob("stitchling", StitchlingEntity::new, 0.6F, 1.3F, 1.1F);
    public static final RegistryEntry<EntityType<GloomJellyEntity>> GLOOM_JELLY = creature("gloom_jelly", GloomJellyEntity::new, 0.9F, 0.9F, 0.6F);
    public static final RegistryEntry<EntityType<GrinningClownEntity>> GRINNING_CLOWN = mob("grinning_clown", GrinningClownEntity::new, 0.7F, 2.4F, 2.1F);
    // The Grinning Clown's kin, each with a trick of its own.
    public static final RegistryEntry<EntityType<MimeClownEntity>> MIME_CLOWN = mob("mime_clown", MimeClownEntity::new, 0.6F, 2.2F, 1.95F);
    public static final RegistryEntry<EntityType<JackInTheBoxEntity>> JACK_IN_THE_BOX = mob("jack_in_the_box", JackInTheBoxEntity::new, 0.9F, 1.0F, 0.8F);
    public static final RegistryEntry<EntityType<BalloonClownEntity>> BALLOON_CLOWN = mob("balloon_clown", BalloonClownEntity::new, 0.7F, 2.3F, 2.0F);
    public static final RegistryEntry<EntityType<FunhouseClownEntity>> FUNHOUSE_CLOWN = mob("funhouse_clown", FunhouseClownEntity::new, 0.7F, 2.4F, 2.1F);
    // Children's-story nightmares and phobias.
    public static final RegistryEntry<EntityType<ClosetBoogeymanEntity>> CLOSET_BOOGEYMAN = mob("closet_boogeyman", ClosetBoogeymanEntity::new, 1.0F, 2.0F, 1.3F);
    public static final RegistryEntry<EntityType<BigBadWolfEntity>> BIG_BAD_WOLF = mob("big_bad_wolf", BigBadWolfEntity::new, 0.8F, 2.1F, 1.9F);
    public static final RegistryEntry<EntityType<SandmanEntity>> SANDMAN = mob("sandman", SandmanEntity::new, 0.7F, 2.5F, 2.2F);
    public static final RegistryEntry<EntityType<ToothTakerEntity>> TOOTH_TAKER = mob("tooth_taker", ToothTakerEntity::new, 0.45F, 0.55F, 0.4F);
    public static final RegistryEntry<EntityType<BroodmotherEntity>> BROODMOTHER = mob("broodmother", BroodmotherEntity::new, 2.0F, 1.2F, 0.8F);
    public static final RegistryEntry<EntityType<BroodlingEntity>> BROODLING = mob("broodling", BroodlingEntity::new, 0.45F, 0.3F, 0.2F);
    public static final RegistryEntry<EntityType<VoidEelEntity>> VOID_EEL = mob("void_eel", VoidEelEntity::new, 1.4F, 1.2F, 0.8F);
    public static final RegistryEntry<EntityType<HiveThingEntity>> HIVE_THING = mob("hive_thing", HiveThingEntity::new, 0.9F, 2.0F, 1.75F);
    public static final RegistryEntry<EntityType<CrawlspaceEntity>> CRAWLSPACE = mob("crawlspace", CrawlspaceEntity::new, 1.1F, 0.45F, 0.3F);
    /** Little Red Riding Hood: no monster, the one kind face out there. */
    public static final RegistryEntry<EntityType<LittleRedEntity>> LITTLE_RED = creature("little_red", LittleRedEntity::new, 0.5F, 1.35F, 1.1F);
    public static final RegistryEntry<EntityType<DreamHareEntity>> DREAM_HARE = creature("dream_hare", DreamHareEntity::new, 0.45F, 0.6F, 0.5F);
    /** An urn that would not break (UrnBlock): it floats after whoever disturbed it, throwing shards. */
    public static final RegistryEntry<EntityType<HauntedUrnEntity>> HAUNTED_URN = mob("haunted_urn", HauntedUrnEntity::new, 0.7F, 0.9F, 0.6F);
    /** A cursed shard a haunted urn throws. */
    public static final RegistryEntry<EntityType<UrnShardEntity>> URN_SHARD = ENTITIES.register("urn_shard",
            () -> EntityType.Builder.<UrnShardEntity>of(UrnShardEntity::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(4).updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, WhyCantISleep.id("urn_shard"))));

    private static <T extends net.minecraft.world.entity.Mob> RegistryEntry<EntityType<T>> creature(String name, EntityType.EntityFactory<T> factory,
                                                                                                    float width, float height, float eye) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(factory, MobCategory.CREATURE).sized(width, height).eyeHeight(eye)
                .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, WhyCantISleep.id(name))));
    }

    private static <T extends net.minecraft.world.entity.Mob> RegistryEntry<EntityType<T>> mob(String name, EntityType.EntityFactory<T> factory,
                                                                                               float width, float height, float eye) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MONSTER).sized(width, height).eyeHeight(eye)
                .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, WhyCantISleep.id(name))));
    }

    private WCISEntities() {
    }

    public static void register() {
        ENTITIES.register();
        EntityAttributes.register(LURKER, LurkerEntity::createAttributes);
        EntityAttributes.register(GARDENER, GardenerEntity::createAttributes);
        EntityAttributes.register(MOURNER, MournerEntity::createAttributes);
        EntityAttributes.register(BRIAR_ARCHER, BriarArcherEntity::createAttributes);
        EntityAttributes.register(THORNLING, ThornlingEntity::createAttributes);
        EntityAttributes.register(WEEPING_WISP, WeepingWispEntity::createAttributes);
        EntityAttributes.register(HEDGE_WARDEN, HedgeWardenEntity::createAttributes);
        EntityAttributes.register(TOMB_REVENANT, TombRevenantEntity::createAttributes);
        EntityAttributes.register(HAUNTED_URN, HauntedUrnEntity::createAttributes);
        EntityAttributes.register(SLEEPWALKER, SleepwalkerEntity::createAttributes);
        EntityAttributes.register(DREAD_MANTA, DreadMantaEntity::createAttributes);
        EntityAttributes.register(DREAM_EATER, DreamEaterEntity::createAttributes);
        EntityAttributes.register(SOMNIMOTH, SomnimothEntity::createAttributes);
        EntityAttributes.register(FACELESS, FacelessEntity::createAttributes);
        EntityAttributes.register(NIGHT_MARE, NightMareEntity::createAttributes);
        EntityAttributes.register(HOLLOW_HOUND, HollowHoundEntity::createAttributes);
        EntityAttributes.register(LULLABY, LullabyEntity::createAttributes);
        EntityAttributes.register(GAZER, GazerEntity::createAttributes);
        EntityAttributes.register(TICK_TOCK, TickTockEntity::createAttributes);
        EntityAttributes.register(STITCHLING, StitchlingEntity::createAttributes);
        EntityAttributes.register(GLOOM_JELLY, GloomJellyEntity::createAttributes);
        EntityAttributes.register(DREAM_HARE, DreamHareEntity::createAttributes);
        EntityAttributes.register(GRINNING_CLOWN, GrinningClownEntity::createAttributes);
        EntityAttributes.register(LITTLE_RED, LittleRedEntity::createAttributes);
        EntityAttributes.register(MIME_CLOWN, MimeClownEntity::createAttributes);
        EntityAttributes.register(JACK_IN_THE_BOX, JackInTheBoxEntity::createAttributes);
        EntityAttributes.register(BALLOON_CLOWN, BalloonClownEntity::createAttributes);
        EntityAttributes.register(FUNHOUSE_CLOWN, FunhouseClownEntity::createAttributes);
        EntityAttributes.register(CLOSET_BOOGEYMAN, ClosetBoogeymanEntity::createAttributes);
        EntityAttributes.register(BIG_BAD_WOLF, BigBadWolfEntity::createAttributes);
        EntityAttributes.register(SANDMAN, SandmanEntity::createAttributes);
        EntityAttributes.register(TOOTH_TAKER, ToothTakerEntity::createAttributes);
        EntityAttributes.register(BROODMOTHER, BroodmotherEntity::createAttributes);
        EntityAttributes.register(BROODLING, BroodlingEntity::createAttributes);
        EntityAttributes.register(VOID_EEL, VoidEelEntity::createAttributes);
        EntityAttributes.register(HIVE_THING, HiveThingEntity::createAttributes);
        EntityAttributes.register(CRAWLSPACE, CrawlspaceEntity::createAttributes);
    }
}
