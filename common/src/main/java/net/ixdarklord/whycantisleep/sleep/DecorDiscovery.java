package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.coolcatcore.api.attachment.Attachment;
import net.ixdarklord.coolcatcore.api.attachment.AttachmentRegistry;
import net.ixdarklord.coolcatcore.api.attachment.SyncPolicy;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * What a player has found out about the soothing and unsettling things by a bed ({@link SleepDecor}), in two steps.
 * A thing they have slept a night near is <em>noticed</em>: its tooltip says it does something to sleep, not what. A
 * thing they have slept a night near with a Dreamcatcher in hand is <em>known</em>: its tooltip says what it does, and
 * the Handbook names it. Before either, its tooltip says nothing of sleep. Kept with the player (CoolCatLib's
 * attachments: saved, sent to them alone, kept through death), as the ids of the items, sorted. With the server's
 * {@code discoverDecor} off, or in creative, everything is known.
 */
public final class DecorDiscovery {
    public static final AttachmentRegistry ATTACHMENTS = AttachmentRegistry.create(WhyCantISleep.MOD_ID);
    /** The items a player knows the effect of, by id, sorted. */
    public static final Attachment<List<Identifier>> KNOWN = ATTACHMENTS.builder("known_decor", Identifier.CODEC.listOf(), List.<Identifier>of())
            .sync(SyncPolicy.SELF).copyOnDeath().build();
    /** The items a player has slept near without knowing their effect, by id, sorted. */
    public static final Attachment<List<Identifier>> NOTICED = ATTACHMENTS.builder("noticed_decor", Identifier.CODEC.listOf(), List.<Identifier>of())
            .sync(SyncPolicy.SELF).copyOnDeath().build();

    private DecorDiscovery() {
    }

    public static void register() {
        ATTACHMENTS.register();
    }

    /** Whether a player knows what an item does by a bed: always for an item that does nothing, in creative, or with discovery off. */
    public static boolean knows(Player player, ItemStack stack) {
        if (SleepDecor.points(stack) == 0 || everythingKnown(player)) return true;
        return KNOWN.get(player).contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** Whether a player has at least slept near an item, so knows it does something to sleep. */
    public static boolean noticed(Player player, ItemStack stack) {
        return knows(player, stack) || NOTICED.get(player).contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** Whether discovery is off for this player: the server's option, or creative. */
    public static boolean everythingKnown(Player player) {
        return !WCISServerConfig.DISCOVER_DECOR.get() || player.isCreative();
    }

    /**
     * A night slept through near these items: with a Dreamcatcher in hand they become known, else noticed. Returns
     * those new to the player, in the order given.
     */
    public static Learned slept(ServerPlayer player, Collection<Item> items, boolean dreamcatcher) {
        if (everythingKnown(player)) return Learned.NOTHING;
        TreeSet<Identifier> known = new TreeSet<>(KNOWN.get(player));
        TreeSet<Identifier> noticed = new TreeSet<>(NOTICED.get(player));
        List<Item> learned = new ArrayList<>();
        List<Item> sensed = new ArrayList<>();
        for (Item item : items) {
            if (SleepDecor.points(new ItemStack(item)) == 0) continue;
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (dreamcatcher) {
                if (known.add(id)) learned.add(item);
                noticed.remove(id);
            } else if (!known.contains(id) && noticed.add(id)) {
                sensed.add(item);
            }
        }
        if (!learned.isEmpty()) KNOWN.set(player, List.copyOf(known));
        if (!learned.isEmpty() || !sensed.isEmpty()) NOTICED.set(player, List.copyOf(noticed));
        return new Learned(learned, sensed);
    }

    /** What a night taught: items now known, and items now noticed. */
    public record Learned(List<Item> known, List<Item> noticed) {
        public static final Learned NOTHING = new Learned(List.of(), List.of());
    }
}
