package dev.mehdi.modernenergistics.core;

import appeng.api.AECapabilities;
import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.config.Actionable;
import appeng.api.stacks.*;
import aztech.modern_industrialization.machines.components.CrafterComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public final class OutputTransfer {
    private OutputTransfer() {}
    public static void toOrigin(MachineBridge bridge) {
        Level level = bridge.owner.getLevel();
        BlockPos target = bridge.returnPosition;
        if (target == null || bridge.returnSide == null || !level.hasChunkAt(target)) return;
        var inv = level.getCapability(AECapabilities.GENERIC_INTERNAL_INV, target, bridge.returnSide);
        if (inv != null) move(bridge, inv);
    }
    public static void move(MachineBridge bridge, GenericInternalInventory target) {
        move(bridge.inventory(), target, bridge::dirty);
        if (bridge.lock.busy() || bridge.running() || !target.canInsert()) return;
        boolean changed = false;
        for (var entry : bridge.borrowed.entrySet()) {
            for (var stack : bridge.inventory().getItemInputs()) if (!stack.isEmpty()
                    && entry.getKey().equals(AEItemKey.of(stack.toStack()))) {
                long moved = insert(target, entry.getKey(), Math.min(entry.getValue(), stack.getAmount()));
                if (moved > 0) { stack.decrement(moved); entry.setValue(entry.getValue() - moved); changed = true; }
            }
            for (var stack : bridge.inventory().getFluidInputs()) if (!stack.isEmpty()
                    && entry.getKey().equals(AEFluidKey.of(stack.toStack()))) {
                long moved = insert(target, entry.getKey(), Math.min(entry.getValue(), stack.getAmount()));
                if (moved > 0) { stack.decrement(moved); entry.setValue(entry.getValue() - moved); changed = true; }
            }
        }
        bridge.borrowed.values().removeIf(n -> n == 0);
        if (changed) bridge.dirty();
    }
    public static void move(CrafterComponent.Inventory source, GenericInternalInventory target, Runnable dirty) {
        if (!target.canInsert()) return;
        boolean changed = false;
        for (var stack : source.getItemOutputs()) if (!stack.isEmpty()) {
            long moved = insert(target, AEItemKey.of(stack.toStack()), stack.getAmount());
            if (moved > 0) { stack.decrement(moved); changed = true; }
        }
        for (var stack : source.getFluidOutputs()) if (!stack.isEmpty()) {
            long moved = insert(target, AEFluidKey.of(stack.toStack()), stack.getAmount());
            if (moved > 0) { stack.decrement(moved); changed = true; }
        }
        if (changed) dirty.run();
    }
    private static long insert(GenericInternalInventory target, AEKey key, long amount) {
        long left = amount;
        for (int i = 0; i < target.size() && left > 0; i++)
            left -= target.insert(i, key, left, Actionable.MODULATE);
        return amount - left;
    }
}
