package net.ixdarklord.whycantisleep.deep;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The dread lantern: a lantern of black iron and smoked red glass with a candle burning in it (its model from
 * tools/gen_decor.py), standing or hanging as a lantern does. It keeps a block entity only so the client can find the
 * ones in view and lay a soft bloom of light round each flame (FlameBloomClient).
 */
public class DreadLanternBlock extends LanternBlock implements EntityBlock {
    public static final MapCodec<LanternBlock> CODEC = simpleCodec(DreadLanternBlock::new);

    public DreadLanternBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<LanternBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DreadLanternBlockEntity(pos, state);
    }
}
