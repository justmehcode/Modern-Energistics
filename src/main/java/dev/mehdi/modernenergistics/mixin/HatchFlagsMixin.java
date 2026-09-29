package dev.mehdi.modernenergistics.mixin;

import aztech.modern_industrialization.machines.multiblocks.*;
import dev.mehdi.modernenergistics.ModernEnergistics;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.Set;

@Mixin(value = HatchFlags.class, remap = false)
public class HatchFlagsMixin {
    @Shadow @Final private Set<HatchType> allowed;
    @Inject(method = "allows", at = @At("HEAD"), cancellable = true)
    private void miae$allowCombined(HatchType type, CallbackInfoReturnable<Boolean> cir) {
        if (type == ModernEnergistics.HATCH_KIND) cir.setReturnValue(allowed.contains(HatchTypes.ITEM_INPUT)
                || allowed.contains(HatchTypes.ITEM_OUTPUT) || allowed.contains(HatchTypes.FLUID_INPUT)
                || allowed.contains(HatchTypes.FLUID_OUTPUT) || allowed.contains(HatchTypes.ENERGY_INPUT));
    }
}
