package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.attachment.Attachment;
import net.ixdarklord.coolcatcore.api.attachment.SyncPolicy;
import net.ixdarklord.whycantisleep.api.SleepEffect;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.TreeSet;

/**
 * The dream journal: every sleep effect a player has woken with, kept with them (a CoolCatLib attachment, saved, sent
 * to them alone, kept through death) as the effects' ids, sorted. The Insomniac's Handbook shows it, the found ones by
 * name and the rest hidden ({@code client/GuideBookDreams}). With the server's {@code dreamJournal} off, nothing is
 * written in it.
 */
public final class DreamJournal {
    /** The sleep effects a player has woken with, by id, sorted. */
    public static final Attachment<List<Identifier>> ENTRIES = DecorDiscovery.ATTACHMENTS.builder("dream_journal", Identifier.CODEC.listOf(),
            List.<Identifier>of()).sync(SyncPolicy.SELF).copyOnDeath().build();

    private DreamJournal() {
    }

    /** Builds the attachment, before the registry it is in is registered. */
    static void touch() {
    }

    /** Whether a player has a sleep effect in their journal. */
    public static boolean has(Player player, Identifier effect) {
        return ENTRIES.get(player).contains(effect);
    }

    /** Writes a sleep effect a player woke with into their journal. Returns whether it is new to it. */
    public static boolean write(ServerPlayer player, SleepEffect effect) {
        if (!WCISServerConfig.DREAM_JOURNAL.get()) return false;
        TreeSet<Identifier> entries = new TreeSet<>(ENTRIES.get(player));
        if (!entries.add(effect.id())) return false;
        ENTRIES.set(player, List.copyOf(entries));
        return true;
    }
}
