package net.ixdarklord.whycantisleep.between;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * A dread candelabra: a stand of dread iron with a slot for each of its candles (1, 2, 3 or 5). Any of the game's
 * candles used on it is set, burning, in its next empty slot; used with an empty hand, the last one comes back out.
 * Its light grows with the candles in it ({@link #CANDLES}); each one's flame flickers and smokes as a candle's does.
 * The candles themselves are drawn by {@code CandelabraClient}, from their own models.
 */
public class CandelabraBlock extends DecorBlock implements EntityBlock {
    public static final MapCodec<CandelabraBlock> CODEC = simpleCodec(properties -> new CandelabraBlock(properties, Shapes.block(), List.of()));
    /** How many candles are in it, for its light. */
    public static final IntegerProperty CANDLES = IntegerProperty.create("candles", 0, 5);
    /** A candle's flame, over the foot of its slot (a candle stands six pixels, its wick one more). */
    private static final double FLAME = 7.5 / 16.0;
    private final List<Vec3> slots;

    /**
     * @param slots where each candle stands, in model pixels as the model is drawn (facing north): the middle of its
     *              foot. Kept alike with CANDELABRA_SLOTS in tools/gen_labyrinth_art.py.
     */
    public CandelabraBlock(BlockBehaviour.Properties properties, VoxelShape north, List<Vec3> slots) {
        super(properties, north, false);
        this.slots = slots;
        this.registerDefaultState(this.defaultBlockState().setValue(CANDLES, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    public List<Vec3> slots() {
        return this.slots;
    }

    /** The slots open in this state (all of them; a candle stand's lower parts have none). */
    public List<Vec3> slots(BlockState state) {
        return this.slots;
    }

    /** Light for the candles burning in it: none dark, up to full for five. */
    public static int light(BlockState state) {
        int candles = state.getValue(CANDLES);
        return candles == 0 ? 0 : Math.min(15, 5 + 2 * candles);
    }

    /** Where a slot is in the world, turned with the candelabra's facing. */
    public static Vec3 slotInWorld(Vec3 slot, Direction facing, BlockPos pos) {
        double x = slot.x / 16.0 - 0.5, z = slot.z / 16.0 - 0.5;
        // As the blockstate turns the model: north as drawn, east a quarter turn clockwise, and on round.
        double turn = Math.toRadians((facing.toYRot() + 180.0) % 360.0);
        double cos = Math.cos(turn), sin = Math.sin(turn);
        return new Vec3(pos.getX() + 0.5 + x * cos - z * sin, pos.getY() + slot.y / 16.0, pos.getZ() + 0.5 + x * sin + z * cos);
    }

    /** Whether a stack is a candle (any colour) that can be set in a candelabra. */
    public static boolean isCandle(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof CandleBlock;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CandelabraBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!isCandle(stack) || !(level.getBlockEntity(pos) instanceof CandelabraBlockEntity candelabra)
                || candelabra.count() >= this.slots(state).size()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Block candle = ((BlockItem) stack.getItem()).getBlock();
        if (!candelabra.add(candle)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        stack.consume(1, player);
        level.setBlock(pos, state.setValue(CANDLES, candelabra.count()), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.CANDLE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.4F, 1.2F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof CandelabraBlockEntity candelabra)
                || candelabra.count() == 0) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Block candle = candelabra.takeLast();
        if (candle == null) return InteractionResult.PASS;
        ItemStack taken = new ItemStack(candle);
        if (!player.addItem(taken)) player.drop(taken, false);
        level.setBlock(pos, state.setValue(CANDLES, candelabra.count()), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CANDLES) == 0 || !(level.getBlockEntity(pos) instanceof CandelabraBlockEntity candelabra)) return;
        List<Block> candles = candelabra.candles();
        Direction facing = state.getValue(FACING);
        List<Vec3> slots = this.slots(state);
        for (int i = 0; i < candles.size() && i < slots.size(); i++) {
            if (candles.get(i) == Blocks.AIR) continue;
            Vec3 wick = slotInWorld(slots.get(i), facing, pos).add(0.0, FLAME, 0.0);
            // As a candle's own flame: a small flame, now and then a wisp of smoke, now and then its crackle.
            if (random.nextFloat() < 0.17F) {
                level.playLocalSound(wick.x, wick.y, wick.z, SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS,
                        1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
            }
            if (random.nextFloat() < 0.3F) level.addParticle(ParticleTypes.SMOKE, wick.x, wick.y, wick.z, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.SMALL_FLAME, wick.x, wick.y, wick.z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CANDLES);
    }
}
