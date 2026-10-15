package net.ixdarklord.whycantisleep.deep.client;

import net.ixdarklord.coolcatcanvas.api.armature.Motion;
import net.ixdarklord.coolcatcanvas.api.client.armature.Armature;
import net.ixdarklord.coolcatcanvas.api.client.bossbar.BossBarRenderers;
import net.ixdarklord.coolcatcanvas.api.client.bossbar.TexturedBossBar;
import net.ixdarklord.coolcatcanvas.api.client.armature.render.ArmatureMobRenderer;
import net.ixdarklord.coolcatcanvas.api.client.vfx.Curve;
import net.ixdarklord.coolcatcanvas.api.client.vfx.ParticleSpec;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcore.api.client.registry.EntityRendererRegistry;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.deep.GardenerEntity;
import net.ixdarklord.whycantisleep.lurker.LurkerPalette;
import net.ixdarklord.whycantisleep.lurker.WCISEntities;

/**
 * The Weeping Gardener as it is drawn ({@link GardenerEntity}): CoolCatLib's Armature renderer, its model and clips
 * from {@code tools/gen_gardener.py}. Its base layer kneels while it is dormant, walks when it moves and idles
 * otherwise; the server plays its attacks on a layer over that ({@code action}). Embers drift up off its heart, and its
 * scythe leaves a red wake as it sweeps.
 */
public final class GardenerClient {
    private static final Motion IDLE = Motion.loop("idle").fade(0.3F);
    private static final Motion WALK = Motion.loop("walk").fade(0.25F);
    private static final Motion DORMANT = Motion.loop("dormant").fade(0.4F);
    private static final ParticleSpec EMBER = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.GLOW))
            .lifetime(20, 36)
            .size(Curve.of(0.08F, 0.05F, 0.0F))
            .opacity(Curve.of(0.0F, 1.0F, 0.0F))
            .color(LurkerPalette.RED, LurkerPalette.CRIMSON)
            .drag(0.94F)
            .build();

    private GardenerClient() {
    }

    public static void init() {
        // Its own boss bar (tools/gen_boss_bars.py): the mask medallion, the thorned frame, the scythe's blade; the
        // half-way thorn kindles once it is past, the eye beats.
        BossBarRenderers.register(WhyCantISleep.id("gardener"), TexturedBossBar.builder(WhyCantISleep.id("textures/gui/boss/gardener.png"), 240, 48)
                .frame(0, 0, 240, 30).track(35, 12, 180, 7).fill(0, 32).trail(0, 40)
                .mark(0.5F, 200, 32, 2, 13, -3)
                .pulse(184, 32, 12, 12).at(12, 14)
                .nameColor(0xFFE8A0A8)
                .build());
        Armature armature = Armature.of(WhyCantISleep.id("gardener"));
        EntityRendererRegistry.register(WCISEntities.GARDENER, context -> {
            ArmatureMobRenderer<GardenerEntity> renderer = new ArmatureMobRenderer<>(context, armature, 1.1F) {
                // As vanilla's mobs: no name over it unless it was given one and is looked at.
                @Override
                protected boolean shouldShowName(GardenerEntity entity, double distanceToCameraSq) {
                    return super.shouldShowName(entity, distanceToCameraSq) && (entity.shouldShowName()
                            || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
                }
            };
            return renderer.deathFlip(false).animate(animator -> {
                animator.layer("base").drive(ctx -> ctx.entity() instanceof GardenerEntity gardener && gardener.isDormant()
                        ? DORMANT : ctx.moving() ? WALK : IDLE);
                animator.layer("action");
                animator.emitter("heart_core", EMBER).rate(10).speed(0.03F).spread(0.02F);
                animator.trail("blade_tip").length(10).width(Curve.of(0.6F, 0.0F)).colors(0xE0FF2D46, 0x008A0A24)
                        .when(ctx -> animator.isPlaying("swing"));
            });
        });
    }
}
