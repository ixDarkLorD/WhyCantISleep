package net.ixdarklord.whycantisleep.deep.client;

import net.ixdarklord.coolcatcanvas.api.client.sky.SkyBlend;
import net.ixdarklord.coolcatcanvas.api.client.sky.SkyLayerDefinition;
import net.ixdarklord.coolcatcanvas.api.client.sky.SkyLayerStage;
import net.ixdarklord.coolcatcanvas.api.client.sky.SkyboxDefinition;
import net.ixdarklord.coolcatcanvas.api.client.sky.Skyboxes;
import net.ixdarklord.coolcatcore.api.event.v2.client.ItemTooltipEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.deep.LucidDeep;
import net.ixdarklord.whycantisleep.deep.WCISItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The Lucid Deep's client side. Its sky is a resource ({@code assets/whycantisleep/skybox/lucid_deep.json}), which
 * CoolCatLib shows by itself there; on top of it, while the player dreams, hangs their Window: a sprite in the sky to
 * the north showing their room live ({@link DreamRoomView}), in a frame drawn by its own shader
 * ({@code shaders/sky/dream_window.fsh}). Being part of the sky, it is the same size and in the same place wherever
 * they stand.
 */
public final class DeepClient {
    private DeepClient() {
    }

    public static void init() {
        DreamRoomView.init();
        DreamTransition.init();
        Skyboxes.register(WhyCantISleep.id("dream_window"), SkyboxDefinition.builder()
                        .layer(SkyLayerDefinition.sprite(DreamRoomView.TEXTURE, LucidDeep.WINDOW_SIZE)
                                .name("window")
                                .fragmentShader(WhyCantISleep.id("sky/dream_window"))
                                // Tipped down from overhead to its height above the northern horizon.
                                .orientation(-(90.0F - LucidDeep.WINDOW_ELEVATION), 0.0F, 0.0F)
                                .stage(SkyLayerStage.ABOVE_CELESTIALS)
                                .blend(SkyBlend.ALPHA)
                                .blur(false)
                                .fog(0.0F))
                        .priority(10)
                        .fade(40, 20)
                        .build())
                .activeWhen(context -> context.inWorld() && context.inDimension(LucidDeep.LEVEL) && DreamRoomView.has())
                .layer("window").bindParam(0, (context, out) -> {
                    // x: how lit the room is; y: a slow breath of light over the whole Window. Using the view keeps
                    // it drawn while the Window shows.
                    DreamRoomView.use();
                    out[0] = Mth.clamp(DreamRoomView.daylight(), 0.3F, 1.0F);
                    out[1] = 0.92F + 0.08F * Mth.sin((float) (System.currentTimeMillis() % 100000L) * 0.0015F);
                    // z: how open the rift is; it closes after they arrive and opens when they look for it.
                    out[2] = DreamTransition.riftOpen(1.0F);
                });

        ItemTooltipEvents.MODIFY.register((stack, context, flag, lines) -> {
            if (stack.is(WCISItems.DREAM_LENS.get())) {
                lines.add(Component.translatable("item.whycantisleep.dream_lens.desc").withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("item.whycantisleep.dream_lens.wake").withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable("item.whycantisleep.dream_lens.need", Math.round(LucidDeep.ENTER_LUCIDITY)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (LucidDeep.ORIGIN.get(stack) == LucidDeep.DREAMT) {
                lines.add(Component.translatable("tooltip.whycantisleep.dreamt").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
            }
        });
    }
}
