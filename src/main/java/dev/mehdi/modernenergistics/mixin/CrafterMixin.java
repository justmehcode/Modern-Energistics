package dev.mehdi.modernenergistics.mixin;

import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import dev.mehdi.modernenergistics.core.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.List;

@Mixin(value = CrafterComponent.class, remap = false)
public abstract class CrafterMixin implements CrafterBridge {
    @Shadow private long usedEnergy;
    @Shadow private int efficiencyTicks;
    @Shadow private int maxEfficiencyTicks;
    @Unique private int miae$idleEfficiency;
    @Unique private MachineBridge miae$bridge;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void miae$init(MachineBlockEntity owner, CrafterComponent.Inventory inv, CrafterComponent.Behavior behavior, CallbackInfo ci) {
        miae$bridge = new MachineBridge(owner, (CrafterComponent)(Object)this);
    }
    @Override public MachineBridge miae$bridge() { return miae$bridge; }
    @Unique private void miae$capEfficiency() {
        if (miae$bridge != null && (miae$bridge.managed || !miae$bridge.lock.recipe().isEmpty()))
            efficiencyTicks = Math.max(0, Math.min(efficiencyTicks, maxEfficiencyTicks));
    }
    @Inject(method = "updateActiveRecipe", at = @At(value = "FIELD",
            target = "Laztech/modern_industrialization/machines/components/CrafterComponent;maxEfficiencyTicks:I", opcode = 181, shift = At.Shift.AFTER))
    private void miae$capChangedRecipe(CallbackInfoReturnable<Boolean> cir) {
        // Cap immediately when the new recipe lowers the limit, before overclock calculation.
        miae$capEfficiency();
    }
    @Inject(method = "tickRecipe", at = @At(value = "INVOKE",
            target = "Laztech/modern_industrialization/machines/components/CrafterComponent;loadDelayedActiveRecipe()V", shift = At.Shift.AFTER))
    private void miae$rememberEfficiency(CallbackInfoReturnable<Boolean> cir) {
        miae$capEfficiency();
        miae$idleEfficiency = miae$bridge.managed && !miae$bridge.lock.busy() && usedEnergy == 0 ? efficiencyTicks : 0;
    }
    @Inject(method = "tickRecipe", at = @At(value = "INVOKE",
            target = "Laztech/modern_industrialization/machines/components/CrafterComponent;clearActiveRecipeIfPossible()V"))
    private void miae$keepIdleEfficiency(CallbackInfoReturnable<Boolean> cir) {
        // Preserve earned warm-up between AE jobs, within the machine's current tier limit.
        // Active jobs and the separate unformed-machine cooldown keep native behavior.
        if (miae$idleEfficiency > 0)
            efficiencyTicks = Math.max(efficiencyTicks, miae$idleEfficiency);
        miae$capEfficiency();
    }
    @Inject(method = "tickRecipe", at = @At("HEAD"))
    private void miae$refreshManagement(CallbackInfoReturnable<Boolean> cir) {
        if (miae$bridge.owner instanceof aztech.modern_industrialization.machines.blockentities.AbstractCraftingMachineBlockEntity) {
            boolean adjacent = false;
            for (var side : net.minecraft.core.Direction.values()) {
                var pos = miae$bridge.owner.getBlockPos().relative(side);
                if (miae$bridge.owner.getLevel().hasChunkAt(pos)
                        && miae$bridge.owner.getLevel().getBlockEntity(pos) instanceof dev.mehdi.modernenergistics.block.SingleMachineProvider) adjacent = true;
            }
            miae$bridge.managed = adjacent || miae$bridge.lock.busy();
        }
        miae$bridge.refresh();
    }
    @Inject(method = "getRecipes()Ljava/lang/Iterable;", at = @At("HEAD"), cancellable = true)
    private void miae$recipes(CallbackInfoReturnable<Iterable<RecipeHolder<MachineRecipe>>> cir) {
        if (!miae$bridge.managed && miae$bridge.lock.recipe().isEmpty()) return;
        miae$bridge.refresh();
        var recipe = miae$bridge.selected();
        cir.setReturnValue(miae$bridge.lock.busy() && recipe != null ? List.of(recipe) : List.of());
    }
    @Inject(method = "canStartRecipe", at = @At("HEAD"), cancellable = true)
    private void miae$guard(MachineRecipe recipe, CallbackInfoReturnable<Boolean> cir) {
        if (!miae$bridge.managed && miae$bridge.lock.recipe().isEmpty()) return;
        var selected = miae$bridge.selected();
        if (!miae$bridge.lock.busy() || selected == null || selected.value() != recipe) cir.setReturnValue(false);
    }
    @Inject(method = "tickRecipe", at = @At(value = "INVOKE", target = "Laztech/modern_industrialization/machines/components/CrafterComponent$Behavior;onCraft()V", shift = At.Shift.AFTER))
    private void miae$complete(CallbackInfoReturnable<Boolean> cir) { miae$bridge.completed(); }
    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void miae$save(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) { miae$bridge.save(tag, registries); }
    @Inject(method = "readNbt", at = @At("TAIL"))
    private void miae$load(CompoundTag tag, HolderLookup.Provider registries, boolean upgrading, CallbackInfo ci) { miae$bridge.load(tag, registries); miae$capEfficiency(); }
}
