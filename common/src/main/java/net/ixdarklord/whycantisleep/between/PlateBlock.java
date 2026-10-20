package net.ixdarklord.whycantisleep.between;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A dread pewter plate for the haunted table, turned to face whoever sets it down, holding nothing, a pile of bones,
 * or a cut of rotten meat ({@link #SERVING}); used with an empty hand, it is served round them in turn.
 */
public class PlateBlock extends DecorBlock {
    public static final MapCodec<PlateBlock> CODEC = simpleCodec(PlateBlock::new);
    public static final EnumProperty<Serving> SERVING = EnumProperty.create("serving", Serving.class);

    public PlateBlock(BlockBehaviour.Properties properties) {
        super(properties, Block.box(3, 0, 3, 13, 3, 13), false);
        this.registerDefaultState(this.defaultBlockState().setValue(SERVING, Serving.EMPTY));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Used with an empty hand, it is served round: nothing, bones, meat, and nothing again.
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(SERVING), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BONE_BLOCK_PLACE, SoundSource.BLOCKS, 0.6F, 1.3F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SERVING);
    }

    public enum Serving implements StringRepresentable {
        EMPTY("empty"), BONES("bones"), MEAT("meat");

        private final String name;

        Serving(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
