package net.ixdarklord.whycantisleep.compat;

import net.ixdarklord.coolcatcore.api.client.registry.ItemRendererRegistry;
import net.ixdarklord.folios.internal.client.GuideBookItemModel;

/**
 * Draws the guide book item as Folios draws its own: on 1.21.1 a book's cover is drawn in code (its model is
 * {@code builtin/entity}, as {@code models/item/guide_book.json} makes ours), tinted and lettered from the book its
 * stack names. Folios-1.21 registers that renderer for its own item only and has no API for a mod's item, so its
 * renderer ({@code GuideBookItemModel}, an internal class) is registered for this one too. Client only.
 */
public final class GuideBookModel {
    private GuideBookModel() {
    }

    public static void register() {
        ItemRendererRegistry.register(WCISItems.GUIDE_BOOK, GuideBookItemModel::render);
    }
}
