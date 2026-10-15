package net.ixdarklord.whycantisleep.deep;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Kept by a stone tomb's head-left part, for its lid: the client draws the lid there, closed or sliding aside. */
public class TombBlockEntity extends BlockEntity {
    public TombBlockEntity(BlockPos pos, BlockState state) {
        super(WCISBlocks.TOMB_ENTITY.get(), pos, state);
    }
}
