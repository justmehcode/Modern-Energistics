package dev.mehdi.modernenergistics.test;

import appeng.api.config.*;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.service.CraftingService;
import net.minecraft.world.level.Level;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;

/** Runs AdvancedAE's real CPU logic using an online test grid instead of building its large CPU structure. */
public class AdvancedBatchTestSupport {
    public static SmartBatchGameTests.Cpu create(IGrid grid, Level level) {
        var cpu = new AdvCraftingCPU(null, java.util.UUID.randomUUID(), 65536) {
            @Override public boolean isActive() { return true; }
            @Override public IGrid getGrid() { return grid; }
            @Override public Level getLevel() { return level; }
            @Override public void markDirty() {}
            @Override public void deactivate() {}
            @Override public IActionSource getSrc() { return IActionSource.empty(); }
            @Override public net.minecraft.network.chat.Component getName() { return net.minecraft.network.chat.Component.literal("Batch test CPU"); }
        };
        return new SmartBatchGameTests.Cpu() {
            @Override public ICraftingSubmitResult submit(ICraftingPlan plan) { return cpu.craftingLogic.trySubmitJob(grid, plan, IActionSource.empty(), null); }
            @Override public int execute() { return cpu.craftingLogic.executeCrafting(1, (CraftingService)grid.getCraftingService(), grid.getEnergyService(), level); }
            @Override public ListCraftingInventory inventory() { return cpu.craftingLogic.getInventory(); }
            @Override public void receive(AEKey key, long amount) { cpu.craftingLogic.insert(key, amount, Actionable.MODULATE); }
            @Override public boolean busy() { return cpu.craftingLogic.hasJob(); }
        };
    }
}
