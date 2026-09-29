package dev.mehdi.modernenergistics.compat;

import dev.mehdi.modernenergistics.block.SingleMachineProvider;
import net.pedroksl.advanced_ae.common.blocks.AdvPatternProviderBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AdvancedProviderBlock extends AdvPatternProviderBlock {
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof AdvancedProviderEntity provider)
                SingleMachineProvider.toggleLocks(provider, provider.getTargets(), player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
