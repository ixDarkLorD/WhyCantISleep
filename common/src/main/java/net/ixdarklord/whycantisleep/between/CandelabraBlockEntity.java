package net.ixdarklord.whycantisleep.between;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The candles set in a candelabra ({@link CandelabraBlock}): one per slot, any of the game's candles, air where a slot is
 * empty. Saved as {@code Candles}, a list of block ids in slot order (the structure generator writes them so too).
 */
public class CandelabraBlockEntity extends BlockEntity {
    private final List<Block> candles = new ArrayList<>();

    public CandelabraBlockEntity(BlockPos pos, BlockState state) {
        super(WCISBlocks.CANDELABRA_ENTITY.get(), pos, state);
        this.fit(state);
    }

    private int slots(BlockState state) {
        return state.getBlock() instanceof CandelabraBlock candelabra ? candelabra.slots().size() : 0;
    }

    /** Pads or trims the slots to the candelabra's own count. */
    private void fit(BlockState state) {
        int slots = this.slots(state);
        while (this.candles.size() < slots) this.candles.add(Blocks.AIR);
        while (this.candles.size() > slots) this.candles.removeLast();
    }

    /** The candle in each slot, air where there is none. */
    public List<Block> candles() {
        return Collections.unmodifiableList(this.candles);
    }

    public int count() {
        return (int) this.candles.stream().filter(candle -> candle != Blocks.AIR).count();
    }

    /** Sets a candle in the first empty slot; false when every slot holds one. */
    public boolean add(Block candle) {
        int slot = this.candles.indexOf(Blocks.AIR);
        if (slot < 0) return false;
        this.candles.set(slot, candle);
        this.changed();
        return true;
    }

    /** Takes out the candle in the last filled slot, or null when there is none. */
    public @Nullable Block takeLast() {
        for (int slot = this.candles.size() - 1; slot >= 0; slot--) {
            Block candle = this.candles.get(slot);
            if (candle == Blocks.AIR) continue;
            this.candles.set(slot, Blocks.AIR);
            this.changed();
            return candle;
        }
        return null;
    }

    private void changed() {
        this.setChanged();
        if (this.level != null) this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Candles", BuiltInRegistries.BLOCK.byNameCodec().listOf(), List.copyOf(this.candles));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.candles.clear();
        input.read("Candles", BuiltInRegistries.BLOCK.byNameCodec().listOf()).ifPresent(this.candles::addAll);
        this.fit(this.getBlockState());
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
        for (Block candle : this.candles) {
            if (candle != Blocks.AIR) {
                Containers.dropItemStack(this.level, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, new ItemStack(candle));
            }
        }
    }
}
