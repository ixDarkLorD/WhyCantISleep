package net.ixdarklord.whycantisleep.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.ToIntFunction;

/**
 * Something that makes a night's sleep better (a buff, points above 0) or worse (a debuff, below 0): a closed room,
 * prowling monsters, creepy music. As each sleeper wakes, every modifier ({@link SleepModifiers}) scores the night from
 * its {@link SleepContext}; those with a say become the night's {@link SleepQuality.Factor}s, shown on the morning card
 * with a buff or debuff icon. Data packs describe them with no code ({@link SleepModifiers} has the format); this is
 * for mods that need more.
 * <pre>{@code
 * SleepModifiers.register(SleepModifier.of(MyMod.id("feather_pillow"), Component.translatable("mymod.modifier.feather_pillow"),
 *         context -> hasFeatherPillow(context.player(), context.bed()) ? 8 : 0));
 * }</pre>
 */
public interface SleepModifier {
    /** What it is; registering another with the same id replaces it. */
    Identifier id();

    /** How a player is told of it, on the morning card and in the sleep status: a name, in Title Case. */
    Component label();

    /** What it makes of a night: points to add (a buff) or take (a debuff), or 0 for no say at all. */
    int points(SleepContext context);

    /** A modifier from a function of the night. */
    static SleepModifier of(Identifier id, Component label, ToIntFunction<SleepContext> points) {
        return new SleepModifier() {
            @Override
            public Identifier id() {
                return id;
            }

            @Override
            public Component label() {
                return label;
            }

            @Override
            public int points(SleepContext context) {
                return points.applyAsInt(context);
            }
        };
    }
}
