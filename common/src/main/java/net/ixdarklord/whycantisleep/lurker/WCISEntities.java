package net.ixdarklord.whycantisleep.lurker;

import net.ixdarklord.coolcatcore.api.registry.DeferredRegister;
import net.ixdarklord.coolcatcore.api.registry.EntityAttributes;
import net.ixdarklord.coolcatcore.api.registry.RegistryEntry;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.deep.GardenerEntity;
import net.ixdarklord.whycantisleep.deep.mob.BriarArcherEntity;
import net.ixdarklord.whycantisleep.deep.mob.HedgeWardenEntity;
import net.ixdarklord.whycantisleep.deep.mob.MournerEntity;
import net.ixdarklord.whycantisleep.deep.mob.ThornlingEntity;
import net.ixdarklord.whycantisleep.deep.mob.TombRevenantEntity;
import net.ixdarklord.whycantisleep.deep.mob.WeepingWispEntity;
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

    // The labyrinth's creatures (deep/mob): the dead, the briars, the tears, the hedge itself.
    public static final RegistryEntry<EntityType<MournerEntity>> MOURNER = mob("mourner", MournerEntity::new, 0.6F, 1.95F, 1.74F);
    public static final RegistryEntry<EntityType<BriarArcherEntity>> BRIAR_ARCHER = mob("briar_archer", BriarArcherEntity::new, 0.6F, 1.99F, 1.74F);
    public static final RegistryEntry<EntityType<ThornlingEntity>> THORNLING = mob("thornling", ThornlingEntity::new, 0.8F, 0.55F, 0.4F);
    public static final RegistryEntry<EntityType<WeepingWispEntity>> WEEPING_WISP = mob("weeping_wisp", WeepingWispEntity::new, 0.5F, 0.8F, 0.5F);
    public static final RegistryEntry<EntityType<TombRevenantEntity>> TOMB_REVENANT = mob("tomb_revenant", TombRevenantEntity::new, 0.65F, 2.45F, 2.2F);
    public static final RegistryEntry<EntityType<HedgeWardenEntity>> HEDGE_WARDEN = mob("hedge_warden", HedgeWardenEntity::new, 1.4F, 2.9F, 2.5F);

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
    }
}
