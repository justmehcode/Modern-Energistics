package dev.mehdi.modernenergistics.mixin;

import aztech.modern_industrialization.machines.components.CrafterComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CrafterComponent.class, remap = false)
public interface CrafterAccessor {
    @Accessor("usedEnergy") long miae$usedEnergy();
}
