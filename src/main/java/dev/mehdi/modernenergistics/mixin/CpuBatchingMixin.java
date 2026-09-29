package dev.mehdi.modernenergistics.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.inv.ListCraftingInventory;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.mehdi.modernenergistics.core.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import java.util.Map;

@Pseudo
@Mixin(targets = {"appeng.crafting.execution.CraftingCpuLogic",
        "net.pedroksl.advanced_ae.common.logic.AdvCraftingCPULogic"}, remap = false)
public abstract class CpuBatchingMixin {
    @Shadow(remap = false) @Final private ListCraftingInventory inventory;
    @WrapOperation(method = "executeCrafting", at = @At(value = "INVOKE",
            target = "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"))
    private boolean me$batch(ICraftingProvider provider, IPatternDetails pattern, KeyCounter[] inputs, Operation<Boolean> original,
            @Local Map.Entry<?, ?> task, @Local(ordinal = 0) KeyCounter expectedOutputs,
            @Local(ordinal = 1) KeyCounter expectedContainers, @Local(argsOnly = true) IEnergyService energy,
            @Local(argsOnly = true) Level level) {
        return SmartBatching.push(provider, pattern, inputs, inventory, energy, level, (BatchTask)task.getValue(),
                expectedOutputs, expectedContainers, batch -> original.call(provider, pattern, batch));
    }
}
