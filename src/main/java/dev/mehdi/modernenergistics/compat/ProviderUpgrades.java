package dev.mehdi.modernenergistics.compat;

import appeng.api.upgrades.Upgrades;
import dev.mehdi.modernenergistics.ModernEnergistics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Mirror installed addons' native provider registrations after their common setup has completed. */
public final class ProviderUpgrades {
    private ProviderUpgrades() {}
    public static void register() {
        mirror("mi_pattern_provider", "ae2:pattern_provider");
        mirror("extended_mi_pattern_provider", "extendedae:ex_pattern_provider", "ae2:pattern_provider");
        mirror("advanced_mi_pattern_provider", "advanced_ae:small_adv_pattern_provider", "ae2:pattern_provider");
        mirror("combined_mi_pattern_provider", "advanced_ae:adv_pattern_provider", "extendedae:ex_pattern_provider", "ae2:pattern_provider");
    }
    private static void mirror(String targetId, String... sourceIds) {
        var targetKey = ModernEnergistics.id(targetId);
        if (!BuiltInRegistries.ITEM.containsKey(targetKey)) return;
        var target = BuiltInRegistries.ITEM.get(targetKey);
        for (var card : BuiltInRegistries.ITEM) {
            int maximum = 0;
            for (var sourceId : sourceIds) {
                var sourceKey = ResourceLocation.parse(sourceId);
                if (BuiltInRegistries.ITEM.containsKey(sourceKey))
                    maximum = Math.max(maximum, Upgrades.getMaxInstallable(card, BuiltInRegistries.ITEM.get(sourceKey)));
            }
            if (maximum > 0 && Upgrades.getMaxInstallable(card, target) == 0)
                Upgrades.add(card, target, maximum);
        }
    }
}
