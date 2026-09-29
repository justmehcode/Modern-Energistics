package dev.mehdi.modernenergistics.test;

import dev.mehdi.modernenergistics.ModernEnergistics;
import dev.mehdi.modernenergistics.block.BridgeHatchEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(ModernEnergistics.ID)
@PrefixGameTestTemplate(false)
public class NamespaceMigrationGameTests {
    @GameTest(template = "bridge_test")
    public static void oldRegistryDataLoadsUnderNewNamespace(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        for (var entry : ModernEnergistics.BLOCKS.getEntries()) {
            var oldId = ResourceLocation.fromNamespaceAndPath("miaebridge", entry.getId().getPath());
            h.assertTrue(BuiltInRegistries.BLOCK.get(oldId) == entry.get(), "Old block ID did not resolve");
            var stateTag = NbtUtils.writeBlockState(entry.get().defaultBlockState());
            stateTag.putString("Name", oldId.toString());
            h.assertTrue(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), stateTag).getBlock() == entry.get(),
                    "Old blockstate did not deserialize");
            var itemTag = (net.minecraft.nbt.CompoundTag)new ItemStack(entry.get()).save(registries);
            itemTag.putString("id", oldId.toString());
            var restored = ItemStack.parseOptional(registries, itemTag);
            h.assertTrue(restored.is(entry.get().asItem()), "Old item stack did not deserialize");
            h.assertTrue(BuiltInRegistries.ITEM.getKey(restored.getItem()).getNamespace().equals(ModernEnergistics.ID),
                    "Restored stack still has the old registry ID");
        }
        var pos = new BlockPos(2, 1, 2);
        h.setBlock(pos, ModernEnergistics.HATCH.get());
        var hatch = (BridgeHatchEntity)h.getBlockEntity(pos);
        hatch.energyInput().receive(12345, false);
        var tag = hatch.saveWithFullMetadata(registries);
        tag.putString("id", "miaebridge:me_io_hatch");
        var restored = BlockEntity.loadStatic(hatch.getBlockPos(), hatch.getBlockState(), tag, registries);
        h.assertTrue(restored instanceof BridgeHatchEntity && ((BridgeHatchEntity)restored).getEnergyComponent().getEu() == 12345,
                "Legacy hatch block entity lost its type or stored energy");
        h.assertTrue(restored.saveWithFullMetadata(registries).getString("id").equals("modernenergistics:me_io_hatch"),
                "Restored hatch did not save its new ID");
        h.succeed();
    }
}
