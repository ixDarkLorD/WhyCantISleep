package net.ixdarklord.whycantisleep.deep;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A stone tomb lying flat: one piece two blocks wide, four long and a block high, its carved lid spanning all eight
 * ({@link #SIDE} left or right of its length, {@link #SEGMENT} from the head, 0, to the foot, 3). {@code facing} is
 * the way its head points. Set down, it lays the rest of itself out ahead (if there is room); break any part and the
 * whole tomb goes (only its head-left part drops it).
 * <p>Its lid is drawn by its head-left part's block entity ({@link TombBlockEntity}, an armature on the client) so it
 * can move: use the tomb and the lid grinds aside ({@link #OPEN}), and a moment later the tomb gives up what is in it:
 * now treasure ({@code chests/garden_tomb}, thrown up out of it), now the dead, climbing out. Opened, it stays open.
 */
public class TombBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<TombBlock> CODEC = simpleCodec(TombBlock::new);
    public static final EnumProperty<Side> SIDE = EnumProperty.create("side", Side.class);
    public static final IntegerProperty SEGMENT = IntegerProperty.create("segment", 0, 3);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** Whether its dead still lies in it (the skeleton in its hollow); not once it has risen. */
    public static final BooleanProperty OCCUPIED = BooleanProperty.create("occupied");
    /** Ticks from opening to what it gives up: as long as the lid takes to slide aside. */
    private static final int REVEAL_TICKS = 22;
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, WhyCantISleep.id("chests/garden_tomb"));
    /** What climbs out of a tomb that holds the dead instead of treasure (set by the Deep's mobs; vanilla until then). */
    public static java.util.function.Supplier<EntityType<?>> risen = () -> EntityType.ZOMBIE;
    /** The top of the hollow's floor, open (its collision there), in blocks. */
    private static final double FLOOR = 3.0 / 16.0;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);
    private static final VoxelShape OPEN_SHAPE = Block.box(0, 0, 0, 16, 3, 16);

    public enum Side implements StringRepresentable {
        LEFT, RIGHT;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public TombBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SIDE, Side.LEFT).setValue(SEGMENT, 0).setValue(OPEN, false).setValue(OCCUPIED, true));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // Open, it is a hollow: only its floor holds anything up, so what rises out of it can climb out.
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? OPEN_SHAPE : SHAPE;
    }

    /** Where a part of the tomb whose head-left part is at {@code origin} lies. */
    public static BlockPos partPos(BlockPos origin, Direction facing, Side side, int segment) {
        BlockPos pos = origin.relative(facing.getOpposite(), segment);
        return side == Side.RIGHT ? pos.relative(facing.getClockWise()) : pos;
    }

    public static BlockPos origin(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos head = pos.relative(facing, state.getValue(SEGMENT));
        return state.getValue(SIDE) == Side.RIGHT ? head.relative(facing.getCounterClockWise()) : head;
    }

    // Set down where it is clicked as its head-left part, its head pointing the way the placer looks.
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        BlockPos origin = context.getClickedPos();
        Level level = context.getLevel();
        for (Side side : Side.values()) {
            for (int s = 0; s < 4; s++) {
                BlockPos at = partPos(origin, facing, side, s);
                if (!at.equals(origin) && !level.getBlockState(at).canBeReplaced(context)) return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        for (Side side : Side.values()) {
            for (int s = 0; s < 4; s++) {
                if (side == Side.LEFT && s == 0) continue;
                level.setBlock(partPos(pos, facing, side, s), state.setValue(SIDE, side).setValue(SEGMENT, s), Block.UPDATE_ALL);
            }
        }
    }

    // One part gone, the rest goes with it.
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockPos origin = origin(pos, state);
        Direction facing = state.getValue(FACING);
        for (Side side : Side.values()) {
            for (int s = 0; s < 4; s++) {
                BlockPos at = partPos(origin, facing, side, s);
                BlockState there = level.getBlockState(at);
                if (!at.equals(pos) && there.is(this) && there.getValue(FACING) == facing) {
                    level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(SIDE) == Side.LEFT && state.getValue(SEGMENT) == 0 ? new TombBlockEntity(pos, state) : null;
    }

    // Use it: the lid grinds aside (every part marked open), and when it has, the tomb gives up what it holds.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(OPEN)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            BlockPos origin = origin(pos, state);
            Direction facing = state.getValue(FACING);
            for (Side side : Side.values()) {
                for (int s = 0; s < 4; s++) {
                    BlockPos at = partPos(origin, facing, side, s);
                    BlockState there = server.getBlockState(at);
                    if (there.is(this)) server.setBlock(at, there.setValue(OPEN, true), Block.UPDATE_CLIENTS);
                }
            }
            Vec3 middle = middle(origin, facing);
            server.playSound(null, middle.x, middle.y, middle.z, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, 0.5F);
            server.playSound(null, middle.x, middle.y, middle.z, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.8F, 0.6F);
            server.scheduleTick(origin, this, REVEAL_TICKS);
        }
        return InteractionResult.SUCCESS;
    }

    // The lid is aside: treasure, or the dead.
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(OPEN) || state.getValue(SIDE) != Side.LEFT || state.getValue(SEGMENT) != 0) return;
        Vec3 middle = middle(pos, state.getValue(FACING));
        if (random.nextFloat() < 0.55F) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(LOOT);
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, middle).create(LootContextParamSets.CHEST);
            for (ItemStack stack : table.getRandomItems(params, random)) {
                ItemEntity item = new ItemEntity(level, middle.x, middle.y + 0.4, middle.z, stack,
                        (random.nextDouble() - 0.5) * 0.2, 0.3, (random.nextDouble() - 0.5) * 0.2);
                item.setDefaultPickUpDelay();
                level.addFreshEntity(item);
            }
            level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, 0.7F);
            level.sendParticles(ParticleTypes.ENCHANT, middle.x, middle.y + 0.5, middle.z, 30, 0.6, 0.3, 1.2, 0.4);
        } else {
            // Its dead rises: the skeleton is gone from the hollow, and it climbs out where it lay, facing the foot.
            Direction facing = state.getValue(FACING);
            for (Side side : Side.values()) {
                for (int s = 0; s < 4; s++) {
                    BlockPos at = partPos(pos, facing, side, s);
                    BlockState there = level.getBlockState(at);
                    if (there.is(this)) level.setBlock(at, there.setValue(OCCUPIED, false), Block.UPDATE_CLIENTS);
                }
            }
            // Just where the tomb drew it lying (TombClient), and as it lay: on the hollow's floor, facing the foot.
            Entity mob = risen.get().create(level, EntitySpawnReason.TRIGGERED);
            if (mob instanceof net.minecraft.world.entity.Mob m) {
                Vec3 at = risePoint(pos, facing);
                float yaw = riseYaw(facing);
                m.snapTo(at.x, at.y, at.z, yaw, 0.0F);
                m.setYHeadRot(yaw);
                m.setYBodyRot(yaw);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.TRIGGERED, null);
                if (m instanceof net.ixdarklord.whycantisleep.deep.mob.TombRevenantEntity revenant) revenant.startRising();
                level.addFreshEntity(m);
            }
            level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.ZOMBIE_AMBIENT, SoundSource.HOSTILE, 1.4F, 0.6F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, middle.x, middle.y + 0.3, middle.z, 25, 0.5, 0.2, 1.2, 0.02);
        }
    }

    /** Where its dead lies, and rises from: the middle of the hollow's floor, from the tomb's head-left part. */
    public static Vec3 risePoint(BlockPos origin, Direction facing) {
        return middle(origin, facing).add(0.0, FLOOR - 0.8, 0.0);
    }

    /** Which way its dead faces, lying and risen: toward the tomb's foot. */
    public static float riseYaw(Direction facing) {
        return facing.getOpposite().toYRot();
    }

    /** The middle of the tomb's top, from its head-left part. */
    private static Vec3 middle(BlockPos origin, Direction facing) {
        Vec3 corner = Vec3.atBottomCenterOf(origin).add(0.0, 0.8, 0.0);
        Vec3 right = Vec3.atLowerCornerOf(facing.getClockWise().getUnitVec3i()).scale(0.5);
        Vec3 back = Vec3.atLowerCornerOf(facing.getOpposite().getUnitVec3i()).scale(1.5);
        return corner.add(right).add(back);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIDE, SEGMENT, OPEN, OCCUPIED);
    }
}
