package dev.mehdi.modernenergistics.core;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.*;
import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import java.util.*;

/** Matches amounts as well as identities. Ambiguous matches are rejected by the caller. */
public final class RecipeMatcher {
    private RecipeMatcher() {}

    public static Map<AEKey, Long> inputs(KeyCounter[] inputs) {
        Map<AEKey, Long> result = new LinkedHashMap<>();
        for (var counter : inputs) for (var entry : counter) {
            if (!(entry.getKey() instanceof AEItemKey || entry.getKey() instanceof AEFluidKey)
                    || entry.getLongValue() <= 0) throw new IllegalArgumentException("Unsupported input");
            result.merge(entry.getKey(), entry.getLongValue(), Math::addExact);
        }
        return result;
    }

    public record Match(long cycles, Map<AEKey, Long> borrowed) {}

    public static Match match(MachineRecipe recipe, IPatternDetails pattern, Map<AEKey, Long> supplied,
            CrafterComponent.Inventory inventory) {
        Map<AEKey, Long> guaranteed = new HashMap<>();
        for (var out : recipe.itemOutputs) if (out.probability() == 1)
            guaranteed.merge(AEItemKey.of(out.getStack()), (long) out.amount(), Math::addExact);
        for (var out : recipe.fluidOutputs) if (out.probability() == 1)
            guaranteed.merge(AEFluidKey.of(out.fluid()), out.amount(), Math::addExact);
        Map<AEKey, Long> expected = new HashMap<>();
        for (var out : pattern.getOutputs()) {
            if (out.amount() <= 0) return null;
            expected.merge(out.what(), out.amount(), Math::addExact);
        }
        if (expected.isEmpty()) return null;
        long cycles = 0;
        for (var out : expected.entrySet()) {
            long produced = guaranteed.getOrDefault(out.getKey(), 0L);
            if (produced <= 0) continue;
            if (out.getValue() % produced != 0) return null;
            long ratio = out.getValue() / produced;
            if (ratio <= 0 || ratio > 1_000_000 || (cycles != 0 && cycles != ratio)) return null;
            cycles = ratio;
        }
        if (cycles == 0) return null;
        Map<AEKey, Long> left = new LinkedHashMap<>(supplied);
        for (var in : recipe.itemInputs) if (in.probability() != 0) {
            if (in.probability() != 1 || !take(left, Math.multiplyExact(cycles, in.amount()),
                    key -> key instanceof AEItemKey item && in.matches(item.toStack()))) return null;
        }
        for (var in : recipe.fluidInputs) if (in.probability() != 0) {
            if (in.probability() != 1 || !take(left, Math.multiplyExact(cycles, in.amount()),
                    key -> key instanceof AEFluidKey fluid && in.fluid().test(fluid.toStack(1)))) return null;
        }
        Map<AEKey, Long> borrowed = new LinkedHashMap<>();
        for (var in : recipe.itemInputs) if (in.probability() == 0) {
            long stored = inventory.getItemInputs().stream().filter(v -> in.matches(v.toStack())).mapToLong(v -> v.getAmount()).sum();
            if (!catalyst(left, borrowed, stored, in.amount(), cycles,
                    key -> key instanceof AEItemKey item && in.matches(item.toStack()))) return null;
        }
        for (var in : recipe.fluidInputs) if (in.probability() == 0) {
            long stored = inventory.getFluidInputs().stream().filter(v -> in.fluid().test(v.toStack())).mapToLong(v -> v.getAmount()).sum();
            if (!catalyst(left, borrowed, stored, in.amount(), cycles,
                    key -> key instanceof AEFluidKey fluid && in.fluid().test(fluid.toStack(1)))) return null;
        }
        for (var out : expected.entrySet()) if (!guaranteed.containsKey(out.getKey())
                && out.getValue() > borrowed.getOrDefault(out.getKey(), 0L)) return null;
        return left.values().stream().allMatch(n -> n == 0) ? new Match(cycles, borrowed) : null;
    }
    private static boolean catalyst(Map<AEKey, Long> left, Map<AEKey, Long> borrowed, long stored,
            long amount, long cycles, java.util.function.Predicate<AEKey> matches) {
        long incoming = 0;
        for (var entry : left.entrySet()) if (matches.test(entry.getKey())) incoming = Math.addExact(incoming, entry.getValue());
        if (Math.addExact(stored, incoming) < amount || incoming > Math.multiplyExact(amount, cycles)) return false;
        for (var entry : left.entrySet()) if (entry.getValue() > 0 && matches.test(entry.getKey())) {
            borrowed.merge(entry.getKey(), entry.getValue(), Math::addExact);
            entry.setValue(0L);
        }
        return true;
    }

    private static boolean take(Map<AEKey, Long> left, long amount, java.util.function.Predicate<AEKey> match) {
        for (var entry : left.entrySet()) if (match.test(entry.getKey())) {
            long take = Math.min(amount, entry.getValue());
            entry.setValue(entry.getValue() - take);
            amount -= take;
            if (amount == 0) return true;
        }
        return amount == 0;
    }
}
