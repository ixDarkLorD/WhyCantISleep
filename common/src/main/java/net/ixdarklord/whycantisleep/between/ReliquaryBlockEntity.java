package net.ixdarklord.whycantisleep.between;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/** The heartwood reliquary's contents: three rows, as a chest, filled from its loot table the first time it opens. */
public class ReliquaryBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            ReliquaryBlockEntity.this.sound(state, true);
            ReliquaryBlockEntity.this.setOpen(state, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            ReliquaryBlockEntity.this.sound(state, false);
            ReliquaryBlockEntity.this.setOpen(state, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int count) {
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == ReliquaryBlockEntity.this;
        }
    };

    public ReliquaryBlockEntity(BlockPos pos, BlockState state) {
        super(WCISBlocks.RELIQUARY_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.trySaveLootTable(output)) ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(input)) ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.whycantisleep.heartwood_reliquary");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator() && this.level != null) {
            // Opened, the tree gives up its hoard and the way out of its heart wakes.
            if (this.level instanceof ServerLevel server) HeartPortalBlock.awaken(server, this.getBlockPos());
            this.openers.incrementOpeners(user.getLivingEntity(), this.level, this.getBlockPos(), this.getBlockState(), user.getContainerInteractionRange());
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator() && this.level != null) {
            this.openers.decrementOpeners(user.getLivingEntity(), this.level, this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return this.level == null ? List.of() : this.openers.getEntitiesWithContainerOpen(this.level, this.getBlockPos());
    }

    public void recheckOpen() {
        if (!this.remove && this.level != null) this.openers.recheckOpeners(this.level, this.getBlockPos(), this.getBlockState());
    }

    private void setOpen(BlockState state, boolean open) {
        if (this.level != null && state.hasProperty(ReliquaryBlock.OPEN)) {
            this.level.setBlock(this.getBlockPos(), state.setValue(ReliquaryBlock.OPEN, open), 3);
        }
    }

    private void sound(BlockState state, boolean open) {
        if (this.level == null) return;
        BlockPos pos = this.getBlockPos();
        // A heavy wooden lid: the chest's sound, lower; and the iron of its bands.
        this.level.playSound(null, pos, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.6F,
                this.level.getRandom().nextFloat() * 0.1F + 0.6F);
        this.level.playSound(null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.25F,
                this.level.getRandom().nextFloat() * 0.1F + 0.7F);
    }
}
