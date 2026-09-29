package dev.mehdi.modernenergistics.core;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.*;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Scoped to one synchronous provider call; never stored on a pattern or shared between CPUs. */
public final class BatchDispatch {
    private record Context(IPatternDetails pattern, int count) {}
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();
    public static boolean run(IPatternDetails pattern, int count, BooleanSupplier action) {
        var previous = CURRENT.get();
        CURRENT.set(new Context(pattern, count));
        try { return action.getAsBoolean(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    public static IPatternDetails scaled(IPatternDetails pattern) {
        var context = CURRENT.get();
        if (context == null || context.pattern != pattern || context.count == 1) return pattern;
        var outputs = pattern.getOutputs().stream()
                .map(s -> new GenericStack(s.what(), Math.multiplyExact(s.amount(), context.count))).toList();
        return new IPatternDetails() {
            @Override public AEItemKey getDefinition() { return pattern.getDefinition(); }
            @Override public IInput[] getInputs() { return pattern.getInputs(); }
            @Override public List<GenericStack> getOutputs() { return outputs; }
            @Override public boolean supportsPushInputsToExternalInventory() { return pattern.supportsPushInputsToExternalInventory(); }
        };
    }
}
