package net.ixdarklord.whycantisleep.between.client;

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
import net.ixdarklord.whycantisleep.between.GardenerEntity;
import net.ixdarklord.whycantisleep.lurker.LurkerPalette;
import net.ixdarklord.whycantisleep.lurker.WCISEntities;

/**
 * The Weeping Gardener as it is drawn ({@link GardenerEntity}): CoolCatLib's Armature renderer, its model and clips
 * from {@code tools/gen_gardener.py}. Its base layer kneels while it is dormant, walks when it moves and idles
 * otherwise (or runs, when it runs its quarry down, {@link GardenerEntity#isRunning}); the server plays its attacks on a layer over that ({@code action}). Embers drift up off its heart, and its
 * scythe leaves a red wake as it sweeps. It weeps blood: drops well under its eye and run slowly down its mask
 * ({@link #TEAR}, from {@code tear_well}), and drops fall from the mask's edge ({@link #DRIP}, from {@code tear_drip}),
 * drawn out long as they fall.
 */
public final class GardenerClient {
    private static final Motion IDLE = Motion.loop("idle").fade(0.3F);
    private static final Motion WALK = Motion.loop("walk").fade(0.25F);
    private static final Motion RUN = Motion.loop("run").fade(0.25F);
    private static final Motion DORMANT = Motion.loop("dormant").fade(0.4F);
    private static final ParticleSpec EMBER = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.GLOW))
            .lifetime(20, 36)
            .size(Curve.of(0.08F, 0.05F, 0.0F))
            .opacity(Curve.of(0.0F, 1.0F, 0.0F))
            .color(LurkerPalette.RED, LurkerPalette.CRIMSON)
            .drag(0.94F)
            .build();

    // A drop of blood welling under its eye and running down its mask: barely moving at first, gathering, slipping down
    // slowly (heavy drag on a little gravity), dark and wet.
    private static final ParticleSpec TEAR = ParticleSpec.builder(VfxMaterial.alpha(VfxMaterial.GLOW))
            .lifetime(32, 40)
            .size(Curve.of(0.012F, 0.032F, 0.026F))
            .opacity(Curve.of(0.7F, 1.0F, 0.9F))
            .color(0xB0182A, 0x6E0A14)
            .gravity(0.0012F)
            .drag(0.86F)
            .build();
    // A drop falling from the mask's edge: a streak drawn out along its fall, quickening.
    private static final ParticleSpec DRIP = ParticleSpec.builder(VfxMaterial.alpha(VfxMaterial.STREAK))
            .shape(ParticleSpec.Shape.SPARK)
            .stretch(1.6F)
            .lifetime(16, 22)
            .size(Curve.of(0.03F, 0.026F))
            .opacity(Curve.of(1.0F, 1.0F, 0.6F))
            .color(0xA01426, 0x5A0610)
            .gravity(0.035F)
            .drag(0.98F)
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
                animator.layer("base").drive(ctx -> {
                    if (!(ctx.entity() instanceof GardenerEntity gardener)) return IDLE;
                    if (gardener.isDormant()) return DORMANT;
                    return ctx.moving() ? (gardener.isRunning() ? RUN : WALK) : IDLE;
                });
                animator.layer("action");
                animator.emitter("heart_core", EMBER).rate(10).speed(0.03F).spread(0.02F);
                // Its tears: welling and running down the mask, and falling from its edge.
                animator.emitter("tear_well", TEAR).rate(1.2F).speed(0.0F).spread(0.0F);
                animator.emitter("tear_drip", DRIP).rate(0.9F).speed(0.0F).spread(0.002F);
                animator.trail("blade_tip").length(10).width(Curve.of(0.6F, 0.0F)).colors(0xE0FF2D46, 0x008A0A24)
                        .when(ctx -> animator.isPlaying("swing"));
            });
        });
    }
}
