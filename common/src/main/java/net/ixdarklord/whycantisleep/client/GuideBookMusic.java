package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.folios.api.FoliosSources;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.sleep.Bedroom;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.JukeboxSong;

import java.util.ArrayList;
import java.util.List;

/**
 * The guide book's live list of the music that counts at bedtime ({@code whycantisleep:bedtime_music}, a Folios
 * source): each jukebox song in one of the mood tags ({@link Bedroom#CALM}, {@link Bedroom#CREEPY}, {@link Bedroom#LOUD}),
 * with {@code name} (the song's description, "C418 - cat") and {@code mood} ({@code calm}, {@code creepy} or
 * {@code loud}; a song in two tags is a row for each) and {@code disc} (the disc that plays it, as an item slot
 * ({@code {slot:...}}), or nothing for a song no disc plays). Worked out from the world's songs and their tags each
 * time the page is laid out, so a data pack's songs and tags show in the book.
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
        var level = Minecraft.getInstance().level;
        if (level == null) return rows;
        var songs = level.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG);
        songs.listElements().forEach(song -> {
            String disc = disc(level.registryAccess(), song);
            add(rows, song, disc, Bedroom.CALM, "calm");
            add(rows, song, disc, Bedroom.CREEPY, "creepy");
            add(rows, song, disc, Bedroom.LOUD, "loud");
        });
        return rows;
    }

    // The disc that plays a song, as an item slot: the first item whose own components say it plays it.
    private static String disc(HolderLookup.Provider registries, Holder.Reference<JukeboxSong> song) {
        for (Item item : BuiltInRegistries.ITEM) {
            JukeboxPlayable playable = item.components().get(DataComponents.JUKEBOX_PLAYABLE);
            if (playable != null && playable.song().unwrap(registries).filter(holder -> holder.is(song.key())).isPresent()) return "{slot:" + BuiltInRegistries.ITEM.getKey(item) + "} ";
        }
        return "";
    }

    private static void add(List<FoliosSources.Row> rows, Holder.Reference<JukeboxSong> song, String disc, TagKey<JukeboxSong> tag, String mood) {
        if (!song.is(tag)) return;
        // The description's translation key, which the page translates; a song with a plain description, as written.
        String name = song.value().description().getContents() instanceof TranslatableContents translatable
                ? translatable.getKey() : song.value().description().getString();
        rows.add(FoliosSources.row(song.key().location() + "#" + mood).field("name", name).field("mood", mood).field("disc", disc).build());
    }
}
