package dev.mehdi.modernenergistics.mixin;

import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.components.CrafterComponent;
import dev.mehdi.modernenergistics.core.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.ModularCrafterAccessBehavior;
import net.swedz.tesseract.neoforge.compat.mi.component.craft.multiplied.MultipliedCrafterComponent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.swedz.tesseract.neoforge.compat.mi.component.craft.AbstractModularCrafterComponent", remap = false)
public abstract class ModularCrafterMixin implements CrafterBridge {
    @Shadow protected long usedEnergy;
    @Shadow protected int efficiencyTicks;
    @Shadow protected int maxEfficiencyTicks;
    @Unique private int miae$idleEfficiency;
    @Unique private MachineBridge miae$bridge;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void miae$init(MachineBlockEntity owner, CrafterComponent.Inventory inventory,
            ModularCrafterAccessBehavior behavior, CallbackInfo ci) {
        if ((Object)this instanceof MultipliedCrafterComponent crafter) {
            miae$bridge = new MachineBridge(owner, crafter, crafter::getRecipeType,
                    r -> behavior.isRecipeBanned(r.eu), () -> usedEnergy > 0);
            miae$bridge.steamFuel = owner instanceof net.swedz.tesseract.neoforge.compat.mi.api.SteamMachineTierHolder;
        }
    }
    @Override public MachineBridge miae$bridge() { return miae$bridge; }
    @Unique private void miae$capEfficiency() {
        if (miae$bridge != null && (miae$bridge.managed || !miae$bridge.lock.recipe().isEmpty()))
            efficiencyTicks = Math.max(0, Math.min(efficiencyTicks, maxEfficiencyTicks));
    }
    @Inject(method = "updateActiveRecipe", at = @At(value = "FIELD",
            target = "Lnet/swedz/tesseract/neoforge/compat/mi/component/craft/AbstractModularCrafterComponent;maxEfficiencyTicks:I", opcode = 181, shift = At.Shift.AFTER))
    private void miae$capChangedRecipe(CallbackInfoReturnable<Boolean> cir) {
        // Cap immediately when the new recipe lowers the limit, before overclock calculation.
        miae$capEfficiency();
    }
    @Inject(method = "tickRecipe", at = @At(value = "INVOKE",
            target = "Lnet/swedz/tesseract/neoforge/compat/mi/component/craft/AbstractModularCrafterComponent;loadDelayedActiveRecipe()V", shift = At.Shift.AFTER))
    private void miae$rememberEfficiency(CallbackInfoReturnable<Boolean> cir) {
        miae$capEfficiency();
        miae$idleEfficiency = miae$bridge != null && miae$bridge.managed && !miae$bridge.lock.busy()
                && usedEnergy == 0 ? efficiencyTicks : 0;
    }
    @Inject(method = "tickRecipe", at = @At(value = "INVOKE",
            target = "Lnet/swedz/tesseract/neoforge/compat/mi/component/craft/AbstractModularCrafterComponent;clearActiveRecipeIfPossible()V"))
    private void miae$keepIdleEfficiency(CallbackInfoReturnable<Boolean> cir) {
        if (miae$idleEfficiency > 0)
            efficiencyTicks = Math.max(efficiencyTicks, miae$idleEfficiency);
        miae$capEfficiency();
    }
    @Inject(method = "tickRecipe", at = @At("HEAD"))
    private void miae$refresh(CallbackInfoReturnable<Boolean> ci) { if (miae$bridge != null) miae$bridge.refresh(); }
    @Inject(method = "tickRecipe", at = @At(value = "INVOKE",
            target = "Lnet/swedz/tesseract/neoforge/compat/mi/component/craft/AbstractModularCrafterComponent;clearLocks()V"))
    private void miae$complete(CallbackInfoReturnable<Boolean> ci) {
        if (miae$bridge != null && (Object)this instanceof MultipliedCrafterComponent crafter) {
            for (int i = 0; i < crafter.getRecipeMultiplier() && miae$bridge.lock.busy(); i++) miae$bridge.completed();
        }
    }
    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void miae$save(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (miae$bridge != null) miae$bridge.save(tag, registries);
    }
    @Inject(method = "readNbt", at = @At("TAIL"))
    private void miae$load(CompoundTag tag, HolderLookup.Provider registries, boolean upgrading, CallbackInfo ci) {
        if (miae$bridge != null) miae$bridge.load(tag, registries);
        miae$capEfficiency();
    }
}
