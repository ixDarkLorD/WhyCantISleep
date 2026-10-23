package net.ixdarklord.whycantisleep.client.hud;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.ixdarklord.coolcatcore.api.utils.ARGB;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The HUD's sprites on 1.20.1, which has no GUI sprite atlas: each is its own texture under
 * {@code textures/gui/sprites/}, as the later versions keep them, and is stretched or nine-sliced to any size by what
 * its {@code .mcmeta} says ({@code gui.scaling}, the later versions' format), tinted with a color. A resource pack can
 * still restyle them. Their sizes and scaling are read once, and again after a resource reload. The few vanilla sprites
 * the mod borrows are parts of 1.20.1's sheets ({@link #VANILLA}).
 */
public final class GuiSprites {
    private static final Map<ResourceLocation, Sprite> SPRITES = new HashMap<>();
    // The later versions' vanilla sprites the HUD uses, where they are on 1.20.1's sheets.
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final ResourceLocation TOASTS = new ResourceLocation("textures/gui/toasts.png");
    private static final Map<ResourceLocation, Sprite> VANILLA = Map.of(
            new ResourceLocation("hud/heart/container"), new Sprite(ICONS, 16, 0, 9, 9, 256, 256, 0),
            new ResourceLocation("hud/heart/container_blinking"), new Sprite(ICONS, 25, 0, 9, 9, 256, 256, 0),
            new ResourceLocation("hud/heart/full"), new Sprite(ICONS, 52, 0, 9, 9, 256, 256, 0),
            new ResourceLocation("hud/heart/full_blinking"), new Sprite(ICONS, 70, 0, 9, 9, 256, 256, 0),
            new ResourceLocation("toast/mouse"), new Sprite(TOASTS, 196, 0, 20, 20, 256, 256, 0));

    private GuiSprites() {
    }

    /** Forgets what was read of the sprites, for a resource reload. */
    public static void clear() {
        SPRITES.clear();
    }

    /** Draws a sprite stretched or nine-sliced to a size. */
    public static void blit(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
        blit(graphics, sprite, x, y, width, height, 0xFFFFFFFF);
    }

    /** Draws a sprite stretched or nine-sliced to a size, tinted with a color (its alpha the opacity). */
    public static void blit(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0 || ARGB.alpha(color) == 0) return;
        Sprite info = SPRITES.computeIfAbsent(sprite, GuiSprites::read);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(ARGB.redFloat(color), ARGB.greenFloat(color), ARGB.blueFloat(color), ARGB.alphaFloat(color));
        if (info.border <= 0) {
            graphics.blit(info.texture, x, y, width, height, info.u, info.v, info.width, info.height, info.textureWidth, info.textureHeight);
        } else {
            nineSlice(graphics, info, x, y, width, height);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    // The corners as they are, the edges stretched along, the middle stretched both ways.
    private static void nineSlice(GuiGraphics graphics, Sprite info, int x, int y, int width, int height) {
        int b = info.border;
        int left = Math.min(b, width / 2);
        int top = Math.min(b, height / 2);
        int right = Math.min(b, width - left);
        int bottom = Math.min(b, height - top);
        int innerW = width - left - right;
        int innerH = height - top - bottom;
        int srcInnerW = info.width - b * 2;
        int srcInnerH = info.height - b * 2;
        int[] xs = {x, x + left, x + width - right};
        int[] ws = {left, innerW, right};
        int[] us = {0, b, info.width - right};
        int[] uws = {left, srcInnerW, right};
        int[] ys = {y, y + top, y + height - bottom};
        int[] hs = {top, innerH, bottom};
        int[] vs = {0, b, info.height - bottom};
        int[] vhs = {top, srcInnerH, bottom};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (ws[col] <= 0 || hs[row] <= 0 || uws[col] <= 0 || vhs[row] <= 0) continue;
                graphics.blit(info.texture, xs[col], ys[row], ws[col], hs[row], info.u + us[col], info.v + vs[row], uws[col], vhs[row], info.textureWidth,
                        info.textureHeight);
            }
        }
    }

    private static Sprite read(ResourceLocation sprite) {
        Sprite vanilla = VANILLA.get(sprite);
        if (vanilla != null) return vanilla;
        ResourceLocation texture = sprite.withPath(path -> "textures/gui/sprites/" + path + ".png");
        int width = 16;
        int height = 16;
        int border = 0;
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
        if (resource.isPresent()) {
            try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                width = image.getWidth();
                height = image.getHeight();
            } catch (Exception e) {
                WhyCantISleep.LOGGER.warn("Couldn't read the HUD sprite {}", texture, e);
            }
            try {
                JsonObject scaling = resource.get().metadata().getSection(ScalingSection.INSTANCE).orElse(null);
                if (scaling != null && "nine_slice".equals(GsonHelper.getAsString(scaling, "type", "stretch"))) {
                    // The slices are laid out for a sprite of the given size; this one may be drawn larger.
                    int base = GsonHelper.getAsInt(scaling, "width", width);
                    border = Math.round(GsonHelper.getAsInt(scaling, "border", 0) * (width / (float) Math.max(1, base)));
                }
            } catch (Exception e) {
                WhyCantISleep.LOGGER.warn("Couldn't read the HUD sprite {}'s scaling", texture, e);
            }
        }
        return new Sprite(texture, 0, 0, width, height, width, height, border);
    }

    // A sprite: a part of a texture (all of it, for the mod's own), and its nine-slice border, if any.
    private record Sprite(ResourceLocation texture, int u, int v, int width, int height, int textureWidth, int textureHeight, int border) {
    }

    // The {@code gui} section of a sprite's mcmeta, as the later versions read it: only its scaling is used here.
    private static final class ScalingSection implements net.minecraft.server.packs.metadata.MetadataSectionSerializer<JsonObject> {
        private static final ScalingSection INSTANCE = new ScalingSection();

        @Override
        public String getMetadataSectionName() {
            return "gui";
        }

        @Override
        public JsonObject fromJson(JsonObject json) {
            return GsonHelper.getAsJsonObject(json, "scaling", new JsonObject());
        }
    }
}
