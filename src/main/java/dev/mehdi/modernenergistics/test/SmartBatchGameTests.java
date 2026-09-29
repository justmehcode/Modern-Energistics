package dev.mehdi.modernenergistics.test;

import appeng.api.config.Actionable;
import appeng.api.crafting.*;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.*;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.crafting.CraftingPlan;
import appeng.crafting.inv.ListCraftingInventory;
import appeng.me.service.CraftingService;
import aztech.modern_industrialization.machines.blockentities.ElectricCraftingMachineBlockEntity;
import aztech.modern_industrialization.util.Simulation;
import dev.mehdi.modernenergistics.ModernEnergistics;
import dev.mehdi.modernenergistics.core.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(ModernEnergistics.ID)
@PrefixGameTestTemplate(false)
public class SmartBatchGameTests {
    public interface Cpu {
        ICraftingSubmitResult submit(ICraftingPlan plan);
        int execute();
        ListCraftingInventory inventory();
        void receive(AEKey key, long amount);
        boolean busy();
    }
    @GameTest(template = "bridge_test", timeoutTicks = 160)
    public static void thousandCraftsUse64Then40WithAllProviders(GameTestHelper h) { setup(h, false, false); }
    @GameTest(template = "bridge_test", timeoutTicks = 160)
    public static void smallerInputBufferUses32ThenRemainder(GameTestHelper h) { setup(h, false, true); }
    @GameTest(template = "bridge_test", timeoutTicks = 160)
    public static void advancedCpuBatchesItsOwnTaskLedger(GameTestHelper h) {
        if (!net.neoforged.fml.ModList.get().isLoaded("advanced_ae")) { h.succeed(); return; }
        setup(h, true, false);
    }
    @GameTest(template = "bridge_test", timeoutTicks = 160)
    public static void hatchUsesAll18InputSlots(GameTestHelper h) { hatch(h, 1, false); }
    @GameTest(template = "bridge_test", timeoutTicks = 160)
    public static void linkedHatchesCombine36InputSlots(GameTestHelper h) { hatch(h, 2, false); }
    @GameTest(template = "bridge_test", timeoutTicks = 160)
    public static void lockedHatchSlotsReduceCapacityToNineStacks(GameTestHelper h) { hatch(h, 1, true); }
    private static void hatch(GameTestHelper h, int hatchCount, boolean lockHalf) {
        var controller = new BlockPos(7, 3, 7);
        h.setBlock(controller, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("modern_industrialization:electric_blast_furnace")));
        var machine = (aztech.modern_industrialization.machines.blockentities.multiblocks.AbstractCraftingMultiblockBlockEntity)h.getBlockEntity(controller);
        var shape = machine.createShapeMatcher(); shape.buildMultiblock(h.getLevel());
        var positions = shape.getPositions().stream().filter(p -> shape.getHatchFlags(p) != null
                && shape.getHatchFlags(p).allows(aztech.modern_industrialization.machines.multiblocks.HatchTypes.ITEM_INPUT)).limit(hatchCount).toList();
        for (var p : positions) h.getLevel().setBlockAndUpdate(p, ModernEnergistics.HATCH.get().defaultBlockState());
        // Use a west-facing exterior position so the common CPU fixture remains two blocks east of its provider.
        var origin = positions.getFirst();
        var direction = Arrays.stream(Direction.values()).filter(d -> !shape.getPositions().contains(origin.relative(d))
                && !shape.getPositions().contains(origin.relative(d).east())
                && !shape.getPositions().contains(origin.relative(d).east(2))
                && !origin.relative(d).east().equals(origin) && !origin.relative(d).east(2).equals(origin)).findFirst().orElseThrow();
        var provider = origin.relative(direction);
        h.getLevel().setBlockAndUpdate(provider, ModernEnergistics.BLOCKS.getEntries().stream().filter(e -> e.getId().getPath().equals("mi_pattern_provider")).findFirst().orElseThrow().get().defaultBlockState());
        h.getLevel().setBlockAndUpdate(provider.east(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        h.getLevel().setBlockAndUpdate(provider.east(2), AEBlocks.CRAFTING_STORAGE_64K.block().defaultBlockState());
        h.startSequence().thenIdle(20).thenExecute(() -> {
            var hatch = (dev.mehdi.modernenergistics.block.BridgeHatchEntity)h.getLevel().getBlockEntity(origin);
            var bridge = hatch.bridge(); h.assertTrue(bridge != null, "Hatch test multiblock did not form");
            h.assertTrue(bridge.inventory().getItemInputs().size() == 18 * hatchCount, "Incorrect linked input slot count");
            if (lockHalf) for (int i = 9; i < 18; i++) bridge.inventory().getItemInputs().get(i).playerLock(Items.DIRT, Simulation.ACT);
            int capacity = (lockHalf ? 9 : 18 * hatchCount) * 64;
            runMachine(h, provider, false, false, bridge, () -> hatch.energyInput().receive(100000, false), capacity * 2 + 37, capacity);
        }).thenSucceed();
    }
    private static void setup(GameTestHelper h, boolean advancedCpu, boolean doubleInput) {
        var providers = new ArrayList<BlockPos>();
        int z = 1;
        for (String name : List.of("mi_pattern_provider", "advanced_mi_pattern_provider", "extended_mi_pattern_provider", "combined_mi_pattern_provider")) {
            var id = ModernEnergistics.id(name);
            if (!BuiltInRegistries.BLOCK.containsKey(id)) continue;
            h.setBlock(new BlockPos(2, 1, z), BuiltInRegistries.BLOCK.get(ResourceLocation.parse("modern_industrialization:electric_compressor")));
            h.setBlock(new BlockPos(3, 1, z), BuiltInRegistries.BLOCK.get(id));
            h.setBlock(new BlockPos(4, 1, z), AEBlocks.CREATIVE_ENERGY_CELL.block());
            h.setBlock(new BlockPos(5, 1, z), AEBlocks.CRAFTING_STORAGE_64K.block());
            providers.add(new BlockPos(3, 1, z)); z += 4;
        }
        h.startSequence().thenIdle(20).thenExecute(() -> {
            for (var pos : providers) run(h, pos, advancedCpu, doubleInput);
        }).thenSucceed();
    }
    private static void run(GameTestHelper h, BlockPos pos, boolean advancedCpu, boolean doubleInput) {
        var machine = (ElectricCraftingMachineBlockEntity)h.getBlockEntity(pos.west());
        runMachine(h, h.absolutePos(pos), advancedCpu, doubleInput, MachineBridge.find(machine),
                () -> machine.getEnergyComponent().insertEu(Long.MAX_VALUE, Simulation.ACT), doubleInput ? 65 : 1000, doubleInput ? 32 : 64);
    }
    private static void runMachine(GameTestHelper h, BlockPos pos, boolean advancedCpu, boolean doubleInput,
            MachineBridge bridge, Runnable power, int requested, int capacity) {
        var provider = (appeng.blockentity.grid.AENetworkedBlockEntity)h.getLevel().getBlockEntity(pos);
        var grid = provider.getMainNode().getGrid();
        h.assertTrue(grid != null && provider.getMainNode().isActive(), "Batch provider not online");
        var input = AEItemKey.of(Items.COBBLESTONE);
        var output = AEItemKey.of(doubleInput ? Items.EMERALD : Items.DIAMOND);
        int perCraft = doubleInput ? 2 : 1;
        var ins = List.of(new GenericStack(input, perCraft));
        var encoded = PatternDetailsHelper.encodeProcessingPattern(ins, List.of(new GenericStack(output, 1)));
        var details = PatternDetailsHelper.decodePattern(encoded, h.getLevel());
        ((appeng.helpers.patternprovider.PatternContainer)provider).getTerminalPatternInventory().setItemDirect(0, encoded);
        try { provider.getClass().getMethod("getLogic").invoke(provider).getClass().getMethod("updatePatterns")
                .invoke(provider.getClass().getMethod("getLogic").invoke(provider)); }
        catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        var storage = new ListCraftingInventory(ignored -> {});
        storage.insert(input, (long)requested * perCraft, Actionable.MODULATE);
        grid.getStorageService().addGlobalStorageProvider(mounts -> mounts.mount(new appeng.api.storage.MEStorage() {
            @Override public long extract(AEKey key, long amount, Actionable mode, IActionSource source) { return storage.extract(key, amount, mode); }
            @Override public long insert(AEKey key, long amount, Actionable mode, IActionSource source) { storage.insert(key, amount, mode); return amount; }
            @Override public void getAvailableStacks(KeyCounter counter) { counter.addAll(storage.list); }
            @Override public net.minecraft.network.chat.Component getDescription() { return net.minecraft.network.chat.Component.literal("Batch test storage"); }
        }, 0));
        var cluster = ((CraftingBlockEntity)h.getLevel().getBlockEntity(pos.east(2))).getCluster();
        h.assertTrue(cluster != null, "Test CPU did not form");
        Cpu cpu = advancedCpu ? AdvancedBatchTestSupport.create(grid, h.getLevel()) : new Cpu() {
            @Override public ICraftingSubmitResult submit(ICraftingPlan plan) { return cluster.craftingLogic.trySubmitJob(grid, plan, IActionSource.empty(), null); }
            @Override public int execute() { return cluster.craftingLogic.executeCrafting(1, (CraftingService)grid.getCraftingService(), grid.getEnergyService(), h.getLevel()); }
            @Override public ListCraftingInventory inventory() { return cluster.craftingLogic.getInventory(); }
            @Override public void receive(AEKey key, long amount) { cluster.craftingLogic.insert(key, amount, Actionable.MODULATE); }
            @Override public boolean busy() { return cluster.craftingLogic.hasJob(); }
        };
        var used = new KeyCounter(); used.add(input, (long)requested * perCraft);
        var result = cpu.submit(new CraftingPlan(new GenericStack(output, requested), 1024, false, false,
                used, new KeyCounter(), new KeyCounter(), Map.of(details, (long)requested)));
        h.assertTrue(result.successful(), "CPU rejected test job: " + result.errorCode());
        int completed = 0;
        while (completed < requested) {
            h.assertTrue(cpu.execute() == 1, "CPU did not dispatch a batch");
            int expected = Math.min(capacity, requested - completed);
            h.assertTrue(bridge.lock.remaining() == expected, "Wrong batch size: " + bridge.lock.remaining() + ", expected " + expected);
            long remainingInputs = (long)(requested - completed - expected) * perCraft;
            h.assertTrue(cpu.inventory().list.get(input) == remainingInputs, "CPU ingredients lost or duplicated");
            h.assertTrue(cpu.execute() == 0, "Busy machine accepted overlapping batch");
            h.assertTrue(cpu.inventory().list.get(input) == remainingInputs, "Rejected batch did not roll back CPU ingredients");
            for (int i = 0; i < 100000 && bridge.lock.busy(); i++) {
                power.run();
                ((aztech.modern_industrialization.machines.components.CrafterComponent)bridge.crafter).tickRecipe();
            }
            h.assertTrue(!bridge.lock.busy(), "Batched machine did not finish");
            var sink = new appeng.helpers.patternprovider.PatternProviderReturnInventory(() -> {});
            long delivered = 0;
            for (int pass = 0; pass < 100 && !bridge.outputsEmpty(); pass++) {
                OutputTransfer.move(bridge, sink);
                for (int i = 0; i < sink.size(); i++) if (sink.getKey(i) != null) {
                    h.assertTrue(output.equals(sink.getKey(i)), "Unexpected batch output");
                    delivered += sink.getAmount(i);
                }
                sink.clear();
            }
            h.assertTrue(delivered == expected && bridge.outputsEmpty(), "Incorrect batch outputs: " + delivered + ", expected " + expected);
            cpu.receive(output, expected);
            bridge.refresh(); completed += expected;
        }
        h.assertTrue(!cpu.busy() && cpu.inventory().list.get(input) == 0, "CPU did not finish exactly the requested job");
    }
}
