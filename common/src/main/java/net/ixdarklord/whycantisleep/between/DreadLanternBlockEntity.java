package net.ixdarklord.whycantisleep.between;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Kept by a dread lantern so the client knows where its flame is, for the bloom round it (FlameBloomClient). */
public class DreadLanternBlockEntity extends BlockEntity {
    public DreadLanternBlockEntity(BlockPos pos, BlockState state) {
        super(WCISBlocks.DREAD_LANTERN_ENTITY.get(), pos, state);
    }
}
