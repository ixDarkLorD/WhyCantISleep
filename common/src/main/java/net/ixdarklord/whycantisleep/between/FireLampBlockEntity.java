package net.ixdarklord.whycantisleep.between;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Kept by a dread fire lamp's top, so the client knows where its fire is, for the bloom round it (FlameBloomClient). */
public class FireLampBlockEntity extends BlockEntity {
    public FireLampBlockEntity(BlockPos pos, BlockState state) {
        super(WCISBlocks.FIRE_LAMP_ENTITY.get(), pos, state);
    }
}
