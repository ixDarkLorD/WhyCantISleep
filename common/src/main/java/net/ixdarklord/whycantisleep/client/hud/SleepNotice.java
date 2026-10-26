package net.ixdarklord.whycantisleep.client.hud;

import net.ixdarklord.whycantisleep.network.WokePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * One message of the sleep HUD: what happened, and what to do about it.
 *
 * @param hint     the second line, or null for a message of one line
 * @param progress what the line along the card's foot shows, 0 to 1, asked each frame; null for the time the card has
 *                 left (it runs down as the card is about to go)
 * @param iconItem the item the card shows, or null for its kind's
 * @param quality  for a night's sleep, its score and what made it, drawn as a meter and a list; else null
 */
public record SleepNotice(Kind kind, Component title, @Nullable Component hint, @Nullable DoubleSupplier progress, @Nullable ItemStack iconItem,
                          @Nullable Quality quality) {
    public SleepNotice(Kind kind, Component title, @Nullable Component hint) {
        this(kind, title, hint, null, null, null);
    }

    /** The same message, its foot line showing something else than the time it has left. */
    public SleepNotice withProgress(DoubleSupplier progress) {
        return new SleepNotice(this.kind, this.title, this.hint, progress, this.iconItem, this.quality);
    }

    /** The same message, showing an item of its own (the very bed hidden under) instead of its kind's. */
    public SleepNotice withIcon(ItemStack icon) {
        return new SleepNotice(this.kind, this.title, this.hint, this.progress, icon, this.quality);
    }

    /** The same message as a night's sleep: its score as a meter of pips, and the things that made it under it. */
    public SleepNotice withQuality(Quality quality) {
        return new SleepNotice(this.kind, this.title, this.hint, this.progress, this.iconItem, quality);
    }

    /**
     * How well a night was slept, for its card.
     *
     * @param score   0 to 100
     * @param details what made it so, the biggest first
     * @param dream   the sleep effect woken with, or null
     */
    public record Quality(int score, List<Detail> details, @Nullable WokePayload.Dream dream) {
    }

    /** One thing that made a night better or worse, and by how much. */
    public record Detail(Component label, int points) {
    }

    /** The item the card shows. */
    public ItemStack icon() {
        return this.iconItem != null ? this.iconItem : this.kind.icon();
    }

    /** What a notice is about, which gives it its color (one of the chat's) and its icon (one of the game's own items). */
    public enum Kind {
        MONSTERS(HudStyle.RED, () -> Items.ZOMBIE_HEAD),
        WRONG_TIME(HudStyle.YELLOW, () -> Items.CLOCK),
        TOO_FAR(HudStyle.YELLOW, () -> Items.SPYGLASS),
        OBSTRUCTED(HudStyle.YELLOW, () -> Items.COBBLESTONE),
        OCCUPIED(HudStyle.YELLOW, () -> Items.BED.red()),
        PLAYERS_AWAKE(HudStyle.AQUA, () -> Items.PLAYER_HEAD),
        NOT_POSSIBLE(HudStyle.WHITE, () -> Items.BARRIER),
        DANGER(HudStyle.RED, () -> Items.TNT),
        NUDGE(HudStyle.GREEN, () -> Items.BELL),
        HIDING(HudStyle.SILVER, () -> Items.BED.gray()),
        // Hiding, with monsters about.
        HIDING_DANGER(HudStyle.RED, () -> Items.BED.gray()),
        // Too soon to hide again: the cooldown counting down.
        HIDE_COOLDOWN(HudStyle.GOLD, () -> Items.CLOCK),
        // Waking: a good night, a middling one, a bad one; woken early; shaken from a night terror.
        WOKE_GOOD(HudStyle.GREEN, () -> Items.BED.lime()),
        WOKE_FAIR(HudStyle.YELLOW, () -> Items.BED.yellow()),
        WOKE_POOR(HudStyle.RED, () -> Items.BED.red()),
        WOKE_EARLY(HudStyle.GOLD, () -> Items.CLOCK),
        SHAKEN(HudStyle.LIGHT_PURPLE, () -> Items.ENDER_EYE),
        // The monsters that blocked a sleep, all dealt with.
        ROOM_SAFE(HudStyle.GREEN, () -> Items.IRON_SWORD);

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

        /** Whether the card is a hiding player's: how long until they have shaken off what hunts them. */
        public boolean hiding() {
            return this == HIDING || this == HIDING_DANGER;
        }

        /** Whether the card warns of danger, so its wash throbs. */
        public boolean danger() {
            return this == MONSTERS || this == DANGER || this == HIDING_DANGER;
        }

        public ItemStack icon() {
            if (this.icon == null) this.icon = new ItemStack(this.item.get());
            return this.icon;
        }
    }
}
