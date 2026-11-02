package net.ixdarklord.whycantisleep.client;

import net.ixdarklord.coolcatcore.api.event.v2.common.InteractionEvents;
import net.ixdarklord.whycantisleep.client.hud.SleepNotice;
import net.ixdarklord.whycantisleep.client.hud.SleepNotices;
import net.ixdarklord.whycantisleep.config.WCISClientConfig;
import net.ixdarklord.whycantisleep.config.WCISConfigs;
import net.ixdarklord.whycantisleep.sleep.BedNaming;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Naming a bed, on the client ({@link BedNaming}): the name goes to the beds the player keeps ({@link BedBookmarks}),
 * and a card says so. Answered on the client too, so it doesn't predict lying down.
 */
public final class BedNamingClient {
    private BedNamingClient() {
    }

    public static void init() {
        InteractionEvents.USE_BLOCK.register((player, hand, hit) -> {
            if (!player.level().isClientSide() || !SleepStatus.serverHasMod() || !WCISConfigs.isEnabled(WCISClientConfig.BED_BOOKMARKS)) {
                return InteractionResult.PASS;
            }
            String name = BedNaming.name(player, hand);
            BlockState state = player.level().getBlockState(hit.getBlockPos());
            if (name == null || !(state.getBlock() instanceof BedBlock)) return InteractionResult.PASS;
            BlockPos head = state.getValue(BedBlock.PART) == BedPart.HEAD ? hit.getBlockPos().immutable()
                    : hit.getBlockPos().relative(state.getValue(BedBlock.FACING));
            BedBookmarks.name(GlobalPos.of(player.level().dimension(), head), name);
            SleepNotices.show(new SleepNotice(SleepNotice.Kind.BED_CHECK, Component.translatable("gui.whycantisleep.bed_named.title", name),
                    Component.translatable("gui.whycantisleep.bed_named.hint")).withIcon(new ItemStack(state.getBlock().asItem())));
            return InteractionResult.SUCCESS;
        });
    }
}
