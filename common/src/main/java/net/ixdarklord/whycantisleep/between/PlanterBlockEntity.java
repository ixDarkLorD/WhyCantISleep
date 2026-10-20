package net.ixdarklord.whycantisleep.between;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The flowers set in a stone planter: any flower of the game's (anything in {@code #small_flowers}, any flower block, and
 * the tall flowers), each at its own random place in the tray and turned its own way. Places are in texels of the
 * planter's 32-texel model (two to a pixel), given facing north, so they turn with it.
 */
public class PlanterBlockEntity extends BlockEntity {
    public static final int CAPACITY = 6;
    // The tray's soil, kept a little in from its rim (pixels 4.5 to 11.5 across, 5.5 to 10.5 deep), and how far apart two
    // flowers are kept.
    private static final int MIN_X = 9, MAX_X = 23, MIN_Z = 11, MAX_Z = 21, SPACING = 5;

    public record Flower(Block block, int x, int z, int yaw) {
        public static final Codec<Flower> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.BLOCK.byNameCodec().fieldOf("Block").forGetter(Flower::block),
                Codec.INT.fieldOf("X").forGetter(Flower::x),
                Codec.INT.fieldOf("Z").forGetter(Flower::z),
                Codec.INT.optionalFieldOf("Yaw", 0).forGetter(Flower::yaw)).apply(instance, Flower::new));
    }

    private final List<Flower> flowers = new ArrayList<>();

    public PlanterBlockEntity(BlockPos pos, BlockState state) {
        super(WCISBlocks.PLANTER_ENTITY.get(), pos, state);
    }

    /** Whether a stack is a flower that can be set in a planter. */
    public static boolean plantable(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) return false;
        Block block = item.getBlock();
        BlockState state = block.defaultBlockState();
        return state.is(BlockTags.SMALL_FLOWERS) || block instanceof FlowerBlock
                || (state.is(BlockTags.FLOWERS) && block instanceof DoublePlantBlock);
    }

    public List<Flower> flowers() {
        return Collections.unmodifiableList(this.flowers);
    }

    public boolean isFull() {
        return this.flowers.size() >= CAPACITY;
    }

    /** Sets a flower at a random free place in the tray; false when there is no room left. */
    public boolean plant(Block block, RandomSource random) {
        if (this.isFull()) return false;
        for (int attempt = 0; attempt < 32; attempt++) {
            int x = MIN_X + random.nextInt(MAX_X - MIN_X + 1);
            int z = MIN_Z + random.nextInt(MAX_Z - MIN_Z + 1);
            boolean clear = this.flowers.stream().allMatch(f -> (f.x() - x) * (f.x() - x) + (f.z() - z) * (f.z() - z) >= SPACING * SPACING);
            if (!clear) continue;
            this.flowers.add(new Flower(block, x, z, random.nextInt(360)));
            this.changed();
            return true;
        }
        return false;
    }

    /** Takes out the flower set last, or null for an empty planter. */
    public @Nullable Flower takeLast() {
        if (this.flowers.isEmpty()) return null;
        Flower flower = this.flowers.removeLast();
        this.changed();
        return flower;
    }

    private void changed() {
        this.setChanged();
        if (this.level != null) this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Flowers", Flower.CODEC.listOf(), List.copyOf(this.flowers));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.flowers.clear();
        input.read("Flowers", Flower.CODEC.listOf()).ifPresent(this.flowers::addAll);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level == null) return;
        for (Flower flower : this.flowers) {
            Containers.dropItemStack(this.level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(flower.block()));
        }
    }
}
