package dev.mehdi.modernenergistics.mixin;

import appeng.api.crafting.IPatternDetails;
import dev.mehdi.modernenergistics.core.BatchDispatch;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = {"appeng.helpers.patternprovider.PatternProviderLogic",
        "net.pedroksl.advanced_ae.common.logic.AdvPatternProviderLogic"}, remap = false)
public abstract class ProviderBatchLockMixin {
    @ModifyVariable(method = "onPushPatternSuccess", at = @At("HEAD"), argsOnly = true)
    private IPatternDetails me$batchOutputLock(IPatternDetails pattern) { return BatchDispatch.scaled(pattern); }
}
