package dev.mehdi.modernenergistics.block;

import appeng.api.behaviors.GenericInternalInventory;
import aztech.modern_industrialization.machines.blockentities.AbstractCraftingMachineBlockEntity;
import dev.mehdi.modernenergistics.core.*;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.EnumSet;

public interface SingleMachineProvider {
    EnumSet<Direction> getTargets();
    static boolean fits(BlockEntity provider, appeng.api.crafting.IPatternDetails pattern, appeng.api.stacks.KeyCounter[] inputs) {
        if (provider.getLevel() == null) return false;
        for (var side : ((SingleMachineProvider)provider).getTargets()) {
            var pos = provider.getBlockPos().relative(side);
            var entity = provider.getLevel().getBlockEntity(pos);
            var bridge = entity instanceof BridgeHatchEntity hatch ? hatch.bridge() : MachineBridge.find(entity);
            if (bridge != null && new DispatchTarget(() -> bridge, pos).simulates(pattern, inputs)) return true;
        }
        return false;
    }
    static EnumSet<Direction> targets(BlockEntity provider, EnumSet<Direction> targets) {
        if (provider.getLevel() == null) return targets;
        targets.removeIf(side -> {
            var target = provider.getLevel().getBlockEntity(provider.getBlockPos().relative(side));
            return !(target instanceof AbstractCraftingMachineBlockEntity) && !(target instanceof BridgeHatchEntity);
        });
        return targets;
    }
    static void tick(BlockEntity provider, EnumSet<Direction> targets, GenericInternalInventory returns) {
        if (provider.getLevel() == null) return;
        for (var side : targets) {
            var be = provider.getLevel().getBlockEntity(provider.getBlockPos().relative(side));
            if (be instanceof AbstractCraftingMachineBlockEntity machine) {
                var bridge = ((CrafterBridge)machine.getCrafterComponent()).miae$bridge();
                bridge.managed = true;
                if (provider.getBlockPos().equals(bridge.returnPosition))
                    OutputTransfer.move(bridge, returns);
                bridge.refresh();
            }
        }
    }
    static void toggleLocks(BlockEntity provider, EnumSet<Direction> targets, net.minecraft.world.entity.player.Player player) {
        for (var side : targets) if (provider.getLevel().getBlockEntity(provider.getBlockPos().relative(side))
                instanceof AbstractCraftingMachineBlockEntity machine) {
            var bridge = ((CrafterBridge)machine.getCrafterComponent()).miae$bridge();
            bridge.lock.setKeep(!bridge.lock.keep());
            bridge.refresh();
            bridge.dirty();
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "Keep Recipe Locked: " + bridge.lock.keep()), false);
        }
    }
}
