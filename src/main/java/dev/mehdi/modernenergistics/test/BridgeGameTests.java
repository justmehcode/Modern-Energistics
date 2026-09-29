package dev.mehdi.modernenergistics.test;

import appeng.api.AECapabilities;
import appeng.api.crafting.*;
import appeng.api.stacks.*;
import appeng.core.definitions.AEBlocks;
import aztech.modern_industrialization.machines.blockentities.ElectricCraftingMachineBlockEntity;
import aztech.modern_industrialization.machines.blockentities.multiblocks.AbstractCraftingMultiblockBlockEntity;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import aztech.modern_industrialization.machines.multiblocks.HatchTypes;
import aztech.modern_industrialization.util.Simulation;
import dev.mehdi.modernenergistics.ModernEnergistics;
import dev.mehdi.modernenergistics.block.*;
import dev.mehdi.modernenergistics.core.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(ModernEnergistics.ID)
@PrefixGameTestTemplate(false)
public class BridgeGameTests {
    private static final String TEMPLATE = "bridge_test";
    @GameTest(template = TEMPLATE)
    public static void efficiencyNeverExceedsChangedRecipeMaximum(GameTestHelper h) {
        var machine = machine(h);
        h.setBlock(new BlockPos(3, 1, 2), ModernEnergistics.BLOCKS.getEntries().stream()
                .filter(e -> e.getId().getPath().equals("mi_pattern_provider")).findFirst().orElseThrow().get());
        var crafter = machine.getCrafterComponent();
        var bridge = ((CrafterBridge)crafter).miae$bridge();
        var ins = List.of(new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.COBBLESTONE), 1));
        int previousMaximum = 0;
        for (var output : List.of(net.minecraft.world.item.Items.DIAMOND, net.minecraft.world.item.Items.EMERALD)) {
            var pattern = PatternDetailsHelper.decodePattern(PatternDetailsHelper.encodeProcessingPattern(ins,
                    List.of(new GenericStack(AEItemKey.of(output), 1))), h.getLevel());
            h.assertTrue(bridge.accepts(pattern, counters(ins), false), "Efficiency test recipe rejected");
            for (int i = 0; i < 5000 && bridge.lock.busy(); i++) {
                machine.getEnergyComponent().insertEu(Long.MAX_VALUE, Simulation.ACT);
                crafter.tickRecipe();
                h.assertTrue(crafter.getEfficiencyTicks() <= crafter.getMaxEfficiencyTicks(),
                        "Efficiency exceeded recipe maximum after switching: " + crafter.getEfficiencyTicks() + "/" + crafter.getMaxEfficiencyTicks());
            }
            h.assertTrue(!bridge.lock.busy(), "Efficiency test recipe did not finish");
            if (previousMaximum > 0) h.assertTrue(crafter.getMaxEfficiencyTicks() < previousMaximum, "Test did not lower the efficiency cap");
            previousMaximum = crafter.getMaxEfficiencyTicks();
            crafter.increaseEfficiencyTicks(previousMaximum);
            OutputTransfer.move(bridge.inventory(), new appeng.helpers.patternprovider.PatternProviderReturnInventory(() -> {}), bridge::dirty);
            bridge.refresh();
        }
        // Reproduce an already-saved over-limit machine from the affected release.
        var saved = new CompoundTag(); crafter.writeNbt(saved, h.getLevel().registryAccess());
        saved.putInt("efficiencyTicks", crafter.getMaxEfficiencyTicks() + 100);
        crafter.readNbt(saved, h.getLevel().registryAccess(), false);
        crafter.tickRecipe();
        h.assertTrue(crafter.getEfficiencyTicks() == crafter.getMaxEfficiencyTicks(), "Saved excess efficiency was not capped");
        for (int i = 0; i < 100; i++) crafter.tickRecipe();
        h.assertTrue(crafter.getEfficiencyTicks() == crafter.getMaxEfficiencyTicks(), "Valid idle efficiency was lost");
        h.succeed();
    }
    private static ElectricCraftingMachineBlockEntity machine(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("modern_industrialization:electric_compressor")));
        return (ElectricCraftingMachineBlockEntity) h.getBlockEntity(new BlockPos(2, 1, 2));
    }
    private static RecipeHolder<MachineRecipe> recipe(ElectricCraftingMachineBlockEntity machine) {
        return machine.recipeType().getRecipesWithCache(machine.getCrafterWorld()).stream()
                .filter(r -> r.value().eu <= machine.getMaxRecipeEu() && r.value().conditions.isEmpty()
                        && r.value().itemInputs.size() == 1 && r.value().fluidInputs.isEmpty()
                        && r.value().itemOutputs.size() == 1 && r.value().fluidOutputs.isEmpty()
                        && r.value().itemInputs.getFirst().probability() == 1
                        && r.value().itemOutputs.getFirst().probability() == 1
                        && r.value().itemInputs.getFirst().amount() <= 16)
                .sorted(Comparator.comparing(r -> r.id().toString())).findFirst().orElseThrow();
    }
    private static List<GenericStack> inputs(MachineRecipe r, int multiplier) {
        return List.of(new GenericStack(AEItemKey.of(r.itemInputs.getFirst().ingredient().getItems()[0]),
                (long)r.itemInputs.getFirst().amount() * multiplier));
    }
    private static List<GenericStack> outputs(MachineRecipe r, int multiplier) {
        return List.of(new GenericStack(AEItemKey.of(r.itemOutputs.getFirst().getStack()),
                (long)r.itemOutputs.getFirst().amount() * multiplier));
    }
    private static KeyCounter[] counters(List<GenericStack> inputs) {
        return inputs.stream().map(s -> { var c = new KeyCounter(); c.add(s.what(), s.amount()); return c; }).toArray(KeyCounter[]::new);
    }
    @GameTest(template = TEMPLATE)
    public static void mixedResourcesRecipeSelectionAndSwitching(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("modern_industrialization:electric_mixer")));
        var machine = (ElectricCraftingMachineBlockEntity)h.getBlockEntity(new BlockPos(2, 1, 2));
        var bridge = ((CrafterBridge)machine.getCrafterComponent()).miae$bridge();
        var ins = List.of(new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.COBBLESTONE), 2),
                new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.DIRT), 3),
                new GenericStack(AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER), 300),
                new GenericStack(AEFluidKey.of(net.minecraft.world.level.material.Fluids.LAVA), 200));
        var outs = List.of(new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.STONE), 2),
                new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.OBSIDIAN), 1),
                new GenericStack(AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER), 100),
                new GenericStack(AEFluidKey.of(net.minecraft.world.level.material.Fluids.LAVA), 50));
        var pattern = PatternDetailsHelper.decodePattern(PatternDetailsHelper.encodeProcessingPattern(ins, outs), h.getLevel());
        var wrong = PatternDetailsHelper.decodePattern(PatternDetailsHelper.encodeProcessingPattern(ins,
                List.of(new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.DIAMOND), 1))), h.getLevel());
        h.assertTrue(!bridge.accepts(wrong, counters(ins), false), "Wrong expected output was accepted");
        for (var tank : machine.getCrafterComponent().getInventory().getFluidInputs()) tank.enableMachineLock(net.minecraft.world.level.material.Fluids.WATER);
        h.assertTrue(!bridge.accepts(pattern, counters(ins), false), "Lava bypassed incompatible tank locks");
        h.assertTrue(machine.getCrafterComponent().getInventory().getItemInputs().stream().allMatch(s -> s.isEmpty()), "Items were not rolled back after fluid failure");
        h.assertTrue(machine.getCrafterComponent().getInventory().getFluidInputs().stream().allMatch(s -> s.isEmpty()), "Earlier fluid insertion was not rolled back");
        for (var tank : machine.getCrafterComponent().getInventory().getFluidInputs()) tank.disableMachineLock();
        h.assertTrue(bridge.accepts(pattern, counters(ins), false), "Mixed batch rejected: " + bridge.status);
        h.assertTrue(bridge.lock.recipe().equals("modernenergistics:test/mixed"), "Recipe selected from inputs alone");
        for (int i = 0; i < 200 && bridge.lock.busy(); i++) {
            machine.getEnergyComponent().insertEu(Long.MAX_VALUE, Simulation.ACT);
            machine.getCrafterComponent().tickRecipe();
        }
        h.assertTrue(!bridge.lock.busy(), "Mixed recipe not completed");
        var inv = machine.getCrafterComponent().getInventory();
        h.assertTrue(inv.getItemOutputs().stream().mapToLong(s -> s.getAmount()).sum() == 3, "Incorrect item outputs");
        h.assertTrue(inv.getFluidOutputs().stream().mapToLong(s -> s.getAmount()).sum() == 150, "Incorrect fluid outputs");
        bridge.refresh();
        h.assertTrue(!bridge.lock.recipe().isEmpty(), "Unlocked while outputs remain");
        var returns = new appeng.helpers.patternprovider.PatternProviderReturnInventory(() -> {});
        OutputTransfer.move(inv, returns, bridge::dirty);
        bridge.refresh();
        h.assertTrue(bridge.outputsEmpty() && bridge.lock.recipe().isEmpty(), "Mixed output did not drain/unlock");
        var alternate = PatternDetailsHelper.decodePattern(PatternDetailsHelper.encodeProcessingPattern(ins,
                List.of(new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.GRANITE), 2),
                        new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.ANDESITE), 1))), h.getLevel());
        h.assertTrue(bridge.accepts(alternate, counters(ins), false), "Could not switch recipe after draining");
        h.assertTrue(bridge.lock.recipe().equals("modernenergistics:test/mixed_alternate"), "Old recipe retained after switch");
        h.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void optionalProviderVariantsDispatch(GameTestHelper h) {
        var providers = new ArrayList<net.minecraft.world.level.block.entity.BlockEntity>();
        var machines = new ArrayList<ElectricCraftingMachineBlockEntity>();
        int z = 2;
        for (String id : List.of("advanced_mi_pattern_provider", "extended_mi_pattern_provider", "combined_mi_pattern_provider")) {
            var key = ModernEnergistics.id(id);
            if (!BuiltInRegistries.BLOCK.containsKey(key)) continue;
            h.setBlock(new BlockPos(2, 1, z), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("modern_industrialization:electric_compressor")));
            h.setBlock(new BlockPos(3, 1, z), BuiltInRegistries.BLOCK.get(key));
            h.setBlock(new BlockPos(4, 1, z), AEBlocks.CREATIVE_ENERGY_CELL.block());
            machines.add((ElectricCraftingMachineBlockEntity)h.getBlockEntity(new BlockPos(2, 1, z)));
            providers.add(h.getBlockEntity(new BlockPos(3, 1, z)));
            z += 4;
        }
        h.startSequence().thenIdle(12).thenExecute(() -> {
            for (int i = 0; i < providers.size(); i++) {
                var provider = providers.get(i);
                var machine = machines.get(i);
                var r = recipe(machine);
                var item = PatternDetailsHelper.encodeProcessingPattern(inputs(r.value(), 1), outputs(r.value(), 1));
                if (!BuiltInRegistries.BLOCK.getKey(provider.getBlockState().getBlock()).getPath().equals("extended_mi_pattern_provider"))
                    item = AdvancedTestSupport.encode(inputs(r.value(), 1), outputs(r.value(), 1));
                var pattern = PatternDetailsHelper.decodePattern(item, h.getLevel());
                var container = (appeng.helpers.patternprovider.PatternContainer)provider;
                int slots = BuiltInRegistries.BLOCK.getKey(provider.getBlockState().getBlock()).getPath()
                        .equals("advanced_mi_pattern_provider") ? 9 : 36;
                h.assertTrue(container.getTerminalPatternInventory().size() == slots, "Addon provider capacity incorrect");
                container.getTerminalPatternInventory().setItemDirect(0, item);
                try {
                    var logic = provider.getClass().getMethod("getLogic").invoke(provider);
                    logic.getClass().getMethod("updatePatterns").invoke(logic);
                    h.assertTrue(((appeng.api.networking.crafting.ICraftingProvider)logic).pushPattern(pattern, counters(inputs(r.value(), 1))),
                            "Addon provider dispatch failed: " + provider.getBlockState());
                } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
                var bridge = ((CrafterBridge)machine.getCrafterComponent()).miae$bridge();
                h.assertTrue(bridge.lock.busy(), "Addon dispatch failed to acquire recipe lock");
            }
        }).thenSucceed();
    }
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void providerDispatchAndReturn(GameTestHelper h) {
        var machine = machine(h);
        h.setBlock(new BlockPos(3, 1, 2), BuiltInRegistries.BLOCK.get(ModernEnergistics.id("mi_pattern_provider")));
        h.setBlock(new BlockPos(4, 1, 2), AEBlocks.CREATIVE_ENERGY_CELL.block());
        var provider = (MIProviderEntity)h.getBlockEntity(new BlockPos(3, 1, 2));
        var r = recipe(machine);
        var item = PatternDetailsHelper.encodeProcessingPattern(inputs(r.value(), 1), outputs(r.value(), 1));
        var pattern = PatternDetailsHelper.decodePattern(item, h.getLevel());
        provider.getLogic().getPatternInv().setItemDirect(0, item);
        h.startSequence().thenIdle(12).thenExecute(() -> {
            provider.getLogic().updatePatterns();
            h.assertTrue(provider.getMainNode().isActive(), "Provider network must be active");
            h.assertTrue(provider.getLogic().pushPattern(pattern, counters(inputs(r.value(), 1))), "Native AE2 dispatch failed");
            var bridge = ((CrafterBridge)machine.getCrafterComponent()).miae$bridge();
            h.assertTrue(bridge.lock.recipe().equals(r.id().toString()), "Wrong recipe selected");
            h.assertTrue(!provider.getLogic().pushPattern(pattern, counters(inputs(r.value(), 1))), "Busy machine accepted a conflicting batch");
            for (int i = 0; i < 20_000 && bridge.lock.busy(); i++) {
                machine.getEnergyComponent().insertEu(Long.MAX_VALUE, Simulation.ACT);
                machine.getCrafterComponent().tickRecipe();
            }
            h.assertTrue(!bridge.lock.busy(), "Recipe never completed");
            h.assertTrue(!bridge.outputsEmpty(), "Output was not produced");
            provider.bridgeTick();
            h.assertTrue(bridge.outputsEmpty(), "Output was not returned to provider");
            h.assertTrue(bridge.lock.recipe().isEmpty(), "Dynamic recipe lock was not released");
            h.assertTrue(provider.getLogic().getReturnInv().getAmount(0) == r.value().itemOutputs.getFirst().amount(), "Wrong returned output count");
            var crafter = machine.getCrafterComponent();
            int warmEfficiency = crafter.getEfficiencyTicks();
            h.assertTrue(warmEfficiency > 0, "Single machine did not earn efficiency");
            for (int i = 0; i < 100; i++) crafter.tickRecipe();
            h.assertTrue(crafter.getEfficiencyTicks() == warmEfficiency, "Single-machine batch gap erased efficiency");
            h.assertTrue(bridge.outputsEmpty(), "Single machine crafted while awaiting a pattern");
            h.setBlock(new BlockPos(3, 1, 2), Blocks.AIR);
            crafter.tickRecipe();
            h.assertTrue(crafter.getEfficiencyTicks() < warmEfficiency, "Removing provider did not restore native cooldown");
        }).thenSucceed();
    }
    @GameTest(template = TEMPLATE)
    public static void hatchDropPreservesItemsAndFluids(GameTestHelper h) {
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModernEnergistics.HATCH.get());
        var hatch = (BridgeHatchEntity)h.getBlockEntity(pos);
        hatch.energyInput().receive(12345, false);
        var item = hatch.getInventory().getItemStacks().getFirst();
        item.setKey(aztech.modern_industrialization.thirdparty.fabrictransfer.api.item.ItemVariant.of(net.minecraft.world.item.Items.IRON_INGOT));
        item.setAmount(17);
        var fluid = hatch.getInventory().getFluidStacks().getFirst();
        fluid.setKey(aztech.modern_industrialization.thirdparty.fabrictransfer.api.fluid.FluidVariant.of(net.minecraft.world.level.material.Fluids.WATER));
        fluid.setAmount(45678);
        var drops = net.minecraft.world.level.block.Block.getDrops(hatch.getBlockState(), h.getLevel(), hatch.getBlockPos(), hatch);
        h.assertTrue(drops.size() == 1 && drops.getFirst().is(ModernEnergistics.HATCH.get().asItem()), "Hatch did not drop itself");
        var data = drops.getFirst().get(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA);
        h.assertTrue(data != null, "Hatch dropped without contents");
        var restored = (BridgeHatchEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(hatch.getBlockPos(),
                hatch.getBlockState(), data.copyTag(), h.getLevel().registryAccess());
        h.assertTrue(restored != null && restored.getInventory().getItemStacks().getFirst().getAmount() == 17, "Dropped hatch lost items");
        h.assertTrue(restored.getInventory().getFluidStacks().getFirst().getAmount() == 45678, "Dropped hatch lost fluids");
        h.assertTrue(restored.getEnergyComponent().getEu() == 12345, "Dropped hatch lost energy");
        h.succeed();
    }
    @GameTest(template = TEMPLATE)
    public static void failedInsertionRollsBackAndStatePersists(GameTestHelper h) {
        var machine = machine(h);
        var bridge = ((CrafterBridge)machine.getCrafterComponent()).miae$bridge();
        var r = recipe(machine);
        var pattern = PatternDetailsHelper.decodePattern(PatternDetailsHelper.encodeProcessingPattern(inputs(r.value(), 100), outputs(r.value(), 100)), h.getLevel());
        h.assertTrue(!bridge.accepts(pattern, counters(inputs(r.value(), 100)), false), "Oversized batch was accepted");
        h.assertTrue(machine.getCrafterComponent().getInventory().getItemInputs().stream().allMatch(s -> s.isEmpty()), "Failed insertion leaked inputs");
        h.assertTrue(!bridge.lock.busy(), "Failed insertion acquired a lock");
        bridge.lock.setKeep(true); bridge.lock.accept(r.id().toString(), 3, true); bridge.managed = true;
        var tag = new CompoundTag(); machine.getCrafterComponent().writeNbt(tag, h.getLevel().registryAccess());
        bridge.lock.restore("", 0, false);
        machine.getCrafterComponent().readNbt(tag, h.getLevel().registryAccess(), false);
        h.assertTrue(bridge.lock.remaining() == 3 && bridge.lock.keep(), "Batch did not survive MI save/load");
        h.succeed();
    }
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void multiblockHatchesLinkAndUnlink(GameTestHelper h) {
        multiblockDispatch(h, false);
    }
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void kanthalBlastFurnaceCombinedProviderAndEnergy(GameTestHelper h) {
        if (!BuiltInRegistries.BLOCK.containsKey(ModernEnergistics.id("combined_mi_pattern_provider"))) { h.succeed(); return; }
        multiblockDispatch(h, true);
    }
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void pressurizerReturnsReusableAirIntake(GameTestHelper h) {
        BlockPos pos = new BlockPos(7, 3, 7);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("modern_industrialization:pressurizer")));
        var machine = (AbstractCraftingMultiblockBlockEntity)h.getBlockEntity(pos);
        var shape = machine.createShapeMatcher();
        shape.buildMultiblock(h.getLevel());
        var hatchPos = shape.getPositions().stream().filter(p -> shape.getHatchFlags(p) != null
                && shape.getHatchFlags(p).allows(HatchTypes.ITEM_INPUT)).findFirst().orElseThrow();
        h.getLevel().setBlockAndUpdate(hatchPos, ModernEnergistics.HATCH.get().defaultBlockState());
        h.startSequence().thenIdle(12).thenExecute(() -> {
            var hatch = (BridgeHatchEntity)h.getLevel().getBlockEntity(hatchPos);
            var bridge = hatch.bridge();
            h.assertTrue(bridge != null, "Pressurizer did not form");
            var intake = AEItemKey.of(BuiltInRegistries.ITEM.get(ResourceLocation.parse("modern_industrialization:air_intake")));
            var air = AEFluidKey.of(BuiltInRegistries.FLUID.get(ResourceLocation.parse("modern_industrialization:liquid_air")));
            var ins = List.of(new GenericStack(intake, 1));
            var outs = List.of(new GenericStack(air, 1000), new GenericStack(intake, 1));
            var pattern = PatternDetailsHelper.decodePattern(PatternDetailsHelper.encodeProcessingPattern(ins, outs), h.getLevel());
            for (int attempt = 0; attempt < 2; attempt++) {
                var target = h.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, hatchPos, Direction.UP);
                h.assertTrue(target.pushPattern(pattern, counters(ins), Direction.UP), "Reusable intake rejected: " + bridge.status);
                var saved = new CompoundTag(); bridge.save(saved, h.getLevel().registryAccess());
                bridge.borrowed.clear(); bridge.load(saved, h.getLevel().registryAccess());
                h.assertTrue(bridge.borrowed.getOrDefault(intake, 0L) == 1, "Reload lost borrowed intake");
                var returns = new appeng.helpers.patternprovider.PatternProviderReturnInventory(() -> {});
                OutputTransfer.move(bridge, returns);
                h.assertTrue(bridge.borrowed.getOrDefault(intake, 0L) == 1, "Returned intake before recipe completion");
                for (int i = 0; i < 10000 && bridge.lock.busy(); i++) {
                    hatch.energyInput().receive(10000, false);
                    machine.getCrafterComponent().tickRecipe();
                }
                h.assertTrue(!bridge.lock.busy(), "Pressurizer never finished liquid air");
                h.assertTrue(!bridge.outputsEmpty(), "Unlocked before returning catalyst");
                OutputTransfer.move(bridge, returns); bridge.refresh();
                long tools = 0, fluid = 0;
                for (int i = 0; i < returns.size(); i++) {
                    if (intake.equals(returns.getKey(i))) tools += returns.getAmount(i);
                    if (air.equals(returns.getKey(i))) fluid += returns.getAmount(i);
                }
                h.assertTrue(tools == 1 && fluid == 1000, "Incorrect liquid air or returned intake amounts");
                h.assertTrue(bridge.outputsEmpty() && bridge.lock.recipe().isEmpty(), "Reusable job did not unlock");
                h.assertTrue(bridge.inventory().getItemInputs().stream().allMatch(v -> v.isEmpty()), "Borrowed intake remained in hatch");
            }
        }).thenSucceed();
    }
    @GameTest(template = TEMPLATE)
    public static void mixedCatalystsReturnExactlyAfter64Crafts(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 1, 2), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("modern_industrialization:electric_mixer")));
        var machine = (ElectricCraftingMachineBlockEntity)h.getBlockEntity(new BlockPos(2, 1, 2));
        var bridge = MachineBridge.find(machine);
        var diamond = AEItemKey.of(net.minecraft.world.item.Items.DIAMOND);
        var water = AEFluidKey.of(net.minecraft.world.level.material.Fluids.WATER);
        var ins = List.of(new GenericStack(diamond, 64),
                new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.COBBLESTONE), 64),
                new GenericStack(water, 6400),
                new GenericStack(AEFluidKey.of(net.minecraft.world.level.material.Fluids.LAVA), 640));
        var outs = List.of(new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.OBSIDIAN), 64),
                new GenericStack(diamond, 64), new GenericStack(water, 6400));
        var pattern = PatternDetailsHelper.decodePattern(PatternDetailsHelper.encodeProcessingPattern(ins, outs), h.getLevel());
        h.assertTrue(bridge.accepts(pattern, counters(ins), false), "Mixed catalyst batch rejected: " + bridge.status);
        for (int i = 0; i < 10000 && bridge.lock.busy(); i++) {
            machine.getEnergyComponent().insertEu(Long.MAX_VALUE, Simulation.ACT); machine.getCrafterComponent().tickRecipe();
        }
        h.assertTrue(!bridge.lock.busy(), "Mixed catalyst batch did not finish");
        var returns = new appeng.helpers.patternprovider.PatternProviderReturnInventory(() -> {});
        OutputTransfer.move(bridge, returns); bridge.refresh();
        var counts = new KeyCounter();
        for (int i = 0; i < returns.size(); i++) if (returns.getKey(i) != null) counts.add(returns.getKey(i), returns.getAmount(i));
        for (var expected : outs) h.assertTrue(counts.get(expected.what()) == expected.amount(), "Incorrect catalyst/output batch count");
        h.assertTrue(bridge.outputsEmpty() && bridge.inventory().getItemInputs().stream().allMatch(v -> v.isEmpty())
                && bridge.inventory().getFluidInputs().stream().allMatch(v -> v.isEmpty()), "Batch left borrowed inputs behind");
        h.succeed();
    }
    private static void multiblockDispatch(GameTestHelper h, boolean electric) {
        BlockPos controller = new BlockPos(7, 3, 7);
        h.setBlock(controller, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("modern_industrialization:"
                + (electric ? "electric_blast_furnace" : "steam_blast_furnace"))));
        var machine = (AbstractCraftingMultiblockBlockEntity)h.getBlockEntity(controller);
        if (electric) {
            var saved = machine.saveWithFullMetadata(h.getLevel().registryAccess());
            saved.putInt("activeShape", 1);
            machine.loadWithComponents(saved, h.getLevel().registryAccess());
        }
        var shape = machine.createShapeMatcher();
        shape.buildMultiblock(h.getLevel());
        var positions = shape.getPositions().stream().filter(p -> shape.getHatchFlags(p) != null
                && shape.getHatchFlags(p).allows(HatchTypes.ITEM_INPUT)).limit(2).toList();
        h.assertTrue(positions.size() == 2, "Expected two hatch positions");
        for (var p : positions) h.getLevel().setBlockAndUpdate(p, ModernEnergistics.HATCH.get().defaultBlockState());
        var returnSide = Arrays.stream(Direction.values()).filter(d -> !shape.getPositions().contains(positions.get(0).relative(d))
                && !positions.get(0).relative(d).equals(machine.getBlockPos())
                && !shape.getPositions().contains(positions.get(0).relative(d, 2))).findFirst().orElseThrow();
        var providerPos = positions.get(0).relative(returnSide);
        var providerBlock = electric ? BuiltInRegistries.BLOCK.get(ModernEnergistics.id("combined_mi_pattern_provider")) : AEBlocks.PATTERN_PROVIDER.block();
        h.getLevel().setBlockAndUpdate(providerPos, providerBlock.defaultBlockState());
        h.getLevel().setBlockAndUpdate(providerPos.relative(returnSide), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        h.startSequence().thenIdle(12).thenExecute(() -> {
            var first = (BridgeHatchEntity)h.getLevel().getBlockEntity(positions.get(0));
            var second = (BridgeHatchEntity)h.getLevel().getBlockEntity(positions.get(1));
            h.assertTrue(first.bridge() != null && first.bridge() == second.bridge(), "Hatches did not link to one controller");
            var inventory = machine.getCrafterComponent().getInventory();
            h.assertTrue(inventory.getItemInputs().size() == 36 && inventory.getItemOutputs().size() == 36, "Item I/O was not aggregated");
            h.assertTrue(inventory.getFluidInputs().size() == 16 && inventory.getFluidOutputs().size() == 16, "Fluid I/O was not aggregated");
            // Steam is fuel, not an unsolicited recipe ingredient. It must survive admission checks.
            var fuel = inventory.getFluidInputs().getFirst();
            if (!electric) {
            fuel.setKey(aztech.modern_industrialization.thirdparty.fabrictransfer.api.fluid.FluidVariant.of(
                    aztech.modern_industrialization.MIFluids.STEAM.asFluid()));
            fuel.setAmount(1_000_000);
            }
            var crafter = machine.getCrafterComponent();
            var recipe = crafter.getBehavior().recipeType().getRecipesWithCache(h.getLevel()).stream()
                    .filter(r -> !crafter.getBehavior().banRecipe(r.value()) && r.value().conditions.isEmpty()
                            && r.value().itemInputs.stream().allMatch(in -> in.probability() == 1)
                            && r.value().fluidInputs.stream().allMatch(in -> in.probability() == 1)
                            && r.value().itemOutputs.stream().allMatch(out -> out.probability() == 1)
                            && r.value().fluidOutputs.stream().allMatch(out -> out.probability() == 1))
                    .sorted(Comparator.comparing(r -> r.id().toString())).findFirst().orElseThrow().value();
            var ins = new ArrayList<GenericStack>();
            var outs = new ArrayList<GenericStack>();
            for (var in : recipe.itemInputs) ins.add(new GenericStack(AEItemKey.of(in.ingredient().getItems()[0]), in.amount()));
            for (var in : recipe.fluidInputs) ins.add(new GenericStack(AEFluidKey.of(in.fluid().getStacks()[0]), in.amount()));
            for (var out : recipe.itemOutputs) outs.add(new GenericStack(AEItemKey.of(out.getStack()), out.amount()));
            for (var out : recipe.fluidOutputs) outs.add(new GenericStack(AEFluidKey.of(out.fluid()), out.amount()));
            var pattern = PatternDetailsHelper.decodePattern(PatternDetailsHelper.encodeProcessingPattern(ins, outs), h.getLevel());
            var provider = h.getLevel().getBlockEntity(providerPos);
            ((appeng.helpers.patternprovider.PatternContainer)provider).getTerminalPatternInventory()
                    .setItemDirect(0, PatternDetailsHelper.encodeProcessingPattern(ins, outs));
            appeng.api.networking.crafting.ICraftingProvider providerLogic;
            try {
                var logic = provider.getClass().getMethod("getLogic").invoke(provider);
                logic.getClass().getMethod("updatePatterns").invoke(logic);
                providerLogic = (appeng.api.networking.crafting.ICraftingProvider)logic;
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
            var secondTarget = h.getLevel().getCapability(AECapabilities.CRAFTING_MACHINE, positions.get(1), Direction.UP);
            h.assertTrue(providerLogic.pushPattern(pattern, counters(ins)), "Provider-to-hatch dispatch rejected: " + first.bridge().status);
            h.assertTrue(providerPos.equals(first.bridge().returnPosition), "Dispatch saved the wrong output destination");
            h.assertTrue(!secondTarget.pushPattern(pattern, counters(ins), Direction.UP), "Second hatch accepted a duplicate concurrent batch");
            var energy = h.getLevel().getCapability(aztech.modern_industrialization.api.energy.EnergyApi.SIDED, first.getBlockPos(), Direction.UP);
            h.assertTrue(energy != null && energy.canReceive() && !energy.canExtract(), "Hatch energy input capability missing");
            if (electric) {
                h.assertTrue(energy.receive(1000, true) == 1000 && energy.getAmount() == 0, "Energy simulation modified storage");
                energy.receive(1000, false);
                crafter.tickRecipe();
                h.assertTrue(energy.getAmount() < 1000, "Blast furnace did not consume hatch energy");
                h.assertTrue(first.craftProgress() > 0, "Hatch does not report controller progress");
            }
            for (int i = 0; i < 200_000 && first.bridge().lock.busy(); i++) {
                if (electric) energy.receive(10000, false);
                crafter.tickRecipe();
            }
            h.assertTrue(!first.bridge().lock.busy(), "Steam multiblock recipe never finished");
            second.tick();
            h.assertTrue(first.bridge().outputsEmpty(), "Linked output hatch did not return all outputs to vanilla provider");
            h.assertTrue(first.bridge().lock.recipe().isEmpty(), "Multiblock lock did not release");
            if (electric) {
                int warmEfficiency = crafter.getEfficiencyTicks();
                h.assertTrue(warmEfficiency > 0, "Completed electric recipe did not earn efficiency");
                for (int i = 0; i < 100; i++) {
                    energy.receive(10000, false);
                    crafter.tickRecipe();
                }
                h.assertTrue(crafter.getEfficiencyTicks() == warmEfficiency, "AE batch gap erased MI efficiency");
                h.assertTrue(first.bridge().outputsEmpty(), "Idle machine crafted without a batch");
                h.assertTrue(secondTarget.pushPattern(pattern, counters(ins), Direction.UP), "Warm machine rejected next batch");
                for (int i = 0; i < 200_000 && first.bridge().lock.busy(); i++) {
                    energy.receive(10000, false);
                    crafter.tickRecipe();
                }
                h.assertTrue(!first.bridge().lock.busy(), "Warm machine failed to finish next batch");
                h.assertTrue(crafter.getEfficiencyTicks() > warmEfficiency, "Efficiency did not build across batches");
                int beforeCooldown = crafter.getEfficiencyTicks();
                crafter.decreaseEfficiencyTicks();
                h.assertTrue(crafter.getEfficiencyTicks() == beforeCooldown - 1, "Native unformed-machine cooldown was suppressed");
            }
            h.getLevel().setBlockAndUpdate(positions.get(0), Blocks.AIR.defaultBlockState());
        }).thenIdle(3).thenExecute(() -> {
            var second = (BridgeHatchEntity)h.getLevel().getBlockEntity(positions.get(1));
            h.assertTrue(second.bridge() == null, "Broken multiblock still accepts jobs");
        }).thenSucceed();
    }
}
