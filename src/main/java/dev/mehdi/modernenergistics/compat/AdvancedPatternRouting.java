package dev.mehdi.modernenergistics.compat;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.pedroksl.advanced_ae.common.patterns.IAdvPatternDetails;

/** MI shares input inventories between faces; honor explicit AdvancedAE face restrictions before atomic dispatch. */
public final class AdvancedPatternRouting {
    private AdvancedPatternRouting() {}
    public static boolean accepts(IPatternDetails pattern, KeyCounter[] counters, Level level, BlockPos pos) {
        if (!(pattern instanceof IAdvPatternDetails advanced) || !advanced.directionalInputsSet()) return true;
        for (var counter : counters) for (var entry : counter) {
            var side = advanced.getDirectionSideForInputKey(entry.getKey());
            if (side == null) continue;
            long remaining = entry.getLongValue();
            if (entry.getKey() instanceof AEItemKey item) {
                var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
                if (handler == null) return false;
                for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
                    int amount = (int)Math.min(remaining, Integer.MAX_VALUE);
                    remaining -= amount - handler.insertItem(slot, item.toStack(amount), true).getCount();
                }
            } else if (entry.getKey() instanceof AEFluidKey fluid) {
                var handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
                if (handler == null || remaining > Integer.MAX_VALUE) return false;
                remaining -= handler.fill(fluid.toStack((int)remaining), IFluidHandler.FluidAction.SIMULATE);
            }
            if (remaining != 0) return false;
        }
        return true;
    }
}
