package dev.mehdi.modernenergistics.core;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.*;
import aztech.modern_industrialization.inventory.*;
import aztech.modern_industrialization.machines.MachineBlockEntity;
import aztech.modern_industrialization.machines.components.CrafterComponent;
import aztech.modern_industrialization.machines.recipe.MachineRecipe;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.item.ItemVariant;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.fluid.FluidVariant;
import aztech.modern_industrialization.thirdparty.fabrictransfer.api.transaction.Transaction;
import dev.mehdi.modernenergistics.block.BridgeHatchEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import java.util.*;

public final class MachineBridge {
    public final BatchLock lock = new BatchLock();
    public final MachineBlockEntity owner;
    public final aztech.modern_industrialization.api.machine.component.CrafterAccess crafter;
    private final java.util.function.Supplier<aztech.modern_industrialization.machines.recipe.MachineRecipeType> recipeType;
    private final java.util.function.Predicate<MachineRecipe> banned;
    private final java.util.function.BooleanSupplier running;
    public final List<BridgeHatchEntity> hatches = new ArrayList<>();
    public final Map<AEKey, Long> borrowed = new LinkedHashMap<>();
    public boolean managed;
    public boolean steamFuel;
    public net.minecraft.core.BlockPos returnPosition;
    public net.minecraft.core.Direction returnSide;
    public String status = "Waiting for a processing pattern";

    public MachineBridge(MachineBlockEntity owner, CrafterComponent crafter) {
        this(owner, crafter, () -> crafter.getBehavior().recipeType(), r -> crafter.getBehavior().banRecipe(r),
                () -> ((dev.mehdi.modernenergistics.mixin.CrafterAccessor)crafter).miae$usedEnergy() > 0);
    }
    public MachineBridge(MachineBlockEntity owner, aztech.modern_industrialization.api.machine.component.CrafterAccess crafter,
            java.util.function.Supplier<aztech.modern_industrialization.machines.recipe.MachineRecipeType> recipeType,
            java.util.function.Predicate<MachineRecipe> banned, java.util.function.BooleanSupplier running) {
        this.owner = owner;
        this.crafter = crafter;
        this.recipeType = recipeType;
        this.banned = banned;
        this.running = running;
    }
    public CrafterComponent.Inventory inventory() { return (CrafterComponent.Inventory)crafter.getInventory(); }
    public boolean running() { return running.getAsBoolean(); }
    public static MachineBridge find(net.minecraft.world.level.block.entity.BlockEntity entity) {
        return entity instanceof aztech.modern_industrialization.api.machine.holder.CrafterComponentHolder holder
                && holder.getCrafterComponent() instanceof CrafterBridge bridge ? bridge.miae$bridge() : null;
    }
    public boolean outputsEmpty() {
        var inv = inventory();
        return borrowed.isEmpty() && inv.getItemOutputs().stream().allMatch(AbstractConfigurableStack::isEmpty)
                && inv.getFluidOutputs().stream().allMatch(AbstractConfigurableStack::isEmpty);
    }
    public void dirty() {
        owner.setChanged();
        for (var hatch : hatches) hatch.setChanged();
    }
    public void completed() {
        if (lock.busy()) { lock.completeRecipe(); dirty(); }
    }
    public void refresh() {
        String before = lock.recipe();
        lock.refresh(outputsEmpty());
        if (!before.equals(lock.recipe())) dirty();
        if (!lock.busy() && !before.isEmpty()) status = outputsEmpty()
                ? (lock.keep() ? "Ready; recipe lock retained" : "Waiting for a processing pattern")
                : "Waiting for output space";
    }
    public boolean accepts(IPatternDetails pattern, KeyCounter[] counters, boolean alreadyRunning) {
        return accepts(pattern, counters, alreadyRunning, false);
    }
    public boolean simulates(IPatternDetails pattern, KeyCounter[] counters) {
        String previousStatus = status;
        try { return accepts(pattern, counters, running(), true); }
        finally { status = previousStatus; }
    }
    private boolean accepts(IPatternDetails pattern, KeyCounter[] counters, boolean alreadyRunning, boolean simulate) {
        if (owner.getLevel() == null || owner.getLevel().isClientSide || alreadyRunning || lock.busy() || !outputsEmpty())
            return false;
        if (!pattern.supportsPushInputsToExternalInventory()) return false;
        try {
            var supplied = RecipeMatcher.inputs(counters);
            var type = recipeType.get();
            if (type == null) { status = "Configure the machine's recipe type first"; return false; }
            var world = (net.minecraft.server.level.ServerLevel)owner.getLevel();
            Set<RecipeHolder<MachineRecipe>> candidates = new HashSet<>(type.getFluidOnlyRecipes(world));
            for (var key : supplied.keySet()) if (key instanceof AEItemKey item)
                candidates.addAll(type.getMatchingRecipes(world, item.getItem()));
            // MI indexes recipes by their first item input, which can be a preloaded catalyst.
            for (var stack : inventory().getItemInputs()) if (!stack.isEmpty())
                candidates.addAll(type.getMatchingRecipes(world, stack.getResource().getItem()));
            RecipeHolder<MachineRecipe> selected = null;
            RecipeMatcher.Match match = null;
            for (var candidate : candidates) {
                if (banned.test(candidate.value())) continue;
                var count = RecipeMatcher.match(candidate.value(), pattern, supplied, inventory());
                if (count == null) continue;
                if (selected != null) { status = "Ambiguous recipe: include all guaranteed outputs in the pattern"; return false; }
                selected = candidate;
                match = count;
            }
            if (selected == null) { status = "No matching MI recipe (check amounts, outputs, tier and catalysts)"; return false; }
            if (!lock.canAccept(selected.id().toString(), true)) { status = "Keep Recipe Locked is enabled"; return false; }
            // Refuse unrelated pre-existing consumables: only preloaded non-consumable catalysts are allowed.
            var recipe = selected.value();
            for (var stack : inventory().getItemInputs()) if (!stack.isEmpty()
                    && recipe.itemInputs.stream().noneMatch(in -> in.probability() == 0 && in.matches(stack.toStack()))) {
                status = "Empty existing input items first (preloaded catalysts are allowed)"; return false;
            }
            for (var stack : inventory().getFluidInputs()) if (!stack.isEmpty() && !isSteamFuel(stack)
                    && recipe.fluidInputs.stream().noneMatch(in -> in.probability() == 0 && in.fluid().test(stack.toStack()))) {
                status = "Empty existing input fluids first"; return false;
            }
            var items = new MIItemStorage(inventory().getItemInputs());
            var fluids = new MIFluidStorage(inventory().getFluidInputs());
            try (var tx = Transaction.openRoot()) {
                for (var entry : supplied.entrySet()) {
                    long inserted = entry.getKey() instanceof AEItemKey item
                            ? items.insert(ItemVariant.of(item.toStack()), entry.getValue(), tx)
                            : fluids.insert(FluidVariant.of(((AEFluidKey) entry.getKey()).toStack(1)), entry.getValue(), tx);
                    if (inserted != entry.getValue()) { status = "Insufficient input capacity or incompatible slot locks"; return false; }
                }
                if (simulate) return true; // Closing the transaction rolls back every provisional insertion.
                tx.commit();
            }
            borrowed.putAll(match.borrowed());
            lock.accept(selected.id().toString(), match.cycles(), true);
            managed = true;
            status = "Processing " + selected.id();
            dirty();
            return true;
        } catch (ArithmeticException | IllegalArgumentException ex) {
            status = "Unsupported or overflowing pattern quantities";
            return false;
        }
    }
    private boolean isSteamFuel(ConfigurableFluidStack stack) {
        return (steamFuel || owner instanceof aztech.modern_industrialization.machines.blockentities.SteamCraftingMachineBlockEntity
                || owner instanceof aztech.modern_industrialization.machines.blockentities.multiblocks.SteamCraftingMultiblockBlockEntity)
                && stack.getResource().getFluid() == aztech.modern_industrialization.MIFluids.STEAM.asFluid();
    }
    public RecipeHolder<MachineRecipe> selected() {
        if (lock.recipe().isEmpty()) return null;
        var type = recipeType.get();
        return type == null ? null : type.getRecipe((net.minecraft.server.level.ServerLevel)owner.getLevel(), ResourceLocation.parse(lock.recipe()));
    }
    public void save(CompoundTag root, net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("recipe", lock.recipe());
        tag.putLong("remaining", lock.remaining());
        tag.putBoolean("keep", lock.keep());
        tag.putBoolean("managed", managed);
        if (returnPosition != null && returnSide != null) {
            tag.putLong("returnPosition", returnPosition.asLong());
            tag.putInt("returnSide", returnSide.ordinal());
        }
        var catalysts = new net.minecraft.nbt.ListTag();
        borrowed.forEach((key, count) -> catalysts.add(GenericStack.writeTag(registries, new GenericStack(key, count))));
        tag.put("borrowed", catalysts);
        root.put("miae_job", tag);
    }
    public void load(CompoundTag root, net.minecraft.core.HolderLookup.Provider registries) {
        var tag = root.getCompound("miae_job");
        lock.restore(tag.getString("recipe"), tag.getLong("remaining"), tag.getBoolean("keep"));
        managed = tag.getBoolean("managed");
        borrowed.clear();
        for (var entry : tag.getList("borrowed", 10)) {
            var stack = GenericStack.readTag(registries, (CompoundTag)entry);
            if (stack != null && stack.amount() > 0) borrowed.merge(stack.what(), stack.amount(), Math::addExact);
        }
        returnPosition = tag.contains("returnPosition") ? net.minecraft.core.BlockPos.of(tag.getLong("returnPosition")) : null;
        returnSide = returnPosition != null ? net.minecraft.core.Direction.from3DDataValue(tag.getInt("returnSide")) : null;
    }
}
