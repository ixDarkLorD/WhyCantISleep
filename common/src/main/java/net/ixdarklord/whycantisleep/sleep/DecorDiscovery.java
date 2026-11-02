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
 * What a player has found out about the soothing and unsettling things by a bed ({@link SleepDecor}): until they
 * have, an item's tooltip doesn't say what it does. They find out by sleeping a night through with one near the bed,
 * or by studying one with a Dreamcatcher (held in the off hand while they check a bed with it). Kept with the player
 * (CoolCatLib's attachments: saved, sent to them alone, kept through death), as the ids of the items, sorted. With the
 * server's {@code discoverDecor} off, or in creative, everything is known.
 */
public final class DecorDiscovery {
    public static final AttachmentRegistry ATTACHMENTS = AttachmentRegistry.create(WhyCantISleep.MOD_ID);
    /** The items a player has discovered, by id, sorted. */
    public static final Attachment<List<Identifier>> KNOWN = ATTACHMENTS.builder("known_decor", Identifier.CODEC.listOf(), List.<Identifier>of())
            .sync(SyncPolicy.SELF).copyOnDeath().build();

    private DecorDiscovery() {
    }

    public static void register() {
        ATTACHMENTS.register();
    }

    /** Whether a player knows what an item does by a bed: always for an item that does nothing, in creative, or with discovery off. */
    public static boolean knows(Player player, ItemStack stack) {
        if (SleepDecor.points(stack) == 0 || !WCISServerConfig.DISCOVER_DECOR.get() || player.isCreative()) return true;
        return KNOWN.get(player).contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /** Teaches a player these items, those of them that soothe or unsettle; returns the ones new to them, in the order given. */
    public static List<Item> discover(ServerPlayer player, Collection<Item> items) {
        if (!WCISServerConfig.DISCOVER_DECOR.get()) return List.of();
        TreeSet<Identifier> known = new TreeSet<>(KNOWN.get(player));
        List<Item> fresh = new ArrayList<>();
        for (Item item : items) {
            if (SleepDecor.points(new ItemStack(item)) != 0 && known.add(BuiltInRegistries.ITEM.getKey(item))) fresh.add(item);
        }
        if (!fresh.isEmpty()) KNOWN.set(player, List.copyOf(known));
        return fresh;
    }
}
