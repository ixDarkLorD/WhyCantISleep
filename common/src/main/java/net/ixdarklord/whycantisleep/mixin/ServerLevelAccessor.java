package net.ixdarklord.whycantisleep.mixin;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Wakes everyone asleep in a level the way the game does when a night is skipped, for a night skipped by vote. */
@Mixin(ServerLevel.class)
public interface ServerLevelAccessor {
    @Invoker("wakeUpAllPlayers")
    void whycantisleep$wakeUpAllPlayers();
}
