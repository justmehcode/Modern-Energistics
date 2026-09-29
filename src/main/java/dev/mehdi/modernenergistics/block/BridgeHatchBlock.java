package dev.mehdi.modernenergistics.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;

public class BridgeHatchBlock extends BaseEntityBlock {
    public static final MapCodec<BridgeHatchBlock> CODEC = simpleCodec(BridgeHatchBlock::new);
    public BridgeHatchBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BridgeHatchEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> { if (be instanceof BridgeHatchEntity hatch) hatch.tick(); };
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof BridgeHatchEntity hatch && player instanceof ServerPlayer serverPlayer) {
            var bridge = hatch.bridge();
            if (player.isShiftKeyDown() && bridge != null) {
                bridge.lock.setKeep(!bridge.lock.keep());
                bridge.refresh(); bridge.dirty();
            } else {
                hatch.openMenu(serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BridgeHatchEntity hatch)
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(hatch.saveWithFullMetadata(params.getLevel().registryAccess())));
        return List.of(stack);
    }
}
