package dev.mehdi.modernenergistics.mixin;

import dev.mehdi.modernenergistics.core.BatchTask;
import org.spongepowered.asm.mixin.*;

@Pseudo
@Mixin(targets = {"appeng.crafting.execution.ExecutingCraftingJob$TaskProgress",
        "net.pedroksl.advanced_ae.common.logic.ExecutingCraftingJob$TaskProgress"}, remap = false)
public abstract class BatchTaskMixin implements BatchTask {
    @Shadow(remap = false) long value;
    @Override public long me$remaining() { return value; }
    @Override public void me$remaining(long amount) { value = amount; }
}
