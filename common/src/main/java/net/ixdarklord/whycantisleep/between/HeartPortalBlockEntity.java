package net.ixdarklord.whycantisleep.between;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Kept by the heart portal's middle block, for its rift: the client opens a rift there while the portal is awake and
 * closes it when the portal sleeps or goes ({@code between.client.HeartPortalClient} sets the hooks).
 */
public class HeartPortalBlockEntity extends BlockEntity {
    /** Each client tick of a portal's middle block. Set by the client. */
    public static Consumer<HeartPortalBlockEntity> clientTick = be -> {
    };
    /** When a portal's middle block goes on the client (broken, or its chunk unloaded). Set by the client. */
    public static Consumer<HeartPortalBlockEntity> clientRemoved = be -> {
    };

    public HeartPortalBlockEntity(BlockPos pos, BlockState state) {
        super(WCISBlocks.HEART_PORTAL_ENTITY.get(), pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && this.level.isClientSide()) clientRemoved.accept(this);
    }
}
