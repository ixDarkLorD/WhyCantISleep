package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.folios.api.FoliosSources;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.client.hud.SleepTooltip;
import net.ixdarklord.whycantisleep.sleep.DecorDiscovery;
import net.ixdarklord.whycantisleep.sleep.SleepDecor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The guide book's live lists of the soothing and unsettling things ({@code whycantisleep:soothing_things} and
 * {@code whycantisleep:unsettling_things}, Folios sources), as the reader has found them out ({@link DecorDiscovery}):
 * a first row of how many they know of how many there are, then each known one by name (its slot and name,
 * {@code {item:...}}), then a muted {@code ???} for each still hidden. Each row's markup is its {@code show} field.
 * Written again whenever the reader learns something.
 */
public final class GuideBookDecor {
    public static final Identifier SOOTHING = WhyCantISleep.id("soothing_things");
    public static final Identifier UNSETTLING = WhyCantISleep.id("unsettling_things");
    private static final String HIDDEN = "{#8C8273}???{/}";

    private GuideBookDecor() {
    }

    public static void init() {
        FoliosSources.register(SOOTHING, () -> rows(SleepDecor.SOOTHING, true));
        FoliosSources.register(UNSETTLING, () -> rows(SleepDecor.UNSETTLING, false));
        DecorDiscovery.KNOWN.onChange((holder, before, after) -> {
            if (holder.isClientSide()) {
                FoliosSources.changed(SOOTHING);
                FoliosSources.changed(UNSETTLING);
            }
        });
    }

    private static List<FoliosSources.Row> rows(TagKey<Item> tag, boolean soothing) {
        // The items of the tag that do what the list says (an item in both tags unsettles).
        List<Item> known = new ArrayList<>();
        int hidden = 0;
        for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            ItemStack stack = new ItemStack(holder.value());
            if (SleepDecor.points(stack) > 0 != soothing) continue;
            if (SleepTooltip.known(stack)) known.add(holder.value());
            else hidden++;
        }
        known.sort(Comparator.comparing(item -> new ItemStack(item).getHoverName().getString()));
        List<FoliosSources.Row> rows = new ArrayList<>();
        rows.add(FoliosSources.row("count").field("show",
                "{lang:guide.whycantisleep.guide.list.discovered|" + known.size() + "|" + (known.size() + hidden) + "}").build());
        for (Item item : known) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            rows.add(FoliosSources.row(id).field("show", "{item:" + id + "}").build());
        }
        for (int i = 0; i < hidden; i++) rows.add(FoliosSources.row("hidden#" + i).field("show", HIDDEN).build());
        return rows;
    }
}
