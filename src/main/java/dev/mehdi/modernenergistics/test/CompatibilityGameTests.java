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

@GameTestHolder(ModernEnergistics.ID)
@PrefixGameTestTemplate(false)
public class CompatibilityGameTests {
    @GameTest(template = "bridge_test")
    public static void advancedLegacyPatternOverflowSurvives(GameTestHelper h) {
        var id = ModernEnergistics.id("advanced_mi_pattern_provider");
        if (!BuiltInRegistries.BLOCK.containsKey(id)) { h.succeed(); return; }
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, BuiltInRegistries.BLOCK.get(id));
        var provider = (appeng.blockentity.AEBaseBlockEntity)h.getBlockEntity(pos);
        var encoded = PatternDetailsHelper.encodeProcessingPattern(
                List.of(new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.COBBLESTONE), 1)),
                List.of(new GenericStack(AEItemKey.of(net.minecraft.world.item.Items.STONE), 1)));
        var old = provider.saveWithFullMetadata(h.getLevel().registryAccess());
        var legacy = new appeng.util.inv.AppEngInternalInventory(36);
        legacy.setItemDirect(0, encoded.copy());
        legacy.setItemDirect(17, encoded.copy());
        legacy.writeToNBT(old, "patterns", h.getLevel().registryAccess());
        provider.loadWithComponents(old, h.getLevel().registryAccess());
        var current = provider.saveWithFullMetadata(h.getLevel().registryAccess());
        provider.loadWithComponents(current, h.getLevel().registryAccess());
        var drops = new ArrayList<ItemStack>();
        provider.addAdditionalDrops(h.getLevel(), provider.getBlockPos(), drops);
        h.assertTrue(drops.stream().filter(s -> ItemStack.isSameItemSameComponents(s, encoded)).mapToInt(ItemStack::getCount).sum() == 2,
                "Advanced capacity migration lost or duplicated patterns");
        h.assertTrue(((appeng.helpers.patternprovider.PatternContainer)provider).getTerminalPatternInventory().size() == 9,
                "Advanced provider did not shrink to nine active slots");
        h.succeed();
    }
    @GameTest(template = "bridge_test", timeoutTicks = 160)
    public static void inductionCardsSupplyHatchEnergyContinuously(GameTestHelper h) {
        if (!net.neoforged.fml.ModList.get().isLoaded("appflux")) { h.succeed(); return; }
        FluxTestSupport.run(h);
    }
    @GameTest(template = "bridge_test", timeoutTicks = 100)
    public static void inductionCardsInstallAndPersist(GameTestHelper h) {
        var key = ResourceLocation.parse("appflux:induction_card");
        if (!BuiltInRegistries.ITEM.containsKey(key)) { h.succeed(); return; }
        var card = BuiltInRegistries.ITEM.get(key);
        int z = 1;
        for (String name : List.of("mi_pattern_provider", "advanced_mi_pattern_provider", "extended_mi_pattern_provider", "combined_mi_pattern_provider")) {
            if (!BuiltInRegistries.BLOCK.containsKey(ModernEnergistics.id(name))) continue;
            BlockPos pos = new BlockPos(1, 1, z++);
            h.setBlock(pos, BuiltInRegistries.BLOCK.get(ModernEnergistics.id(name)));
            var be = h.getBlockEntity(pos);
            try {
                var logic = be.getClass().getMethod("getLogic").invoke(be);
                h.assertTrue(logic instanceof IUpgradeableObject, name + " lacks native upgrade hooks");
                var inventory = ((IUpgradeableObject)logic).getUpgrades();
                h.assertTrue(inventory.addItems(new ItemStack(card)).isEmpty(), name + " rejected induction card");
                h.assertTrue(inventory.isInstalled(card), name + " card did not activate");
                var saved = be.saveWithFullMetadata(h.getLevel().registryAccess());
                inventory.clear();
                be.loadWithComponents(saved, h.getLevel().registryAccess());
                h.assertTrue(inventory.isInstalled(card), name + " lost card on reload");
                inventory.clear();
                h.assertTrue(!inventory.isInstalled(card), name + " retained removed card effect");
            } catch (ReflectiveOperationException ex) { throw new RuntimeException(ex); }
        }
        h.succeed();
    }
    @GameTest(template = "bridge_test", timeoutTicks = 100)
    public static void extendedIndustrializationParallelCraft(GameTestHelper h) {
        if (!net.neoforged.fml.ModList.get().isLoaded("extended_industrialization")) { h.succeed(); return; }
        EITestSupport.run(h);
    }
}
