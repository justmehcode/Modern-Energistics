package dev.mehdi.modernenergistics.compat;

import appeng.api.stacks.AEItemKey;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.*;
import appeng.menu.locator.MenuHostLocator;
import com.glodblock.github.extendedae.api.caps.IGenericInvHost;
import com.glodblock.github.extendedae.container.ContainerExPatternProvider;
import dev.mehdi.modernenergistics.block.MIProviderEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ExtendedProviderEntity extends MIProviderEntity implements IGenericInvHost {
    public ExtendedProviderEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override protected PatternProviderLogic createLogic() { return new PatternProviderLogic(getMainNode(), this, 36); }
    @Override public GenericStackInv getGenericInv() { return getLogic().getReturnInv(); }
    @Override public void openMenu(Player player, MenuHostLocator locator) { MenuOpener.open(ContainerExPatternProvider.TYPE, player, locator); }
    @Override public void returnToMainMenu(Player player, ISubMenu menu) { MenuOpener.returnTo(ContainerExPatternProvider.TYPE, player, menu.getLocator()); }
    @Override public ItemStack getMainMenuIcon() { return new ItemStack(getBlockState().getBlock()); }
    @Override public AEItemKey getTerminalIcon() { return AEItemKey.of(getBlockState().getBlock()); }
}
