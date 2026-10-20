package net.ixdarklord.whycantisleep.combat;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;

/**
 * What of a boss's combat its clients see: its posture (0 to 1) and whether it is staggered, for its bar, its
 * animation and its effects. Synced entity data has to be defined per entity class, once, in a static field:
 * <pre>{@code
 * private static final CombatData COMBAT = CombatData.define(MyBoss.class);
 *
 * protected void defineSynchedData(SynchedEntityData.Builder builder) {
 *     super.defineSynchedData(builder);
 *     COMBAT.define(builder);
 * }
 * }</pre>
 */
public record CombatData(EntityDataAccessor<Float> posture, EntityDataAccessor<Boolean> staggered) {
    public static CombatData define(Class<? extends Entity> type) {
        return new CombatData(SynchedEntityData.defineId(type, EntityDataSerializers.FLOAT),
                SynchedEntityData.defineId(type, EntityDataSerializers.BOOLEAN));
    }

    public void define(SynchedEntityData.Builder builder) {
        builder.define(this.posture, 0.0F);
        builder.define(this.staggered, false);
    }

    /** Its posture, 0 (steady) to 1 (about to break), as its clients see it. */
    public float posture(Entity entity) {
        return entity.getEntityData().get(this.posture);
    }

    /** Whether it is staggered, as its clients see it. */
    public boolean staggered(Entity entity) {
        return entity.getEntityData().get(this.staggered);
    }
}
