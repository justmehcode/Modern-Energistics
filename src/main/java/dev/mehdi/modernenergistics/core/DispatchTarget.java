package dev.mehdi.modernenergistics.core;

import appeng.api.crafting.IPatternDetails;
import appeng.api.implementations.blockentities.*;
import appeng.api.stacks.*;
import dev.mehdi.modernenergistics.mixin.CrafterAccessor;
import net.minecraft.core.Direction;
import java.util.List;
import java.util.function.Supplier;

public record DispatchTarget(Supplier<MachineBridge> bridge, net.minecraft.core.BlockPos source) implements ICraftingMachine {
    // Always true: otherwise AE2 falls back to ordinary insertion and bypasses the recipe handshake.
    @Override public boolean acceptsPlans() { return true; }
    @Override public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputs, Direction side) {
        var target = bridge.get();
        if (target == null) return false;
        if (net.neoforged.fml.ModList.get().isLoaded("advanced_ae")
                && !dev.mehdi.modernenergistics.compat.AdvancedPatternRouting.accepts(pattern, inputs, target.owner.getLevel(), source)) {
            target.status = "Advanced pattern input face cannot accept ingredients";
            return false;
        }
        if (!target.accepts(BatchDispatch.scaled(pattern), inputs, target.running())) return false;
        target.returnPosition = source.relative(side);
        target.returnSide = side.getOpposite();
        target.dirty();
        return true;
    }
    public boolean simulates(IPatternDetails pattern, KeyCounter[] inputs) {
        var target = bridge.get();
        if (target == null) return false;
        if (net.neoforged.fml.ModList.get().isLoaded("advanced_ae")
                && !dev.mehdi.modernenergistics.compat.AdvancedPatternRouting.accepts(pattern, inputs, target.owner.getLevel(), source)) return false;
        return target.simulates(BatchDispatch.scaled(pattern), inputs);
    }
    @Override public PatternContainerGroup getCraftingMachineInfo() {
        var target = bridge.get();
        return target == null ? PatternContainerGroup.nothing() : new PatternContainerGroup(
                AEItemKey.of(target.owner.getBlockState().getBlock()), target.owner.getDisplayName(), List.of());
    }
}
