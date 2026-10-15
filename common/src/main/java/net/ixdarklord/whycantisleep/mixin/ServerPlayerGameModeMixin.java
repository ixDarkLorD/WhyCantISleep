package net.ixdarklord.whycantisleep.mixin;

import net.ixdarklord.whycantisleep.deep.GardenWard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The Garden's ward: a player's breaking of a block it holds is refused from the first blow (GardenWard). */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Shadow protected ServerLevel level;
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"), cancellable = true)
    private void whycantisleep$ward(BlockPos pos, ServerboundPlayerActionPacket.Action action, Direction face, int maxY, int sequence, CallbackInfo ci) {
        if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK || !GardenWard.refuses(this.player, pos)) return;
        if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) GardenWard.struck(this.player, pos, face);
        // The block as it is (the client may have broken it ahead of the server), and its contents.
        this.player.connection.send(new ClientboundBlockUpdatePacket(pos, this.level.getBlockState(pos)));
        BlockEntity entity = this.level.getBlockEntity(pos);
        if (entity != null) {
            Packet<?> update = entity.getUpdatePacket();
            if (update != null) this.player.connection.send(update);
        }
        this.player.connection.ackBlockChangesUpTo(sequence);
        ci.cancel();
    }
}
