package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.folios.api.FoliosSources;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.compat.GuideBookSources;
import net.ixdarklord.whycantisleep.sleep.DreamJournal;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The dream journal's page in the guide book: a client source, {@code whycantisleep:dream_journal}, of every sleep
 * effect the server knows ({@code whycantisleep:sleep_effects}, as the server sent it), each row's markup its
 * {@code show} field: a "3 of 22 found" row first, then the effects in the order the server gives them, those the player
 * has woken with by their name in their rarity's color, the rest as a muted {@code ???}. Written again when the journal
 * changes.
 */
public final class GuideBookDreams {
    public static final Identifier JOURNAL = WhyCantISleep.id("dream_journal");
    private static final String HIDDEN = "{#8C8273}???{/}";

    private GuideBookDreams() {
    }

    public static void init() {
        FoliosSources.register(JOURNAL, GuideBookDreams::rows);
        DreamJournal.ENTRIES.onChange((holder, before, after) -> {
            if (holder.isClientSide()) FoliosSources.changed(JOURNAL);
        });
    }

    private static List<FoliosSources.Row> rows() {
        var player = Minecraft.getInstance().player;
        List<FoliosSources.Row> effects = FoliosSources.rows(GuideBookSources.SLEEP_EFFECTS);
        List<FoliosSources.Row> rows = new ArrayList<>();
        int found = 0;
        List<FoliosSources.Row> lines = new ArrayList<>();
        for (FoliosSources.Row effect : effects) {
            boolean has = player != null && DreamJournal.has(player, Identifier.parse(effect.id()));
            if (has) found++;
            lines.add(FoliosSources.row(effect.id()).field("show", has ? effect.fields().getOrDefault("name", HIDDEN) : HIDDEN).build());
        }
        rows.add(FoliosSources.row("count").field("show",
                "{lang:guide.whycantisleep.guide.list.dreams_found|" + found + "|" + effects.size() + "}").build());
        rows.addAll(lines);
        return rows;
    }
}
