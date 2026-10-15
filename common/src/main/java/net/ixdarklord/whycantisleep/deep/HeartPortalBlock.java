package net.ixdarklord.whycantisleep.deep;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * The way out of the weeping tree's heart: a 3 by 3 membrane of dark heartwood in the chamber's back wall. It lies
 * dormant until the heartwood reliquary is first opened ({@link #awaken}); then it tears open into a red rift (drawn
 * on the client by {@link HeartPortalBlockEntity}, on the {@link #CORE} block in its middle) and whoever steps into
 * it is thrown out of the labyrinth, onto the way before the gate the tree's door faces ({@link #FACING}: from the
 * portal towards the door).
 */
public class HeartPortalBlock extends BaseEntityBlock {
    public static final MapCodec<HeartPortalBlock> CODEC = simpleCodec(HeartPortalBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** The middle block of the nine, which keeps the rift. */
    public static final BooleanProperty CORE = BooleanProperty.create("core");
    /** How far from the tree's middle the portal puts you: past the walls (80 out), on the way to the gate. */
    private static final int OUT = 90;

    public HeartPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(ACTIVE, false).setValue(CORE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /** Wakes every dormant portal block near a place (the reliquary, as it is first opened). */
    public static void awaken(ServerLevel level, BlockPos around) {
        boolean woke = false;
        for (BlockPos pos : BlockPos.betweenClosed(around.offset(-6, -3, -6), around.offset(6, 4, 6))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof HeartPortalBlock && !state.getValue(ACTIVE)) {
                level.setBlock(pos, state.setValue(ACTIVE, true), Block.UPDATE_ALL);
                woke = true;
            }
        }
        if (woke) {
            level.playSound(null, around, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.6F);
            level.playSound(null, around, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 0.5F);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean past) {
        if (!state.getValue(ACTIVE) || !(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player)) return;
        if (player.isOnPortalCooldown() || player.isPassenger() || player.isVehicle()) return;
        // The tree's middle is two blocks from the portal towards the door; the way out is OUT further on.
        Direction facing = state.getValue(FACING);
        BlockPos core = pos;
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            BlockState s = level.getBlockState(near);
            if (s.getBlock() instanceof HeartPortalBlock && s.getValue(CORE)) {
                core = near.immutable();
                break;
            }
        }
        int x = core.getX() + facing.getStepX() * (OUT + 2);
        int z = core.getZ() + facing.getStepZ() * (OUT + 2);
        server.getChunk(x >> 4, z >> 4);
        int y = server.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= server.getMinY()) y = core.getY();
        server.playSound(null, player.blockPosition(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.teleportTo(server, x + 0.5, y, z + 0.5, Set.<Relative>of(), facing.toYRot(), 0.0F, true);
        player.setPortalCooldown(60);
        server.playSound(null, player.blockPosition(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(ACTIVE) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return Shapes.block();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(CORE) ? new HeartPortalBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, WCISBlocks.HEART_PORTAL_ENTITY.get(), (l, p, s, be) -> HeartPortalBlockEntity.clientTick.accept(be)) : null;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, CORE);
    }
}
