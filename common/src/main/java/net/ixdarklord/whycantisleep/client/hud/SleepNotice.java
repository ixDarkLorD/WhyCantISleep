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
    /** What a notice is about, which gives it its color and its icon (one of the game's own items). */
    public enum Kind {
        MONSTERS(0xFF5555, () -> Items.ZOMBIE_HEAD),
        WRONG_TIME(0xFFC94D, () -> Items.CLOCK),
        TOO_FAR(0x66B3FF, () -> Items.SPYGLASS),
        OBSTRUCTED(0xFFA64D, () -> Items.COBBLESTONE),
        OCCUPIED(0xFFA64D, () -> Items.RED_BED),
        PLAYERS_AWAKE(0x66B3FF, () -> Items.PLAYER_HEAD),
        NOT_POSSIBLE(0xA6A6B8, () -> Items.BARRIER),
        DANGER(0xFF4040, () -> Items.TNT),
        NUDGE(0x6FD06F, () -> Items.BELL);

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
