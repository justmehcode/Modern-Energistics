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

public class EITestSupport {
    public static void run(GameTestHelper h) {
        var key = ResourceLocation.parse("extended_industrialization:large_electric_furnace");
        if (!BuiltInRegistries.BLOCK.containsKey(key)) { h.succeed(); return; }
        var pos = new BlockPos(7, 3, 7);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(key));
        var machine = (MultiblockMachineBlockEntity)h.getBlockEntity(pos);
        var shape = machine.createShapeMatcher();
        shape.buildMultiblock(h.getLevel());
        var hatchPos = shape.getPositions().stream().filter(p -> shape.getHatchFlags(p) != null
                && shape.getHatchFlags(p).allows(HatchTypes.ITEM_INPUT)).findFirst().orElseThrow();
        h.getLevel().setBlockAndUpdate(hatchPos, ModernEnergistics.HATCH.get().defaultBlockState());
        var side = Arrays.stream(Direction.values()).filter(d -> !shape.getPositions().contains(hatchPos.relative(d))
                && !hatchPos.relative(d).equals(machine.getBlockPos()) && !shape.getPositions().contains(hatchPos.relative(d, 2)))
                .findFirst().orElseThrow();
        var providerPos = hatchPos.relative(side);
        h.getLevel().setBlockAndUpdate(providerPos, AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());
        h.getLevel().setBlockAndUpdate(providerPos.relative(side), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        h.startSequence().thenIdle(12).thenExecute(() -> {
            var hatch = (BridgeHatchEntity)h.getLevel().getBlockEntity(hatchPos);
            var bridge = MachineBridge.find(machine);
            h.assertTrue(bridge != null && hatch.bridge() == bridge, "EI hatch did not bind");
            var ins = List.of(new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.COBBLESTONE), 4));
            var outs = List.of(new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.STONE), 4));
            var encoded = PatternDetailsHelper.encodeProcessingPattern(ins, outs);
            var pattern = PatternDetailsHelper.decodePattern(encoded, h.getLevel());
            var provider = (appeng.blockentity.crafting.PatternProviderBlockEntity)h.getLevel().getBlockEntity(providerPos);
            provider.getLogic().getPatternInv().setItemDirect(0, encoded);
            provider.getLogic().updatePatterns();
            var counter = new KeyCounter(); counter.add(ins.getFirst().what(), 4);
            h.assertTrue(provider.getLogic().pushPattern(pattern, new KeyCounter[]{counter}), "EI rejected batch: " + bridge.status);
            var crafter = (net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.MultipliedCrafterComponent)bridge.crafter;
            for (int i = 0; i < 50000 && bridge.lock.busy(); i++) {
                hatch.energyInput().receive(10000, false);
                crafter.tickRecipe();
            }
            h.assertTrue(!bridge.lock.busy(), "EI parallel batch accounting did not finish");
            h.assertTrue(bridge.inventory().getItemOutputs().stream().mapToLong(s -> s.getAmount()).sum() == 4, "EI produced incorrect output count");
            hatch.tick();
            h.assertTrue(bridge.outputsEmpty() && bridge.lock.recipe().isEmpty(), "EI output return/unlock failed");
            int warmEfficiency = crafter.getEfficiencyTicks();
            h.assertTrue(warmEfficiency > 0, "EI batch did not earn efficiency");
            for (int i = 0; i < 100; i++) {
                hatch.energyInput().receive(10000, false);
                crafter.tickRecipe();
            }
            h.assertTrue(crafter.getEfficiencyTicks() == warmEfficiency, "AE batch gap erased EI efficiency");
            h.assertTrue(bridge.outputsEmpty(), "Idle EI machine crafted without a batch");
            counter = new KeyCounter(); counter.add(ins.getFirst().what(), 4);
            h.assertTrue(provider.getLogic().pushPattern(pattern, new KeyCounter[]{counter}), "Warm EI machine rejected next batch");
            for (int i = 0; i < 50000 && bridge.lock.busy(); i++) {
                hatch.energyInput().receive(10000, false);
                crafter.tickRecipe();
            }
            h.assertTrue(!bridge.lock.busy() && crafter.getEfficiencyTicks() > warmEfficiency,
                    "EI efficiency did not build across batches");
            h.assertTrue(bridge.inventory().getItemOutputs().stream().mapToLong(s -> s.getAmount()).sum() == 4,
                    "Warm EI batch produced incorrect outputs");
            int beforeCooldown = crafter.getEfficiencyTicks();
            crafter.decreaseEfficiencyTicks();
            h.assertTrue(crafter.getEfficiencyTicks() == beforeCooldown - 1, "EI native cooldown was suppressed");
            hatch.tick();
            crafter.increaseEfficiencyTicks(crafter.getMaxEfficiencyTicks());
            int previousMaximum = crafter.getMaxEfficiencyTicks();
            var changedInputs = List.of(new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.COBBLESTONE), 1));
            var changedPattern = PatternDetailsHelper.decodePattern(PatternDetailsHelper.encodeProcessingPattern(changedInputs,
                    List.of(new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.DIAMOND), 1))), h.getLevel());
            var changedCounter = new KeyCounter(); changedCounter.add(changedInputs.getFirst().what(), 1);
            h.assertTrue(bridge.accepts(changedPattern, new KeyCounter[]{changedCounter}, false), "EI cap-change recipe rejected");
            for (int i = 0; i < 50000 && bridge.lock.busy(); i++) {
                hatch.energyInput().receive(10000, false);
                crafter.tickRecipe();
                h.assertTrue(crafter.getEfficiencyTicks() <= crafter.getMaxEfficiencyTicks(), "EI efficiency exceeded new maximum");
            }
            h.assertTrue(!bridge.lock.busy() && crafter.getMaxEfficiencyTicks() < previousMaximum, "EI test did not finish with a lower cap");
        }).thenSucceed();
    }
}
