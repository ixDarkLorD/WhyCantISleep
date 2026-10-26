package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.folios.api.FoliosSources;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.sleep.Bedroom;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;

import java.util.ArrayList;
import java.util.List;

/**
 * The guide book's live list of the music that counts at bedtime ({@code whycantisleep:bedtime_music}, a Folios
 * source): each music disc in one of the mood tags ({@link Bedroom#CALM}, {@link Bedroom#CREEPY}, {@link Bedroom#LOUD};
 * 1.20.1 has no jukebox songs, so they are item tags of discs), with {@code name} (the disc's description, "C418 -
 * cat") and {@code mood} ({@code calm}, {@code creepy} or {@code loud}; a disc in two tags is a row for each). Worked out
 * from the tags each time the page is laid out, so a data pack's tags show in the book.
 */
public final class GuideBookMusic {
    public static final ResourceLocation SOURCE = WhyCantISleep.id("bedtime_music");

    private GuideBookMusic() {
    }

    public static void init() {
        FoliosSources.register(SOURCE, GuideBookMusic::rows);
    }

    private static List<FoliosSources.Row> rows() {
        List<FoliosSources.Row> rows = new ArrayList<>();
        add(rows, Bedroom.CALM, "calm");
        add(rows, Bedroom.CREEPY, "creepy");
        add(rows, Bedroom.LOUD, "loud");
        return rows;
    }

    private static void add(List<FoliosSources.Row> rows, TagKey<Item> tag, String mood) {
        for (Holder<Item> disc : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            if (!(disc.value() instanceof RecordItem record)) continue;
            // The description's translation key, which the page translates; a disc with a plain description, as written.
            Component description = record.getDisplayName();
            String name = description.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : description.getString();
            rows.add(FoliosSources.row(BuiltInRegistries.ITEM.getKey(disc.value()) + "#" + mood).field("name", name).field("mood", mood).build());
        }
    }
}
