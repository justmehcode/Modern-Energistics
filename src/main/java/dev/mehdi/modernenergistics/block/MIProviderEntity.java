package dev.mehdi.modernenergistics.block;

import appeng.blockentity.crafting.PatternProviderBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import java.util.EnumSet;

public class MIProviderEntity extends PatternProviderBlockEntity implements SingleMachineProvider {
    public MIProviderEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        dev.mehdi.modernenergistics.core.SmartBatching.register(getLogic(), this);
    }
    @Override public EnumSet<Direction> getTargets() { return SingleMachineProvider.targets(this, super.getTargets()); }
    public void bridgeTick() { SingleMachineProvider.tick(this, getTargets(), getLogic().getReturnInv()); }
    @Override public net.minecraft.world.item.ItemStack getMainMenuIcon() { return new net.minecraft.world.item.ItemStack(getBlockState().getBlock()); }
    @Override public appeng.api.stacks.AEItemKey getTerminalIcon() { return appeng.api.stacks.AEItemKey.of(getBlockState().getBlock()); }
}
