package net.ixdarklord.whycantisleep.sleep;

import net.ixdarklord.whycantisleep.config.WCISServerConfig;
import net.ixdarklord.whycantisleep.network.SleepBlockedPayload;
import net.ixdarklord.whycantisleep.network.WCISNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;

import java.util.List;

/** The server's side of a failed sleep: it knows exactly which monsters blocked it, and tells the player. */
public final class SleepServer {
    private SleepServer() {
    }

    /** Called when the game refused a sleep with "monsters nearby" ({@code ServerPlayerMixin}). */
    public static void onBlockedByMonsters(ServerPlayer player, BlockPos bed) {
        List<Monster> blockers = SleepChecks.blockers(player, bed);
        if (blockers.isEmpty()) return;

        if (WCISNetwork.hasMod(player)) {
            WCISNetwork.sendToPlayer(player, new SleepBlockedPayload(bed, blockers.stream().map(Entity::getId).toList()));
        } else if (WCISServerConfig.ALLOW_HIGHLIGHTS.get() && WCISServerConfig.SHOW_TO_EVERYONE.get()) {
            // A client without the mod can only be shown the game's own glowing effect, which everyone nearby sees too.
            int ticks = WCISServerConfig.HIGHLIGHT_DURATION.get() * 20;
            for (Monster monster : blockers) {
                monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
            }
        }
    }
}
