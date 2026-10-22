package net.ixdarklord.whycantisleep.mixin.client;

import net.ixdarklord.whycantisleep.client.HidingPose;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Lets a player's render state carry whether they are hiding, lying flat, and trembling there ({@link HidingPose}). */
@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements HidingPose {
    @Unique
    private boolean whycantisleep$hiding;
    @Unique
    private boolean whycantisleep$trembling;
    @Unique
    private boolean whycantisleep$lying;
    @Unique
    private boolean whycantisleep$tucked;
    @Unique
    private float whycantisleep$peekRise;

    @Override
    public float whycantisleep$peekRise() {
        return this.whycantisleep$peekRise;
    }

    @Override
    public void whycantisleep$setPeekRise(float rise) {
        this.whycantisleep$peekRise = rise;
    }

    @Override
    public boolean whycantisleep$isTucked() {
        return this.whycantisleep$tucked;
    }

    @Override
    public void whycantisleep$setTucked(boolean tucked) {
        this.whycantisleep$tucked = tucked;
    }

    @Override
    public boolean whycantisleep$isLying() {
        return this.whycantisleep$lying;
    }

    @Override
    public void whycantisleep$setLying(boolean lying) {
        this.whycantisleep$lying = lying;
    }

    @Override
    public boolean whycantisleep$isHiding() {
        return this.whycantisleep$hiding;
    }

    @Override
    public void whycantisleep$setHiding(boolean hiding) {
        this.whycantisleep$hiding = hiding;
    }

    @Override
    public boolean whycantisleep$isTrembling() {
        return this.whycantisleep$trembling;
    }

    @Override
    public void whycantisleep$setTrembling(boolean trembling) {
        this.whycantisleep$trembling = trembling;
    }
}
