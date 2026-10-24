package net.ixdarklord.whycantisleep.config;

import net.ixdarklord.coolcatcore.api.config.type.EnumType;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** How the hiding squint's eyelids are drawn: each has its own textures, {@code hud/eyelids/<name>}. */
public enum EyelidStyle implements EnumType.Displayable {
    SMOOTH,
    STEPPED,
    DITHERED,
    PIXEL;

    /** The name of its textures. */
    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Component displayName() {
        return Component.translatable("config.whycantisleep.eyelid_style." + this.id());
    }
}
