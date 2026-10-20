package net.ixdarklord.whycantisleep.between.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxBlend;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.coolcatcore.api.client.registry.BlockEntityRendererRegistry;
import net.ixdarklord.coolcatcore.api.registry.RegistryEntry;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.between.WCISBlocks;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The bloom of the Dark Between's flames: round each dread lantern's candle and each dread fire lamp's fire in view, a soft
 * glow spills out past the glass and the iron, a wide faint haze and a warmer heart, breathing and guttering a little
 * as a flame does, each in its own time.
 * <p>
 * The lights are found by their block entity renderers (which draw nothing themselves): the game asks them only for
 * the ones in view and in range, so each frame they note them, and the world effect pass draws a glow at each.
 */
public final class FlameBloomClient {
    // Hardly softened where it meets things: a light always has a wall, a floor or its own glass close behind its
    // flame, and the default softness faded the glow away against them.
    private static final VfxMaterial GLOW = new VfxMaterial(VfxMaterial.GLOW, VfxBlend.ADDITIVE, 0.05F);
    private static final double FADE_FROM = 24.0, FADE_TO = 40.0;

    /**
     * How one kind of light blooms: where its flame is in its block, how big its glow is (1 for a lantern's candle),
     * and its four layers' colours, outermost first (alpha is each layer's strength).
     */
    private record Bloom(Vec3 flame, float size, int[] layers, List<BlockPos> seen) {
    }

    // A lantern's candle: small, red.
    private static final Bloom LANTERN = new Bloom(new Vec3(0.5, 6.0 / 16.0, 0.5), 1.0F,
            new int[]{0x30FF3228, 0x69FF5A3C, 0x87FF965A, 0xA5FFE1AA}, new ArrayList<>());
    // A fire lamp's fire: bigger, and more of the fire's orange.
    private static final Bloom FIRE_LAMP = new Bloom(new Vec3(0.5, 7.0 / 16.0, 0.5), 1.45F,
            new int[]{0x30FF4626, 0x69FF6E34, 0x87FFA05A, 0xA5FFDCA0}, new ArrayList<>());
    private static final float[] SIZES = {1.45F, 0.78F, 0.4F, 0.2F};
    // A candle's flame on a candelabra: small and warm, its wicks noted by CandelabraClient as it draws them.
    private static final int[] CANDLE = {0x26FFA040, 0x52FFB45A, 0x75FFD08A, 0x96FFF0C8};
    private static final float CANDLE_SIZE = 0.5F;
    private static final List<Vec3> CANDLES = new ArrayList<>();
    // Other glows noted for this frame: where, how big, and their four layers' colours (see glow).
    private record Glow(Vec3 at, float size, int[] layers) {
    }

    private static final List<Glow> GLOWS = new ArrayList<>();

    private FlameBloomClient() {
    }

    public static void init() {
        finder(WCISBlocks.DREAD_LANTERN_ENTITY, LANTERN);
        finder(WCISBlocks.FIRE_LAMP_ENTITY, FIRE_LAMP);
        WorldVfx.register(WhyCantISleep.id("flame_bloom"), FlameBloomClient::render);
    }

    private static <T extends BlockEntity> void finder(RegistryEntry<BlockEntityType<T>> type, Bloom bloom) {
        BlockEntityRendererRegistry.register(type, context -> new Finder<>(bloom));
    }

    /** Notes a candle's flame to bloom this frame (a lit candle on a candelabra, where its wick burns). */
    public static void candle(Vec3 flame) {
        CANDLES.add(flame);
    }

    /** Notes a glow to bloom this frame (a haunted urn's mouth, ...): its size, and four layers' colours, outermost first. */
    public static void glow(Vec3 at, float size, int[] layers) {
        GLOWS.add(new Glow(at, size, layers));
    }

    private static void render(VfxFrame frame) {
        if (LANTERN.seen().isEmpty() && FIRE_LAMP.seen().isEmpty() && CANDLES.isEmpty() && GLOWS.isEmpty()) return;
        VfxDraw draw = frame.draw(GLOW);
        draw(frame, draw, LANTERN);
        draw(frame, draw, FIRE_LAMP);
        for (Vec3 flame : CANDLES) glow(frame, draw, flame, CANDLE_SIZE, CANDLE, flame.hashCode());
        CANDLES.clear();
        for (Glow g : GLOWS) glow(frame, draw, g.at(), g.size(), g.layers(), g.at().hashCode());
        GLOWS.clear();
    }

    private static void draw(VfxFrame frame, VfxDraw draw, Bloom bloom) {
        for (BlockPos pos : bloom.seen()) {
            glow(frame, draw, Vec3.atLowerCornerOf(pos).add(bloom.flame()), bloom.size(), bloom.layers(), pos.hashCode());
        }
        bloom.seen().clear();
    }

    private static void glow(VfxFrame frame, VfxDraw draw, Vec3 at, float size, int[] layers, int hash) {
        Vec3 camera = frame.cameraPosition();
        float time = frame.time();
        double distance = at.distanceTo(camera);
        float seen = 1.0F - (float) Mth.clamp((distance - FADE_FROM) / (FADE_TO - FADE_FROM), 0.0, 1.0);
        if (seen <= 0.0F) return;
        // A flame's breath: two slow waves and a quick flutter, out of step from one light to the next.
        float seed = (hash & 1023) * 0.37F;
        float breath = 0.82F + 0.1F * Mth.sin(time * 2.1F + seed) + 0.05F * Mth.sin(time * 5.3F + seed * 1.7F)
                + 0.03F * Mth.sin(time * 13.0F + seed * 2.3F);
        float strength = seen * breath;
        // Drawn a little toward the eye, so the light spills over the glass and the iron as bloom does.
        Vec3 glow = at.add(camera.subtract(at).normalize().scale(Math.min(0.4 * size, distance * 0.5)));
        for (int i = 0; i < SIZES.length; i++) {
            int layer = layers[i];
            draw.billboard(glow.x, glow.y, glow.z, SIZES[i] * size, 0.0F,
                    ARGB.color(Math.round(ARGB.alpha(layer) * strength), ARGB.red(layer), ARGB.green(layer), ARGB.blue(layer)));
        }
    }

    // Draws nothing: notes each light the game would draw this frame.
    private static final class Finder<T extends BlockEntity> implements BlockEntityRenderer<T, BlockEntityRenderState> {
        private final Bloom bloom;

        Finder(Bloom bloom) {
            this.bloom = bloom;
        }

        @Override
        public BlockEntityRenderState createRenderState() {
            return new BlockEntityRenderState();
        }

        @Override
        public void extractRenderState(T be, BlockEntityRenderState state, float partialTicks, Vec3 cameraPosition,
                                       ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            BlockEntityRenderState.extractBase(be, state, breakProgress);
            this.bloom.seen().add(be.getBlockPos().immutable());
        }

        @Override
        public void submit(BlockEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        }

        @Override
        public int getViewDistance() {
            return (int) FADE_TO;
        }
    }
}
