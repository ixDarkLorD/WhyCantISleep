package net.ixdarklord.whycantisleep.between.client;

import net.ixdarklord.coolcatcanvas.api.armature.Motion;
import net.ixdarklord.coolcatcanvas.api.client.armature.Armature;
import net.ixdarklord.coolcatcanvas.api.client.armature.render.ArmatureBlockRenderer;
import net.ixdarklord.coolcatcore.api.client.registry.BlockEntityRendererRegistry;
import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.between.ReliquaryBlock;
import net.ixdarklord.whycantisleep.between.ReliquaryBlockEntity;
import net.ixdarklord.whycantisleep.between.WCISBlocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The heartwood reliquary, drawn as an armature ({@code armatures/heartwood_reliquary}, from tools/gen_reliquary.py) so
 * its lid swings: up past open and settling as it opens, dropping shut with a small bounce as it closes, following
 * the block's {@code open} state. A reliquary first seen shut just stays shut (its closing swing is only for one that
 * was seen open).
 */
public final class ReliquaryClient {
    private static final Motion CLOSED = Motion.hold("closed");
    private static final Motion OPEN = Motion.hold("open");
    private static final Motion CLOSE = Motion.hold("close");
    /** Reliquaries seen open since they were loaded, so their next shut is a swing. */
    private static final Set<BlockEntity> SEEN_OPEN = Collections.newSetFromMap(new WeakHashMap<>());

    private ReliquaryClient() {
    }

    public static void init() {
        BlockEntityRendererRegistry.register(WCISBlocks.RELIQUARY_ENTITY, ArmatureBlockRenderer.<ReliquaryBlockEntity>of(
                Armature.of(WhyCantISleep.id("heartwood_reliquary")), renderer -> renderer.animate(animator -> animator.layer("lid").drive(ctx -> {
                    BlockEntity be = ctx.blockEntity();
                    if (be == null) return CLOSED;
                    BlockState state = be.getBlockState();
                    boolean open = state.hasProperty(ReliquaryBlock.OPEN) && state.getValue(ReliquaryBlock.OPEN);
                    if (open) {
                        SEEN_OPEN.add(be);
                        return OPEN;
                    }
                    return SEEN_OPEN.contains(be) ? CLOSE : CLOSED;
                }))));
    }
}
