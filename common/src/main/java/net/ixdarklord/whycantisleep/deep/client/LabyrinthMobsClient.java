package net.ixdarklord.whycantisleep.deep.client;

import net.ixdarklord.coolcatcanvas.api.armature.Motion;
import net.ixdarklord.coolcatcanvas.api.client.armature.Armature;
import net.ixdarklord.coolcatcanvas.api.client.armature.render.ArmatureMobRenderer;
import net.ixdarklord.coolcatcore.api.client.registry.EntityRendererRegistry;
import net.ixdarklord.coolcatcore.api.registry.RegistryEntry;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.lurker.WCISEntities;
import net.ixdarklord.whycantisleep.deep.mob.TombRevenantEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * The labyrinth's creatures as they are drawn (deep/mob): CoolCatLib armatures from tools/gen_mobs.py. A base layer
 * walks when they move and idles otherwise (the Briar Archer draws its bow while it has a target); an action layer
 * plays their blow whenever they swing.
 */
public final class LabyrinthMobsClient {
    private static final Motion IDLE = Motion.loop("idle").fade(0.25F);
    private static final Motion WALK = Motion.loop("walk").fade(0.2F);
    private static final Motion AIM = Motion.loop("aim").fade(0.2F);
    private static final Motion ATTACK = Motion.once("attack").fade(0.08F);
    private static final Motion RISE = Motion.hold("rise").fade(0.0F);
    // Lying in its tomb (the tomb's stand-in, TombClient): the first frame of its rise, held.
    private static final Motion LIE = Motion.hold("rise").speed(0.0F).fade(0.0F);
    // Its eyes kindle as it sits up: dark until this far into its rise (ticks), lit fully this many ticks after.
    private static final float EYES_KINDLE = 8.0F;
    private static final float EYES_FULL = 12.0F;

    private LabyrinthMobsClient() {
    }

    public static void init() {
        mob(WCISEntities.MOURNER, "mourner", 0.5F, true, false);
        mob(WCISEntities.BRIAR_ARCHER, "briar_archer", 0.5F, false, true);
        mob(WCISEntities.THORNLING, "thornling", 0.6F, true, false);
        mob(WCISEntities.WEEPING_WISP, "weeping_wisp", 0.3F, false, false);
        mob(WCISEntities.HEDGE_WARDEN, "hedge_warden", 1.0F, true, false);
        mob(WCISEntities.TOMB_REVENANT, "tomb_revenant", 0.5F, true, false);
    }

    private static <T extends Mob> void mob(RegistryEntry<EntityType<T>> type, String name, float shadow, boolean strikes, boolean archer) {
        Armature armature = Armature.of(WhyCantISleep.id(name));
        EntityRendererRegistry.register(type, context -> {
            ArmatureMobRenderer<T> renderer = new ArmatureMobRenderer<>(context, armature, shadow) {
                // As vanilla's mobs: no name over it unless it was given one and is looked at.
                @Override
                protected boolean shouldShowName(T entity, double distanceToCameraSq) {
                    return super.shouldShowName(entity, distanceToCameraSq) && (entity.shouldShowName()
                            || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
                }
            };
            if (archer) renderer.holdItem("right_hand", EquipmentSlot.MAINHAND);
            renderer.glow((entity, partialTicks) -> entity instanceof TombRevenantEntity revenant
                    ? revenant.lying ? 0.0F : Math.clamp((revenant.riseTicks(partialTicks) - EYES_KINDLE) / EYES_FULL, 0.0F, 1.0F)
                    : 1.0F);
            renderer.animate(animator -> {
                animator.layer("base").drive(ctx -> {
                    // A Tomb Revenant climbs out of its tomb before anything else.
                    if (ctx.entity() instanceof TombRevenantEntity revenant && revenant.lying) return LIE;
                    if (ctx.entity() instanceof TombRevenantEntity revenant && revenant.isRising()) return RISE;
                    if (archer && ctx.entity() instanceof Mob mob && mob.isAggressive()) return AIM;
                    return ctx.moving() ? WALK : IDLE;
                });
                if (strikes) {
                    animator.layer("action").drive(ctx -> ctx.entity() instanceof LivingEntity living && living.swinging ? ATTACK : null);
                }
            });
            return renderer;
        });
    }
}
