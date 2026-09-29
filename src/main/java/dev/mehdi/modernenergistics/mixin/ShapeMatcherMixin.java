package dev.mehdi.modernenergistics.mixin;

import aztech.modern_industrialization.machines.multiblocks.*;
import aztech.modern_industrialization.machines.blockentities.multiblocks.AbstractCraftingMultiblockBlockEntity;
import dev.mehdi.modernenergistics.block.BridgeHatchEntity;
import dev.mehdi.modernenergistics.core.CrafterBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = ShapeMatcher.class, remap = false)
public abstract class ShapeMatcherMixin {
    @Shadow @Final protected BlockPos controllerPos;
    @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
    private void miae$onlyCrafters(BlockPos pos, Level world, CallbackInfoReturnable<Boolean> cir) {
        if (world.getBlockEntity(pos) instanceof BridgeHatchEntity
                && dev.mehdi.modernenergistics.core.MachineBridge.find(world.getBlockEntity(controllerPos)) == null) cir.setReturnValue(false);
    }
    @Inject(method = "rematch", at = @At("TAIL"))
    private void miae$link(Level world, CallbackInfo ci) {
        var bridge = dev.mehdi.modernenergistics.core.MachineBridge.find(world.getBlockEntity(controllerPos));
        if (bridge == null) return;
        var matcher = (ShapeMatcher)(Object)this;
        bridge.hatches.clear();
        if (matcher.isMatchSuccessful()) for (var hatch : matcher.getMatchedHatches())
            if (hatch instanceof BridgeHatchEntity combined) {
                bridge.hatches.add(combined);
                combined.bind(bridge, matcher);
            }
        bridge.managed = !bridge.hatches.isEmpty() || bridge.lock.busy();
        bridge.dirty();
    }
}
