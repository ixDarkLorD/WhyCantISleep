package net.ixdarklord.whycantisleep.deep.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.coolcatcanvas.api.armature.Motion;
import net.ixdarklord.coolcatcanvas.api.client.armature.Armature;
import net.ixdarklord.coolcatcanvas.api.client.armature.render.ArmatureBlockRenderer;
import net.ixdarklord.coolcatcore.api.client.registry.BlockEntityRendererRegistry;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.deep.TombBlock;
import net.ixdarklord.whycantisleep.deep.TombBlockEntity;
import net.ixdarklord.whycantisleep.deep.WCISBlocks;
import net.ixdarklord.whycantisleep.deep.mob.TombRevenantEntity;
import net.ixdarklord.whycantisleep.lurker.WCISEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A stone tomb: its lid drawn as an armature ({@code armatures/stone_tomb_lid}, from tools/gen_tomb.py) on its
 * head-left part so it can move (lifted a little, it grinds aside and tips half off the tomb when the tomb is opened;
 * a tomb first seen already open just shows its lid aside), and its dead lying under it.
 * <p>
 * The dead is the Tomb Revenant itself: a stand-in of the mob ({@link TombRevenantEntity#lying}), held at the first
 * frame of its rise, its eyes out, just where and as the tomb will spawn it (TombBlock.risePoint, riseYaw). When the
 * one that rises arrives it takes up from the very same pose, and the stand-in goes; so nothing jumps.
 */
public final class TombClient {
    private static final Motion CLOSED = Motion.hold("closed");
    private static final Motion OPEN = Motion.hold("open");
    private static final Motion OPENED = Motion.hold("opened");
    /** Ticks the stand-in waits, its tomb emptied, for the risen one to arrive in the client. */
    private static final long HANDOFF = 10;
    private static final Set<BlockEntity> SEEN_CLOSED = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<BlockEntity, Lying> LYING = new WeakHashMap<>();

    private TombClient() {
    }

    public static void init() {
        Armature armature = Armature.of(WhyCantISleep.id("stone_tomb_lid"));
        BlockEntityRendererRegistry.register(WCISBlocks.TOMB_ENTITY, context -> {
            Renderer renderer = new Renderer(context, armature);
            renderer.animate(animator -> animator.layer("lid").drive(ctx -> {
                BlockEntity be = ctx.blockEntity();
                if (be == null) return CLOSED;
                BlockState state = be.getBlockState();
                boolean open = state.hasProperty(TombBlock.OPEN) && state.getValue(TombBlock.OPEN);
                if (!open) {
                    SEEN_CLOSED.add(be);
                    return CLOSED;
                }
                return SEEN_CLOSED.contains(be) ? OPEN : OPENED;
            }));
            return renderer;
        });
    }

    // The dead lying in a tomb: its stand-in, and when the tomb gave it up (in client ticks), if it has.
    private static final class Lying {
        final TombRevenantEntity body;
        long emptiedAt = -1;
        boolean seenOccupied;

        Lying(TombRevenantEntity body) {
            this.body = body;
        }
    }

    // The stand-in to draw in this tomb now, or null.
    private static @Nullable TombRevenantEntity lying(TombBlockEntity be, BlockState state) {
        Level level = be.getLevel();
        if (level == null || !state.hasProperty(TombBlock.OCCUPIED) || !state.hasProperty(TombBlock.FACING)) return null;
        boolean occupied = state.getValue(TombBlock.OCCUPIED);
        Lying lying = LYING.get(be);
        if (lying == null || lying.body.level() != level) {
            if (!occupied) return null;  // first seen empty: nothing ever lay there for this client
            TombRevenantEntity body = new TombRevenantEntity(WCISEntities.TOMB_REVENANT.get(), level);
            body.lying = true;
            lying = new Lying(body);
            LYING.put(be, lying);
        }
        Direction facing = state.getValue(TombBlock.FACING);
        Vec3 at = TombBlock.risePoint(be.getBlockPos(), facing);
        float yaw = TombBlock.riseYaw(facing);
        TombRevenantEntity body = lying.body;
        body.snapTo(at.x, at.y, at.z, yaw, 0.0F);
        body.setOldPosAndRot();
        body.setYHeadRot(yaw);
        body.yHeadRotO = yaw;
        body.setYBodyRot(yaw);
        body.yBodyRotO = yaw;
        if (occupied) {
            lying.seenOccupied = true;
            lying.emptiedAt = -1;
            return body;
        }
        // Emptied: the stand-in stays until the risen one is there to take its place (a tick or so), then goes.
        if (!lying.seenOccupied) return null;
        if (lying.emptiedAt < 0) lying.emptiedAt = level.getGameTime();
        if (level.getGameTime() - lying.emptiedAt > HANDOFF) return null;
        boolean arrived = !level.getEntitiesOfClass(TombRevenantEntity.class, new AABB(at, at).inflate(0.5), revenant -> !revenant.lying).isEmpty();
        return arrived ? null : body;
    }

    public static final class Renderer extends ArmatureBlockRenderer<TombBlockEntity> {
        Renderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context, Armature armature) {
            super(context, armature);
        }

        @Override
        public State createRenderState() {
            return new TombState();
        }

        @Override
        public void extractRenderState(TombBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                       ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
            TombState tomb = (TombState) state;
            tomb.lying = null;
            BlockState block = be.getBlockState();
            // Shut, the hollow is hidden: nothing to draw in it.
            if (!block.hasProperty(TombBlock.OPEN) || !block.getValue(TombBlock.OPEN)) return;
            TombRevenantEntity body = lying(be, block);
            if (body == null) return;
            tomb.lying = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(body, partialTicks);
            tomb.offset = body.position().subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
        }

        @Override
        public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
            super.submit(state, poseStack, collector, camera);
            TombState tomb = (TombState) state;
            if (tomb.lying != null) {
                Minecraft.getInstance().getEntityRenderDispatcher().submit(tomb.lying, camera, tomb.offset.x, tomb.offset.y, tomb.offset.z, poseStack, collector);
            }
        }
    }

    public static final class TombState extends ArmatureBlockRenderer.State {
        public @Nullable EntityRenderState lying;
        public Vec3 offset = Vec3.ZERO;
    }
}
