package net.ixdarklord.whycantisleep.between;

import net.ixdarklord.coolcatcore.api.event.v2.common.ServerTickEvents;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What the wild creatures and their charms need kept on the server from tick to tick: where each player last stood on
 * solid ground (the Void Scale takes them back there), who is shrunk by Shrinking Skin and until when, and blocks put
 * down for a while (the Broodmother's webs), taken away again when their time is up.
 */
public final class WildsServer {
    /** The Shrinking Skin's scale modifier on its wearer. */
    public static final Identifier SHRINK = WhyCantISleep.id("shrinking_skin");

    /** Where a player last stood on solid ground. */
    public record Ground(ResourceKey<Level> dimension, Vec3 at) {
    }

    private record Temporary(ResourceKey<Level> dimension, BlockPos pos, BlockState state, long until) {
    }

    private static final Map<UUID, Ground> LAST_GROUND = new HashMap<>();
    private static final Map<UUID, Long> SHRUNK = new HashMap<>();
    private static final List<Temporary> TEMPORARY = new ArrayList<>();

    private WildsServer() {
    }

    public static void init() {
        ServerTickEvents.END_LEVEL.register(WildsServer::tick);
    }

    /** Puts a block down for a while; it is taken away after {@code ticks} if it is still there. */
    public static void temporary(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        level.setBlock(pos, state, 3);
        TEMPORARY.add(new Temporary(level.dimension(), pos.immutable(), state, level.getGameTime() + ticks));
    }

    public static @Nullable Ground lastGround(Player player) {
        return LAST_GROUND.get(player.getUUID());
    }

    /** Shrinks a player to half their size for {@code ticks}. */
    public static void shrink(ServerPlayer player, int ticks) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale == null) return;
        scale.addOrUpdateTransientModifier(new AttributeModifier(SHRINK, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        SHRUNK.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    private static void tick(ServerLevel level) {
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            if (now % 10 == 0 && player.onGround() && !player.isSpectator()) {
                BlockPos below = player.blockPosition().below();
                if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    LAST_GROUND.put(player.getUUID(), new Ground(level.dimension(), player.position()));
                }
            }
            Long until = SHRUNK.get(player.getUUID());
            if (until != null && now >= until) {
                SHRUNK.remove(player.getUUID());
                AttributeInstance scale = player.getAttribute(Attributes.SCALE);
                if (scale != null) scale.removeModifier(SHRINK);
            }
        }
        for (Iterator<Temporary> it = TEMPORARY.iterator(); it.hasNext(); ) {
            Temporary t = it.next();
            if (!t.dimension().equals(level.dimension()) || now < t.until()) continue;
            if (level.getBlockState(t.pos()) == t.state()) level.removeBlock(t.pos(), false);
            it.remove();
        }
    }
}
