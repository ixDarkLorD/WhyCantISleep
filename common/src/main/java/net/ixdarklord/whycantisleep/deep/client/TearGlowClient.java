package net.ixdarklord.whycantisleep.deep.client;

import net.ixdarklord.coolcatcanvas.api.client.vfx.Curve;
import net.ixdarklord.coolcatcanvas.api.client.vfx.ParticleSpec;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldParticles;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.deep.LucidDeep;
import net.ixdarklord.whycantisleep.lurker.LurkerPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;

/**
 * The air over the tears: in the Lucid Deep, where its water (blood red: the garden's puddles of tears, the lake round
 * the weeping tree) lies open to the air, small lights shimmer on its surface, and ash rises off it and drifts away,
 * now and then an ember among it. Like the game's own ambient effects, a handful of random places near the player are
 * looked at each tick, so it costs nothing however much water there is.
 */
public final class TearGlowClient {
    private static final int RANGE = 14;
    private static final int TRIES = 200;
    /** A teardrop (textures/vfx/tear.png, drawn by gen_deep_blocks.py), upright, tinted by the particle's colour. */
    public static final Identifier TEAR = WhyCantISleep.id("textures/vfx/tear.png");
    /** A flake of ash (textures/vfx/ash.png), tinted by the particle's colour. */
    public static final Identifier ASH = WhyCantISleep.id("textures/vfx/ash.png");
    /** A glowing red tear rising slowly: not used over the puddles now, kept for the Deep's other weeping things. */
    public static final ParticleSpec TEAR_DROP = ParticleSpec.builder(VfxMaterial.additive(TEAR))
            .lifetime(50, 90)
            .size(Curve.of(0.06F, 0.2F, 0.0F))
            .opacity(Curve.of(0.0F, 1.0F, 0.0F))
            .color(0xFF6A78, LurkerPalette.RED)
            .drag(0.96F)
            .build();
    // A glint on the surface: it winks up and out again where it is.
    private static final ParticleSpec SHIMMER = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.SPARKLE))
            .lifetime(8, 16)
            .size(Curve.of(0.0F, 0.22F, 0.0F))
            .opacity(Curve.of(0.0F, 1.0F, 0.0F))
            .color(0xFFE0E4, 0xFF7A88)
            .randomRoll()
            .drag(0.8F)
            .build();
    // Ash, dark against the red light, turning as it climbs and slows.
    private static final ParticleSpec ASH_FLAKE = ParticleSpec.builder(VfxMaterial.alpha(ASH))
            .lifetime(60, 120)
            .size(Curve.of(0.07F, 0.1F, 0.06F))
            .opacity(Curve.of(0.0F, 0.85F, 0.0F))
            .color(0x4A3E42, 0x1E1719)
            .randomRoll()
            .spin(Curve.of(0.08F, 0.02F))
            .drag(0.97F)
            .build();
    // An ember among the ash, burning out as it rises.
    private static final ParticleSpec EMBER = ParticleSpec.builder(VfxMaterial.additive(VfxMaterial.GLOW))
            .lifetime(30, 60)
            .size(Curve.of(0.05F, 0.03F, 0.0F))
            .opacity(Curve.of(1.0F, 0.7F, 0.0F))
            .color(0xFFB08A, LurkerPalette.RED)
            .drag(0.96F)
            .build();
    private static final RandomSource RANDOM = RandomSource.create();
    private static final BlockPos.MutableBlockPos AT = new BlockPos.MutableBlockPos();

    private TearGlowClient() {
    }

    public static void init() {
        ClientTickEvents.END.register(TearGlowClient::tick);
    }

    private static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        Player player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused() || !LucidDeep.isDeep(level)) return;
        BlockPos centre = player.blockPosition();
        for (int i = 0; i < TRIES; i++) {
            AT.set(centre.getX() + RANDOM.nextInt(RANGE * 2 + 1) - RANGE, centre.getY() + RANDOM.nextInt(13) - 8,
                    centre.getZ() + RANDOM.nextInt(RANGE * 2 + 1) - RANGE);
            FluidState fluid = level.getFluidState(AT);
            if (!fluid.is(FluidTags.WATER) || !fluid.isSource()) continue;
            AT.move(0, 1, 0);
            boolean open = level.getBlockState(AT).isAir();
            AT.move(0, -1, 0);
            if (!open) continue;
            double x = AT.getX() + RANDOM.nextDouble(), y = AT.getY() + 0.9, z = AT.getZ() + RANDOM.nextDouble();
            float roll = RANDOM.nextFloat();
            if (roll < 0.5F) {
                WorldParticles.spawn(SHIMMER, x, y + 0.02, z, 0.0, 0.0, 0.0);
            } else if (roll < 0.94F) {
                WorldParticles.spawn(ASH_FLAKE, x, y + 0.05, z, (RANDOM.nextDouble() - 0.5) * 0.02, 0.012 + RANDOM.nextDouble() * 0.018,
                        (RANDOM.nextDouble() - 0.5) * 0.02);
            } else {
                WorldParticles.spawn(EMBER, x, y + 0.05, z, (RANDOM.nextDouble() - 0.5) * 0.01, 0.02 + RANDOM.nextDouble() * 0.02,
                        (RANDOM.nextDouble() - 0.5) * 0.01);
            }
        }
    }
}
