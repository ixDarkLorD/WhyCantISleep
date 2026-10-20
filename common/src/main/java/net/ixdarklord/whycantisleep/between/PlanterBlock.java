package net.ixdarklord.whycantisleep.between;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The stone planter: any flower used on it is set in its tray at a random free place ({@link PlanterBlockEntity}), up to
 * six; used with an empty hand, the flower set last comes back out. Broken, it lets its flowers fall.
 */
public class PlanterBlock extends DecorBlock implements EntityBlock {
    public static final MapCodec<PlanterBlock> CODEC = simpleCodec(properties -> new PlanterBlock(properties, Shapes.block()));

    public PlanterBlock(BlockBehaviour.Properties properties, VoxelShape north) {
        super(properties, north, false);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlanterBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!PlanterBlockEntity.plantable(stack) || !(level.getBlockEntity(pos) instanceof PlanterBlockEntity planter) || planter.isFull()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Block flower = ((BlockItem) stack.getItem()).getBlock();
        if (!planter.plant(flower, level.getRandom())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        stack.consume(1, player);
        SoundType sound = flower.defaultBlockState().getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof PlanterBlockEntity planter)
                || planter.flowers().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        PlanterBlockEntity.Flower flower = planter.takeLast();
        if (flower == null) return InteractionResult.PASS;
        ItemStack taken = new ItemStack(flower.block());
        if (!player.addItem(taken)) player.drop(taken, false);
        SoundType sound = flower.block().defaultBlockState().getSoundType();
        level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.SUCCESS;
    }
}
