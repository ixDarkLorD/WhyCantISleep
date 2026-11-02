package net.ixdarklord.whycantisleep.compat.wthit;

import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.ixdarklord.whycantisleep.compat.LookedAtLines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

/**
 * The mod's lines in WTHIT's tooltip of the block looked at ({@link LookedAtLines}). The only class that touches
 * WTHIT's API: named in {@code waila_plugins.json}, and never loaded without WTHIT.
 */
public class WCISWthitPlugin implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(Lines.INSTANCE, Block.class);
    }

    private enum Lines implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            for (Component line : LookedAtLines.lines(accessor.getLevel(), accessor.getPosition(), accessor.getBlockState())) tooltip.addLine(line);
        }
    }
}
