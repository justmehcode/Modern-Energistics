package dev.mehdi.modernenergistics;

import appeng.api.AECapabilities;
import appeng.api.behaviors.GenericInternalInventory;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import aztech.modern_industrialization.machines.blockentities.AbstractCraftingMachineBlockEntity;
import aztech.modern_industrialization.machines.multiblocks.*;
import dev.mehdi.modernenergistics.block.*;
import dev.mehdi.modernenergistics.compat.OptionalProviders;
import dev.mehdi.modernenergistics.core.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.registries.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

@Mod(ModernEnergistics.ID)
public class ModernEnergistics {
    public static final String ID = "modernenergistics";
    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(ID, path); }
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final HatchType HATCH_KIND = HatchTypes.register(id("combined_io"), Component.literal("ME Input/Output Hatch"));
    public static final DeferredBlock<BridgeHatchBlock> HATCH = BLOCKS.register("me_io_hatch",
            () -> new BridgeHatchBlock(Block.Properties.of().strength(4).sound(SoundType.METAL)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BridgeHatchEntity>> HATCH_ENTITY = ENTITIES.register("me_io_hatch",
            () -> BlockEntityType.Builder.of(BridgeHatchEntity::new, HATCH.get()).build(null));
    private static final List<Consumer<RegisterCapabilitiesEvent>> PROVIDER_CAPS = new ArrayList<>();

    public ModernEnergistics(IEventBus bus) {
        ITEMS.registerSimpleBlockItem(HATCH);
        provider("mi_pattern_provider", MIProviderBlock::new, MIProviderEntity.class, MIProviderEntity::new,
                e -> e.getLogic().getReturnInv(), MIProviderEntity::bridgeTick);
        boolean advanced = ModList.get().isLoaded("advanced_ae"), extended = ModList.get().isLoaded("extendedae");
        if (advanced) OptionalProviders.advanced();
        if (extended) OptionalProviders.extended();
        if (advanced && extended) OptionalProviders.combined();
        TABS.register("main", () -> CreativeModeTab.builder().title(Component.literal("Modern Energistics"))
                .icon(() -> new ItemStack(HATCH.get())).displayItems((p, out) -> ITEMS.getEntries().forEach(i -> out.accept(i.get()))).build());
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); TABS.register(bus);
        bus.addListener((ModifyRegistriesEvent event) -> {
            legacyAliases(event.getRegistry(Registries.BLOCK), BLOCKS);
            legacyAliases(event.getRegistry(Registries.ITEM), ITEMS);
            legacyAliases(event.getRegistry(Registries.BLOCK_ENTITY_TYPE), ENTITIES);
            legacyAliases(event.getRegistry(Registries.CREATIVE_MODE_TAB), TABS);
        });
        bus.addListener(this::capabilities);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent event) ->
                event.enqueueWork(dev.mehdi.modernenergistics.compat.ProviderUpgrades::register));
    }
    private static <T> void legacyAliases(Registry<T> registry, DeferredRegister<T> entries) {
        // Only new IDs are registered; aliases let existing serialized entries resolve to them.
        for (var entry : entries.getEntries())
            registry.addAlias(ResourceLocation.fromNamespaceAndPath("miaebridge", entry.getId().getPath()), entry.getId());
    }
    @FunctionalInterface public interface EntityFactory<T extends AEBaseBlockEntity> {
        T create(BlockEntityType<?> type, BlockPos pos, BlockState state);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends AEBaseBlockEntity & IInWorldGridNodeHost> void provider(String name,
            Supplier<? extends AEBaseEntityBlock<?>> blockFactory, Class<T> entityClass, EntityFactory<T> factory,
            Function<T, GenericInternalInventory> returns, Consumer<T> tick) {
        var block = BLOCKS.register(name, blockFactory);
        var item = ITEMS.registerSimpleBlockItem(block);
        var type = ENTITIES.register(name, () -> {
            AtomicReference<BlockEntityType<T>> ref = new AtomicReference<>();
            var result = BlockEntityType.Builder.<T>of((p, s) -> factory.create(ref.get(), p, s), block.get()).build(null);
            ref.set(result);
            ((AEBaseEntityBlock)block.get()).setBlockEntity(entityClass, result, null,
                    (BlockEntityTicker<T>)(l, p, s, e) -> tick.accept(e));
            AEBaseBlockEntity.registerBlockEntityItem(result, item.get());
            return result;
        });
        PROVIDER_CAPS.add(event -> {
            event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type.get(), (be, ctx) -> be);
            event.registerBlockEntity(AECapabilities.GENERIC_INTERNAL_INV, type.get(), (be, side) -> returns.apply(be));
        });
    }
    private void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(aztech.modern_industrialization.api.energy.EnergyApi.SIDED,
                HATCH_ENTITY.get(), (be, side) -> be.energyInput());
        event.registerBlockEntity(AECapabilities.CRAFTING_MACHINE, HATCH_ENTITY.get(), (be, side) -> new DispatchTarget(be::bridge, be.getBlockPos()));
        // External extraction is available, but pattern input always goes through the crafting capability.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, HATCH_ENTITY.get(), (be, side) -> be.getInventory().itemStorage.itemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, HATCH_ENTITY.get(), (be, side) -> be.getInventory().fluidStorage.fluidHandler);
        for (var register : PROVIDER_CAPS) register.accept(event);
        Block[] machines = BuiltInRegistries.BLOCK.stream().toArray(Block[]::new);
        event.registerBlock(AECapabilities.CRAFTING_MACHINE, (level, pos, state, be, side) -> {
            if (!(be instanceof AbstractCraftingMachineBlockEntity machine) || side == null) return null;
            if (!(level.getBlockEntity(pos.relative(side)) instanceof SingleMachineProvider)) return null;
            return new DispatchTarget(() -> level.getBlockEntity(pos.relative(side)) instanceof SingleMachineProvider
                    ? ((CrafterBridge)machine.getCrafterComponent()).miae$bridge() : null, pos);
        }, machines);
    }
}
