package net.ixdarklord.whycantisleep.lurker;

import net.ixdarklord.coolcatcore.api.registry.DeferredRegister;
import net.ixdarklord.coolcatcore.api.registry.EntityAttributes;
import net.ixdarklord.coolcatcore.api.registry.RegistryEntry;
import net.ixdarklord.whycantisleep.WhyCantISleep;
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

    private WCISEntities() {
    }

    public static void register() {
        ENTITIES.register();
        EntityAttributes.register(LURKER, LurkerEntity::createAttributes);
    }
}
