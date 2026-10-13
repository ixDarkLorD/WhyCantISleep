package net.ixdarklord.whycantisleep.client.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * One message of the sleep HUD: what happened, and what to do about it.
 *
 * @param hint the second line, or null for a message of one line
 */
public record SleepNotice(Kind kind, Component title, @Nullable Component hint) {
    /** What a notice is about, which gives it its color (one of the chat's) and its icon (one of the game's own items). */
    public enum Kind {
        MONSTERS(HudStyle.RED, () -> Items.ZOMBIE_HEAD),
        WRONG_TIME(HudStyle.YELLOW, () -> Items.CLOCK),
        TOO_FAR(HudStyle.YELLOW, () -> Items.SPYGLASS),
        OBSTRUCTED(HudStyle.YELLOW, () -> Items.COBBLESTONE),
        OCCUPIED(HudStyle.YELLOW, () -> Items.RED_BED),
        PLAYERS_AWAKE(HudStyle.AQUA, () -> Items.PLAYER_HEAD),
        NOT_POSSIBLE(HudStyle.WHITE, () -> Items.BARRIER),
        DANGER(HudStyle.RED, () -> Items.TNT),
        NUDGE(HudStyle.GREEN, () -> Items.BELL),
        MOOD(HudStyle.YELLOW, () -> Items.SUNFLOWER),
        SANITY(HudStyle.LIGHT_PURPLE, () -> Items.ENDER_EYE);

        private final int color;
        private final Supplier<Item> item;
        private @Nullable ItemStack icon;

        Kind(int color, Supplier<Item> item) {
            this.color = color;
            this.item = item;
        }

        /** The accent color, as {@code 0xRRGGBB}. */
        public int color() {
            return this.color;
        }

        public ItemStack icon() {
            if (this.icon == null) this.icon = new ItemStack(this.item.get());
            return this.icon;
        }
    }
}
