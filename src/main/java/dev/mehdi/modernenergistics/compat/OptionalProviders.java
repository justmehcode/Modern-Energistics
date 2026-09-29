package dev.mehdi.modernenergistics.compat;

import dev.mehdi.modernenergistics.ModernEnergistics;
import dev.mehdi.modernenergistics.block.MIProviderBlock;

public final class OptionalProviders {
    private OptionalProviders() {}
    public static void extended() {
        ModernEnergistics.provider("extended_mi_pattern_provider", MIProviderBlock::new, ExtendedProviderEntity.class,
                ExtendedProviderEntity::new, e -> e.getLogic().getReturnInv(), ExtendedProviderEntity::bridgeTick);
    }
    public static void advanced() {
        ModernEnergistics.provider("advanced_mi_pattern_provider", AdvancedProviderBlock::new, AdvancedProviderEntity.class,
                AdvancedProviderEntity::new, e -> e.getLogic().getReturnInv(), AdvancedProviderEntity::bridgeTick);
    }
    public static void combined() {
        ModernEnergistics.provider("combined_mi_pattern_provider", AdvancedProviderBlock::new, CombinedProviderEntity.class,
                CombinedProviderEntity::new, e -> e.getLogic().getReturnInv(), AdvancedProviderEntity::bridgeTick);
    }
}
