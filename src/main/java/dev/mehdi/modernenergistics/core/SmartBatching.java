package dev.mehdi.modernenergistics.core;

import appeng.api.config.*;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.inv.ICraftingInventory;
import net.minecraft.world.level.Level;
import java.util.*;
import java.util.function.Predicate;

public final class SmartBatching {
    private static final int MAX_RECIPE_EXECUTIONS = 1_000_000;
    private static final Map<ICraftingProvider, java.lang.ref.WeakReference<net.minecraft.world.level.block.entity.BlockEntity>> PROVIDERS = Collections.synchronizedMap(new WeakHashMap<>());
    public static void register(ICraftingProvider provider, net.minecraft.world.level.block.entity.BlockEntity entity) {
        PROVIDERS.put(provider, new java.lang.ref.WeakReference<>(entity));
    }
    private record Extra(KeyCounter[] inputs, KeyCounter outputs, KeyCounter containers) {}

    public static boolean push(ICraftingProvider provider, IPatternDetails pattern, KeyCounter[] first,
            ICraftingInventory inventory, IEnergyService energy, Level level, BatchTask task,
            KeyCounter outputs, KeyCounter containers, Predicate<KeyCounter[]> send) {
        var reference = PROVIDERS.get(provider);
        var entity = reference == null ? null : reference.get();
        if (entity == null || !pattern.supportsPushInputsToExternalInventory() || task.me$remaining() <= 1)
            return send.test(first);
        var extras = new ArrayList<Extra>();
        boolean accepted = false;
        try {
            // Probe transactionally before extracting potentially large CPU reservations.
            int limit = maximum((int)Math.min(MAX_RECIPE_EXECUTIONS, task.me$remaining()), count -> {
                var scaled = scale(first, count);
                return fits(entity, pattern, scaled, count, energy);
            });
            if (limit == 0) return false;
            // Extract only from this CPU's reserved ingredients, never from general network storage.
            for (int i = 1; i < limit; i++) {
                var out = new KeyCounter(); var remainder = new KeyCounter();
                var inputs = CraftingCpuHelper.extractPatternInputs(pattern, inventory, level, out, remainder);
                if (inputs == null) break;
                extras.add(new Extra(inputs, out, remainder));
            }
            // Alternative ingredients can change slot packing; verify the actual extracted prefix.
            int count = maximum(extras.size() + 1, n -> fits(entity, pattern, combine(first, extras, n), n, energy));
            if (count == 0) return false;
            while (extras.size() >= count) {
                var removed = extras.removeLast();
                CraftingCpuHelper.reinjectPatternInputs(inventory, removed.inputs);
            }
            var merged = combine(first, extras, count);
            var combinedOutputs = new KeyCounter(); add(combinedOutputs, outputs);
            var combinedContainers = new KeyCounter(); add(combinedContainers, containers);
            for (var extra : extras) { add(combinedOutputs, extra.outputs); add(combinedContainers, extra.containers); }
            double totalPower = CraftingCpuHelper.calculatePatternPower(merged);
            if (!BatchDispatch.run(pattern, count, () -> send.test(merged))) return false;
            accepted = true;
            energy.extractAEPower(Math.max(0, totalPower - CraftingCpuHelper.calculatePatternPower(first)),
                    Actionable.MODULATE, PowerMultiplier.CONFIG);
            task.me$remaining(task.me$remaining() - extras.size());
            outputs.reset(); outputs.addAll(combinedOutputs);
            containers.reset(); containers.addAll(combinedContainers);
            return true;
        } catch (ArithmeticException ex) {
            // No commit occurs until all combined counts have been checked for overflow.
            return false;
        } finally {
            if (!accepted) for (var extra : extras) CraftingCpuHelper.reinjectPatternInputs(inventory, extra.inputs);
        }
    }
    private static boolean fits(net.minecraft.world.level.block.entity.BlockEntity entity, IPatternDetails pattern,
            KeyCounter[] inputs, int count, IEnergyService energy) {
        double power = CraftingCpuHelper.calculatePatternPower(inputs);
        return energy.extractAEPower(power, Actionable.SIMULATE, PowerMultiplier.CONFIG) >= power - 0.01
                && BatchDispatch.run(pattern, count, () -> dev.mehdi.modernenergistics.block.SingleMachineProvider.fits(entity, pattern, inputs));
    }
    private static int maximum(int upper, java.util.function.IntPredicate fits) {
        int low = 0, high = upper;
        while (low < high) {
            int mid = low + (high - low + 1) / 2;
            if (fits.test(mid)) low = mid; else high = mid - 1;
        }
        return low;
    }
    private static KeyCounter[] scale(KeyCounter[] first, int count) {
        var result = new KeyCounter[first.length];
        for (int i = 0; i < first.length; i++) {
            result[i] = new KeyCounter();
            for (var entry : first[i]) if (entry.getLongValue() > 0)
                result[i].add(entry.getKey(), Math.multiplyExact(entry.getLongValue(), count));
        }
        return result;
    }
    private static KeyCounter[] combine(KeyCounter[] first, List<Extra> extras, int count) {
        var result = scale(first, 1);
        for (int n = 0; n < count - 1; n++) for (int i = 0; i < first.length; i++) add(result[i], extras.get(n).inputs[i]);
        return result;
    }
    private static void add(KeyCounter target, KeyCounter source) {
        for (var entry : source) if (entry.getLongValue() > 0) {
            Math.addExact(target.get(entry.getKey()), entry.getLongValue());
            target.add(entry.getKey(), entry.getLongValue());
        }
    }
}
