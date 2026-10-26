package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.folios.api.FoliosSources;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.sleep.Bedroom;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.JukeboxSong;

import java.util.ArrayList;
import java.util.List;

/**
 * The guide book's live list of the music that counts at bedtime ({@code whycantisleep:bedtime_music}, a Folios
 * source): each jukebox song in one of the mood tags ({@link Bedroom#CALM}, {@link Bedroom#CREEPY}, {@link Bedroom#LOUD}),
 * with {@code name} (the song's description, "C418 - cat") and {@code mood} ({@code calm}, {@code creepy} or
 * {@code loud}; a song in two tags is a row for each). Worked out from the world's songs and their tags each time the
 * page is laid out, so a data pack's songs and tags show in the book.
 */
public final class GuideBookMusic {
    public static final Identifier SOURCE = WhyCantISleep.id("bedtime_music");

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
            add(rows, song, Bedroom.CALM, "calm");
            add(rows, song, Bedroom.CREEPY, "creepy");
            add(rows, song, Bedroom.LOUD, "loud");
        });
        return rows;
    }

    private static void add(List<FoliosSources.Row> rows, Holder.Reference<JukeboxSong> song, TagKey<JukeboxSong> tag, String mood) {
        if (!song.is(tag)) return;
        // The description's translation key, which the page translates; a song with a plain description, as written.
        String name = song.value().description().getContents() instanceof TranslatableContents translatable
                ? translatable.getKey() : song.value().description().getString();
        rows.add(FoliosSources.row(song.key().identifier() + "#" + mood).field("name", name).field("mood", mood).build());
    }
}
