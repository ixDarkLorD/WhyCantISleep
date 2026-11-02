package net.ixdarklord.whycantisleep.compat.jade;

import net.ixdarklord.whycantisleep.WhyCantISleep;
import net.ixdarklord.whycantisleep.compat.LookedAtLines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * The mod's lines in Jade's tooltip of the block looked at ({@link LookedAtLines}). The only class that touches
 * Jade's API: found through Fabric's {@code jade} entrypoint and Forge's {@link WailaPlugin} annotation, and
 * never loaded without Jade.
 */
@WailaPlugin(WhyCantISleep.MOD_ID)
public class WCISJadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Lines.INSTANCE, Block.class);
    }

    private enum Lines implements IBlockComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID = WhyCantISleep.id("sleep");

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            for (Component line : LookedAtLines.lines(accessor.getLevel(), accessor.getPosition(), accessor.getBlockState())) tooltip.add(line);
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
