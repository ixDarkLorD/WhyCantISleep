package net.ixdarklord.whycantisleep.compat;

import net.ixdarklord.folios.api.FoliosSources;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.api.SleepEffect;
import net.ixdarklord.whycantisleep.api.SleepEffects;
import net.ixdarklord.whycantisleep.api.SleepModifier;
import net.ixdarklord.whycantisleep.api.SleepModifiers;
import net.ixdarklord.whycantisleep.api.SleepQuality;
import net.ixdarklord.whycantisleep.sleep.DataSleepModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The live lists the guide book shows (Folios sources, sent to each player by Folios as they join and after every data
 * pack reload), so effects and modifiers a data pack adds or changes show in the book with no one editing it:
 * <ul>
 *     <li>{@code whycantisleep:sleep_effects}: every sleep effect, with {@code name} (in its rarity's color),
 *     {@code kind} ({@code positive}, {@code negative}, {@code neutral}), {@code rank} (its rarity, 0 common to 4
 *     legendary), {@code effects} (what it gives) and {@code quality} (the nights it comes after, or nothing);</li>
 *     <li>{@code whycantisleep:sleep_modifiers}: every data pack modifier, with {@code label}, {@code points} (colored,
 *     "+2 each, up to +10"), {@code value} (the most it can be worth, to sort and split by) and {@code hint} (what it
 *     is, for the mod's own) and {@code icon} (an item in a slot, for the mod's own).</li>
 * </ul>
 * The words are markup with {@code {lang:...}} tags, so each reader sees them in their own language.
 */
public final class GuideBookSources {
    public static final ResourceLocation SLEEP_EFFECTS = WhyCantISleep.id("sleep_effects");
    public static final ResourceLocation SLEEP_MODIFIERS = WhyCantISleep.id("sleep_modifiers");
    private static final String KEYS = "guide.whycantisleep.guide.list.";
    // The mod's own modifiers, which have a hint in the lang file (guide.whycantisleep.guide.list.hint.<path>).
    // An item for each of the mod's own modifiers, drawn in a slot before it (Folios's {slot:...}).
    private static final Map<ResourceLocation, String> ICONS = Map.ofEntries(
            Map.entry(SleepQuality.ENCLOSED, "minecraft:oak_door"), Map.entry(SleepQuality.EXPOSED, "minecraft:grass_block"),
            Map.entry(SleepQuality.DARK_CORNERS, "minecraft:black_candle"), Map.entry(SleepQuality.TOO_BRIGHT, "minecraft:glowstone"),
            Map.entry(SleepQuality.MONSTERS, "minecraft:zombie_head"), Map.entry(SleepQuality.CALM_MUSIC, "minecraft:music_disc_cat"),
            Map.entry(SleepQuality.CREEPY_MUSIC, "minecraft:music_disc_11"), Map.entry(SleepQuality.LOUD_MUSIC, "minecraft:music_disc_pigstep"),
            Map.entry(SleepQuality.SOOTHING, "minecraft:poppy"), Map.entry(SleepQuality.UNSETTLING, "minecraft:skeleton_skull"),
            Map.entry(SleepQuality.RESTLESS, "minecraft:clock"), Map.entry(SleepQuality.SLEEPLESS, "minecraft:phantom_membrane"),
            Map.entry(SleepQuality.NIGHTMARE, "minecraft:wither_rose"), Map.entry(SleepQuality.WOKE_EARLY, "minecraft:bell"),
            Map.entry(SleepQuality.ROUTINE, "minecraft:red_bed"));
    private static final Set<ResourceLocation> HINTED = Set.of(SleepQuality.ENCLOSED, SleepQuality.EXPOSED, SleepQuality.DARK_CORNERS,
            SleepQuality.TOO_BRIGHT, SleepQuality.MONSTERS, SleepQuality.CALM_MUSIC, SleepQuality.CREEPY_MUSIC, SleepQuality.LOUD_MUSIC,
            SleepQuality.SOOTHING, SleepQuality.UNSETTLING, SleepQuality.RESTLESS, SleepQuality.SLEEPLESS, SleepQuality.NIGHTMARE,
            SleepQuality.WOKE_EARLY, SleepQuality.ROUTINE);
    // The game's rarity and buff colors are made for dark tooltips: these are mid tones, read on the book's light pages
    // and its dark mode alike (common in the page's own color).
    private static final int[] RARITY = {-1, 0xC9A227, 0x2BA3B5, 0xB05CC8, 0xE07B1F};
    private static final int GOOD = 0x3FA34D;
    private static final int BAD = 0xD9493F;
    // Folios's mark for a {lang:...} tag's last part being its fallback (FoliosText.translation writes the same).
    private static final char FALLBACK = '\uE000';

    private GuideBookSources() {
    }

    public static void init() {
        FoliosSources.registerServer(SLEEP_EFFECTS, server -> effects());
        FoliosSources.registerServer(SLEEP_MODIFIERS, server -> modifiers());
    }

    private static List<FoliosSources.Row> effects() {
        List<FoliosSources.Row> rows = new ArrayList<>();
        for (SleepEffect effect : SleepEffects.all()) {
            List<String> gives = new ArrayList<>();
            for (SleepEffect.Effect one : effect.effects()) gives.add(effect(one));
            rows.add(FoliosSources.row(effect.id().toString())
                    .field("name", "**" + colored(RARITY[effect.rarity().ordinal()], text(effect.name())) + "**")
                    .field("kind", effect.kind().getSerializedName())
                    .field("rank", effect.rarity().ordinal())
                    .field("effects", String.join(", ", gives))
                    .field("quality", quality(effect.minQuality(), effect.maxQuality()))
                    .build());
        }
        return rows;
    }

    // "Speed II, 1 min": the effect's name, its level past the first, and how long it lasts (nothing for an instant).
    private static String effect(SleepEffect.Effect effect) {
        StringBuilder out = new StringBuilder(lang(effect.effect().value().getDescriptionId()));
        if (effect.amplifier() > 0) out.append(' ').append(lang("enchantment.level." + (effect.amplifier() + 1)));
        int seconds = effect.duration() / 20;
        if (seconds >= 60 && seconds % 60 == 0) out.append(", ").append(lang(KEYS + "minutes", seconds / 60));
        else if (seconds > 0) out.append(", ").append(lang(KEYS + "seconds", seconds));
        return out.toString();
    }

    // The nights an effect comes after, " (60+)", " (40 or less)", " (40 to 60)"; nothing when any night will do.
    private static String quality(int min, int max) {
        if (min <= 0 && max >= 100) return "";
        String range = min <= 0 ? lang(KEYS + "quality_below", max) : max >= 100 ? lang(KEYS + "quality_from", min) : lang(KEYS + "quality_between", min, max);
        return " (" + range + ")";
    }

    private static List<FoliosSources.Row> modifiers() {
        List<FoliosSources.Row> rows = new ArrayList<>();
        for (SleepModifier modifier : SleepModifiers.all()) {
            DataSleepModifiers.Worth worth = DataSleepModifiers.worth(modifier);
            // Those registered in code score from the night itself: there is nothing to say of their points.
            if (worth == null) continue;
            int value = worth.each() == 0 ? worth.points() : worth.most() != null ? worth.most() : worth.each();
            rows.add(FoliosSources.row(modifier.id().toString())
                    .field("label", text(modifier.label()))
                    .field("points", points(worth))
                    .field("value", value)
                    .field("icon", ICONS.containsKey(modifier.id()) ? "{slot:" + ICONS.get(modifier.id()) + "} " : "")
                    .field("hint", HINTED.contains(modifier.id()) ? ": " + lang(KEYS + "hint." + modifier.id().getPath()) : "")
                    .build());
        }
        return rows;
    }

    // "+10", or "+2 each, up to +10", in green for a buff and red for a debuff.
    private static String points(DataSleepModifiers.Worth worth) {
        if (worth.each() == 0) return colored(worth.points() >= 0 ? GOOD : BAD, signed(worth.points()));
        int color = worth.each() >= 0 ? GOOD : BAD;
        String each = colored(color, signed(worth.each()));
        if (worth.most() == null) return lang(KEYS + "points_each", each);
        return lang(KEYS + (worth.each() >= 0 ? "points_up_to" : "points_down_to"), each, colored(color, signed(worth.most())));
    }

    private static String signed(int points) {
        return points > 0 ? "+" + points : String.valueOf(points);
    }

    private static String colored(int color, String text) {
        if (color < 0) return text;
        return String.format("{#%06X}%s{/}", color & 0xFFFFFF, text);
    }

    // A translation tag, its %s filled in.
    private static String lang(String key, Object... with) {
        StringBuilder out = new StringBuilder("{lang:").append(key);
        for (Object one : with) out.append('|').append(one);
        return out.append('}').toString();
    }

    // A component as markup: a translation as a tag, so the reader sees their own language (its fallback, or a data
    // pack's plain sentence, shown where the reader has no such key); anything else as written.
    private static String text(Component component) {
        if (component.getContents() instanceof TranslatableContents translatable && translatable.getArgs().length == 0) {
            String fallback = translatable.getFallback() != null ? translatable.getFallback() : translatable.getKey();
            return "{lang:" + translatable.getKey() + "|" + FALLBACK + escape(fallback) + "}";
        }
        return escape(component.getString());
    }

    // Plain text kept from being read as markup.
    private static String escape(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            if ("\\*_~`{}[]|#".indexOf(c) >= 0) out.append('\\');
            out.append(c);
        }
        return out.toString();
    }
}
