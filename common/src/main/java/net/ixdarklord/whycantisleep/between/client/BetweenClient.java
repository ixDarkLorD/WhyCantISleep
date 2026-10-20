package net.ixdarklord.whycantisleep.between.client;

import net.ixdarklord.coolcatcanvas.api.client.sky.SkyBlend;
import net.ixdarklord.coolcatcanvas.api.client.sky.SkyLayerDefinition;
import net.ixdarklord.coolcatcanvas.api.client.sky.SkyLayerStage;
import net.ixdarklord.coolcatcanvas.api.client.sky.SkyboxDefinition;
import net.ixdarklord.coolcatcanvas.api.client.sky.Skyboxes;
import net.ixdarklord.coolcatcore.api.event.v2.client.ItemTooltipEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.between.DarkBetween;
import net.ixdarklord.whycantisleep.between.WCISItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The Dark Between's client side. Its sky is a resource ({@code assets/whycantisleep/skybox/dark_between.json}), which
 * CoolCatLib shows by itself there; on top of it, while the player dreams, hangs their Window: a sprite in the sky to
 * the north showing their room live ({@link DreamRoomView}), in a frame drawn by its own shader
 * ({@code shaders/sky/dream_window.fsh}). Being part of the sky, it is the same size and in the same place wherever
 * they stand.
 */
public final class BetweenClient {
    // The rift's sprite is this many times the size of the tear itself, to leave room round it for its glow and the
    // lightning it throws out (shaders/sky/dream_window.fsh has the same number).
    private static final float RIFT_REACH = 2.2F;

    private BetweenClient() {
    }

    public static void init() {
        DreamRoomView.init();
        DreamTransition.init();
        CastleVortex.init();
        DreamerAnimations.init();
        GardenerClient.init();
        HeartPortalClient.init();
        ReliquaryClient.init();
        TombClient.init();
        PlanterClient.init();
        CandelabraClient.init();
        HauntedUrnClient.init();
        // The Dream Glider's glide, moved on the client as the player's own movement is.
        net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents.END.register(minecraft -> {
            if (minecraft.player != null && !minecraft.isPaused()) net.ixdarklord.whycantisleep.between.DreamGlider.glide(minecraft.player);
        });
        LabyrinthMobsClient.init();
        WildsClient.init();
        TearGlowClient.init();
        GardenWardClient.init();
        FlameBloomClient.init();
        LabyrinthAtmosphere.init();
        BetweenSkyClient.init();
        Skyboxes.register(WhyCantISleep.id("dream_window"), SkyboxDefinition.builder()
                        .layer(SkyLayerDefinition.sprite(DreamRoomView.TEXTURE, DarkBetween.WINDOW_SIZE * RIFT_REACH)
                                .name("window")
                                .fragmentShader(WhyCantISleep.id("sky/dream_window"))
                                // Tipped down from overhead to its height above the northern horizon.
                                .orientation(-(90.0F - DarkBetween.WINDOW_ELEVATION), 0.0F, 0.0F)
                                .stage(SkyLayerStage.ABOVE_CELESTIALS)
                                .blend(SkyBlend.ALPHA)
                                .blur(false)
                                .fog(0.0F))
                        .priority(10)
                        .fade(40, 20)
                        .build())
                .activeWhen(context -> context.inWorld() && context.inDimension(DarkBetween.LEVEL) && DreamRoomView.has())
                .layer("window").bindParam(0, (context, out) -> {
                    // x: how lit the room is; y: a slow breath of light over the whole Window. Using the view keeps
                    // it drawn while the Window shows.
                    DreamRoomView.use();
                    out[0] = Mth.clamp(DreamRoomView.daylight(), 0.3F, 1.0F);
                    out[1] = 0.92F + 0.08F * Mth.sin((float) (System.currentTimeMillis() % 100000L) * 0.0015F);
                    // z: how open the rift is; it closes after they arrive and opens when they look for it. Out of
                    // the Dark Between it is shut at once, so nothing of it shows while the sky fades back to the real one.
                    out[2] = context.inDimension(DarkBetween.LEVEL) ? DreamTransition.riftOpen(1.0F) : 0.0F;
                })
                .bindParam(1, (context, out) -> DreamTransition.strike(0.0F, out));

        ItemTooltipEvents.MODIFY.register((stack, context, flag, lines) -> {
            if (stack.is(WCISItems.DREAM_LENS.get())) {
                lines.add(Component.translatable("item.whycantisleep.dream_lens.desc").withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("item.whycantisleep.dream_lens.wake").withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("item.whycantisleep.dream_lens.need", Math.round(DarkBetween.ENTER_LUCIDITY)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (DarkBetween.ORIGIN.get(stack) == DarkBetween.DREAMT) {
                lines.add(Component.translatable("tooltip.whycantisleep.dreamt").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
            }
        });
    }
}
