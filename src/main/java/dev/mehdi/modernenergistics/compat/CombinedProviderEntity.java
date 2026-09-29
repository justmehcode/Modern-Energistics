package dev.mehdi.modernenergistics.compat;

import com.glodblock.github.extendedae.api.caps.IGenericInvHost;
import appeng.helpers.externalstorage.GenericStackInv;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** AdvancedAE scheduling and menu, plus ExtendedAE's generic-inventory host integration. */
public class CombinedProviderEntity extends AdvancedProviderEntity implements IGenericInvHost {
    public CombinedProviderEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state, 36); }
    @Override public GenericStackInv getGenericInv() { return getLogic().getReturnInv(); }
}
