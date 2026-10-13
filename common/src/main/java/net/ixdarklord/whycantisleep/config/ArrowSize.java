package net.ixdarklord.whycantisleep.config;

import net.ixdarklord.coolcatcore.api.config.type.EnumType;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** How large the off-screen arrows are drawn. */
public enum ArrowSize implements EnumType.Displayable {
    SMALL(0.75F),
    MEDIUM(1.0F),
    LARGE(1.5F);

    private final float scale;

    ArrowSize(float scale) {
        this.scale = scale;
    }

    public float scale() {
        return this.scale;
    }

    @Override
    public Component displayName() {
        return Component.translatable("config.whycantisleep.arrow_size." + this.name().toLowerCase(Locale.ROOT));
    }
}
