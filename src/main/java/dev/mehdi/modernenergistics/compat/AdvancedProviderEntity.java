package dev.mehdi.modernenergistics.compat;

import dev.mehdi.modernenergistics.block.SingleMachineProvider;
import net.pedroksl.advanced_ae.common.entities.AdvPatternProviderEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import appeng.api.stacks.AEItemKey;
import java.util.EnumSet;

public class AdvancedProviderEntity extends AdvPatternProviderEntity implements SingleMachineProvider {
    private final appeng.util.inv.AppEngInternalInventory overflow = new appeng.util.inv.AppEngInternalInventory(27);
    public AdvancedProviderEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { this(type, pos, state, 9); }
    protected AdvancedProviderEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
        dev.mehdi.modernenergistics.core.SmartBatching.register(getLogic(), this);
    }
    @Override public void loadTag(net.minecraft.nbt.CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);
        overflow.clear();
        overflow.readFromNBT(data, "miae_pattern_overflow", registries);
        if (getTerminalPatternInventory().size() == 9) {
            var oldPatterns = new appeng.util.inv.AppEngInternalInventory(36);
            oldPatterns.readFromNBT(data, "patterns", registries);
            for (int i = 9; i < 36; i++) if (!oldPatterns.getStackInSlot(i).isEmpty())
                overflow.setItemDirect(i - 9, oldPatterns.getStackInSlot(i));
        }
    }
    @Override public void saveAdditional(net.minecraft.nbt.CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        overflow.writeToNBT(data, "miae_pattern_overflow", registries);
    }
    @Override public void addAdditionalDrops(net.minecraft.world.level.Level level, BlockPos pos, java.util.List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (var stack : overflow) if (!stack.isEmpty()) drops.add(stack.copy());
    }
    @Override public void clearContent() { super.clearContent(); overflow.clear(); }
    @Override public EnumSet<Direction> getTargets() { return SingleMachineProvider.targets(this, super.getTargets()); }
    public void bridgeTick() { SingleMachineProvider.tick(this, getTargets(), getLogic().getReturnInv()); }
    @Override public ItemStack getMainMenuIcon() { return new ItemStack(getBlockState().getBlock()); }
    @Override public AEItemKey getTerminalIcon() { return AEItemKey.of(getBlockState().getBlock()); }
}
