package dev.mehdi.modernenergistics.test;

import appeng.api.AECapabilities;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.*;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.core.definitions.AEBlocks;
import aztech.modern_industrialization.machines.multiblocks.MultiblockMachineBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import dev.mehdi.modernenergistics.ModernEnergistics;
import dev.mehdi.modernenergistics.block.BridgeHatchEntity;
import dev.mehdi.modernenergistics.core.MachineBridge;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

public class FluxTestSupport {
    public static void run(GameTestHelper h) {
        var cardId = ResourceLocation.parse("appflux:induction_card");
        if (!BuiltInRegistries.ITEM.containsKey(cardId)) { h.succeed(); return; }
        var card = BuiltInRegistries.ITEM.get(cardId);
        var providers = new ArrayList<appeng.blockentity.grid.AENetworkedBlockEntity>();
        var hatches = new ArrayList<BridgeHatchEntity>();
        var upgrades = new ArrayList<appeng.api.upgrades.IUpgradeInventory>();
        int z = 1;
        for (String name : List.of("mi_pattern_provider", "advanced_mi_pattern_provider", "extended_mi_pattern_provider", "combined_mi_pattern_provider")) {
            if (!BuiltInRegistries.BLOCK.containsKey(ModernEnergistics.id(name))) continue;
            h.setBlock(new BlockPos(2, 1, z), ModernEnergistics.HATCH.get());
            h.setBlock(new BlockPos(3, 1, z), BuiltInRegistries.BLOCK.get(ModernEnergistics.id(name)));
            h.setBlock(new BlockPos(4, 1, z), AEBlocks.CREATIVE_ENERGY_CELL.block());
            hatches.add((BridgeHatchEntity)h.getBlockEntity(new BlockPos(2, 1, z)));
            providers.add((appeng.blockentity.grid.AENetworkedBlockEntity)h.getBlockEntity(new BlockPos(3, 1, z)));
            z += 4;
        }
        h.startSequence().thenIdle(15).thenExecute(() -> {
            AEKey flux = com.glodblock.github.appflux.common.me.key.FluxKey.of(com.glodblock.github.appflux.common.me.key.type.EnergyType.FE);
            for (var provider : providers) {
                provider.getMainNode().getGrid().getStorageService().addGlobalStorageProvider(mounts -> mounts.mount(new appeng.api.storage.MEStorage() {
                    private long remaining = 100_000_000;
                    @Override public long extract(AEKey key, long amount, appeng.api.config.Actionable mode, appeng.api.networking.security.IActionSource source) {
                        if (!key.equals(flux)) return 0;
                        long taken = Math.min(amount, remaining);
                        if (mode == appeng.api.config.Actionable.MODULATE) remaining -= taken;
                        return taken;
                    }
                    @Override public void getAvailableStacks(KeyCounter out) { out.add(flux, remaining); }
                    @Override public net.minecraft.network.chat.Component getDescription() { return net.minecraft.network.chat.Component.literal("Test energy storage"); }
                }, 0));
                try {
                    var inventory = ((IUpgradeableObject)provider.getClass().getMethod("getLogic").invoke(provider)).getUpgrades();
                    h.assertTrue(inventory.addItems(new ItemStack(card)).isEmpty(), "Induction card was rejected");
                    upgrades.add(inventory);
                } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
            }
        }).thenIdle(20).thenExecute(() -> {
            for (int i = 0; i < hatches.size(); i++) {
                h.assertTrue(hatches.get(i).getEnergyComponent().getEu() > 0, "Induction card supplied no energy: " + providers.get(i).getBlockState());
                hatches.get(i).getEnergyComponent().consumeEu(Long.MAX_VALUE, aztech.modern_industrialization.util.Simulation.ACT);
            }
        }).thenIdle(20).thenExecute(() -> {
            for (int i = 0; i < hatches.size(); i++) {
                h.assertTrue(hatches.get(i).getEnergyComponent().getEu() > 0, "Induction card only worked once: " + providers.get(i).getBlockState());
                upgrades.get(i).clear();
                hatches.get(i).getEnergyComponent().consumeEu(Long.MAX_VALUE, aztech.modern_industrialization.util.Simulation.ACT);
            }
        }).thenIdle(20).thenExecute(() -> {
            for (var hatch : hatches) h.assertTrue(hatch.getEnergyComponent().getEu() == 0, "Removed induction card still supplied energy");
        }).thenSucceed();
    }
}
