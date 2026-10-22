package net.ixdarklord.whycantisleep.client;

/**
 * A player's render state that knows whether they are hiding, lying flat or not, and whether they tremble there with
 * monsters about ({@code AvatarRenderStateMixin}): set when it is filled ({@code AvatarRendererMixin}) and read when the
 * model is posed ({@code HumanoidModelMixin}) and placed ({@code AvatarRendererMixin.getRenderOffset}).
 */
public interface HidingPose {
    boolean whycantisleep$isHiding();

    void whycantisleep$setHiding(boolean hiding);

    boolean whycantisleep$isTrembling();

    void whycantisleep$setTrembling(boolean trembling);

    /** Whether they lie flat on the floor (under a bed or table), rather than stand. */
    boolean whycantisleep$isLying();

    void whycantisleep$setLying(boolean lying);

    /** Whether they are crouched inside something (a chest), only their eyes showing ({@code ChestPeek}). */
    boolean whycantisleep$isTucked();

    void whycantisleep$setTucked(boolean tucked);

    /** How far a player crouched in a chest has risen to peer out of it, 0 to 1 ({@code HidingClient.peekRise}). */
    float whycantisleep$peekRise();

    void whycantisleep$setPeekRise(float rise);
}
