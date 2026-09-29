package dev.mehdi.modernenergistics.test;

import appeng.api.stacks.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class AdvancedTestSupport {
    public static ItemStack encode(List<GenericStack> inputs, List<GenericStack> outputs) {
        var sides = new HashMap<AEKey, Direction>();
        for (var input : inputs) sides.put(input.what(), Direction.NORTH);
        return net.pedroksl.advanced_ae.common.patterns.AdvPatternDetailsEncoder.encodeProcessingPattern(inputs, outputs, sides);
    }
}
