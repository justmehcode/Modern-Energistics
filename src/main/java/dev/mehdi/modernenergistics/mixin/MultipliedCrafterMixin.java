package dev.mehdi.modernenergistics.mixin;

import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import dev.mehdi.modernenergistics.core.CrafterBridge;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Collection;
import java.util.List;

@Pseudo
@Mixin(targets = "net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.MultipliedCrafterComponent", remap = false)
public abstract class MultipliedCrafterMixin {
    @Inject(method = "getRecipes", at = @At("HEAD"), cancellable = true)
    private void miae$recipes(CallbackInfoReturnable<Collection<RecipeHolder<MachineRecipe>>> ci) {
        var bridge = ((CrafterBridge)this).miae$bridge();
        if (bridge == null || (!bridge.managed && bridge.lock.recipe().isEmpty())) return;
        bridge.refresh();
        var selected = bridge.selected();
        ci.setReturnValue(bridge.lock.busy() && selected != null ? List.of(selected) : List.of());
    }
    @Inject(method = "canStartRecipe(Lnet/minecraft/world/item/crafting/RecipeHolder;Z)Z", at = @At("HEAD"), cancellable = true)
    private void miae$guard(RecipeHolder<MachineRecipe> recipe, boolean ignoreConditions, CallbackInfoReturnable<Boolean> ci) {
        var bridge = ((CrafterBridge)this).miae$bridge();
        if (bridge != null && (bridge.managed || !bridge.lock.recipe().isEmpty()) && !bridge.lock.permits(recipe.id().toString()))
            ci.setReturnValue(false);
    }
    @Inject(method = "calculateMultiplier", at = @At("RETURN"), cancellable = true)
    private void miae$limitBatch(RecipeHolder<MachineRecipe> recipe, CallbackInfoReturnable<Integer> ci) {
        var bridge = ((CrafterBridge)this).miae$bridge();
        if (bridge != null && bridge.lock.busy()) ci.setReturnValue((int)Math.min(ci.getReturnValue(), bridge.lock.remaining()));
    }
    @Inject(method = "tryStartRecipe(Lnet/minecraft/world/item/crafting/RecipeHolder;)Z", at = @At("HEAD"), cancellable = true)
    private void miae$guardCached(RecipeHolder<MachineRecipe> recipe, CallbackInfoReturnable<Boolean> ci) {
        var bridge = ((CrafterBridge)this).miae$bridge();
        if (bridge != null && (bridge.managed || !bridge.lock.recipe().isEmpty()) && !bridge.lock.permits(recipe.id().toString()))
            ci.setReturnValue(false);
    }
}
