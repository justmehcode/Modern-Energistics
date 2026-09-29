package dev.mehdi.modernenergistics.block;

import aztech.modern_industrialization.inventory.*;
import aztech.modern_industrialization.machines.BEP;
import aztech.modern_industrialization.machines.components.*;
import aztech.modern_industrialization.machines.gui.MachineGuiParameters;
import aztech.modern_industrialization.api.energy.*;
import aztech.modern_industrialization.api.machine.holder.EnergyComponentHolder;
import aztech.modern_industrialization.machines.guicomponents.EnergyBar;
import aztech.modern_industrialization.machines.guicomponents.ProgressBar;
import aztech.modern_industrialization.machines.multiblocks.*;
import dev.mehdi.modernenergistics.ModernEnergistics;
import dev.mehdi.modernenergistics.core.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

public class BridgeHatchEntity extends HatchBlockEntity implements EnergyComponentHolder {
    public static final int ITEM_SLOTS = 18, FLUID_SLOTS = 8;
    public static final long TANK_CAPACITY = 1_024_000;
    private final MIInventory inventory;
    private final EnergyComponent energy;
    private final MIEnergyStorage energyInput;
    private MachineBridge controller;
    private ShapeMatcher matcher;

    public BridgeHatchEntity(BlockPos pos, BlockState state) {
        super(new BEP(ModernEnergistics.HATCH_ENTITY.get(), pos, state),
                new MachineGuiParameters.Builder(ModernEnergistics.id("me_io_hatch"), true).backgroundHeight(310).build(),
                OrientationComponent.Params.noFacing(false, false));
        var items = new ArrayList<ConfigurableItemStack>();
        var fluids = new ArrayList<ConfigurableFluidStack>();
        for (int i = 0; i < ITEM_SLOTS; i++) items.add(ConfigurableItemStack.standardInputSlot());
        for (int i = 0; i < ITEM_SLOTS; i++) items.add(ConfigurableItemStack.standardOutputSlot());
        for (int i = 0; i < FLUID_SLOTS; i++) fluids.add(ConfigurableFluidStack.standardInputSlot(TANK_CAPACITY));
        for (int i = 0; i < FLUID_SLOTS; i++) fluids.add(ConfigurableFluidStack.standardOutputSlot(TANK_CAPACITY));
        inventory = new MIInventory(items, fluids,
                new SlotPositions.Builder().addSlots(8, 30, 9, 2).addSlots(8, 80, 9, 2).build(),
                new SlotPositions.Builder().addSlots(8, 130, 8, 1).addSlots(8, 164, 8, 1).build());
        registerComponents(inventory);
        // Same buffer size as MI's HV energy input hatch; lower-tier cables remain usable.
        energy = new EnergyComponent(this, 600L * CableTier.HV.getEu());
        energyInput = new MIEnergyStorage.NoExtract() {
            @Override public long receive(long amount, boolean simulate) {
                return energy.insertEu(amount, simulate ? aztech.modern_industrialization.util.Simulation.SIMULATE
                        : aztech.modern_industrialization.util.Simulation.ACT);
            }
            @Override public boolean canReceive() { return true; }
            @Override public long getAmount() { return energy.getEu(); }
            @Override public long getCapacity() { return energy.getCapacity(); }
            @Override public boolean canConnect(CableTier tier) { return tier.compareTo(CableTier.HV) <= 0; }
        };
        registerComponents(energy);
        registerGuiComponent(new dev.mehdi.modernenergistics.gui.HatchStatus());
        registerGuiComponent(new EnergyBar(new EnergyBar.Params(8, 190), energy::getEu, energy::getCapacity));
        registerGuiComponent(new ProgressBar(new ProgressBar.Params(80, 188, "arrow"), this::craftProgress));
        registerGuiComponent(new aztech.modern_industrialization.machines.guicomponents.ShapeSelection(
                new aztech.modern_industrialization.machines.guicomponents.ShapeSelection.Behavior() {
                    @Override public void handleClick(int line, int delta) {
                        var target = bridge();
                        if (target != null && line == 0) {
                            target.lock.setKeep(delta > 0);
                            target.refresh(); target.dirty();
                        }
                    }
                    @Override public int getCurrentIndex(int line) {
                        return bridge() != null && bridge().lock.keep() ? 1 : 0;
                    }
                }, new aztech.modern_industrialization.machines.guicomponents.ShapeSelection.LineInfo(
                        List.of(net.minecraft.network.chat.Component.literal("Automatic recipe release"),
                                net.minecraft.network.chat.Component.literal("Keep recipe locked")), true)));
    }
    public void bind(MachineBridge bridge, ShapeMatcher matcher) { this.controller = bridge; this.matcher = matcher; }
    public MachineBridge bridge() {
        return controller != null && !controller.owner.isRemoved() && matcher != null && matcher.isMatchSuccessful()
                && matcher.getMatchedHatches().contains(this) ? controller : null;
    }
    @Override public void unlink() { super.unlink(); controller = null; matcher = null; }
    @Override public HatchType getHatchType() { return ModernEnergistics.HATCH_KIND; }
    @Override public boolean upgradesToSteel() { return true; }
    @Override public MIInventory getInventory() { return inventory; }
    @Override public EnergyComponent getEnergyComponent() { return energy; }
    public MIEnergyStorage energyInput() { return energyInput; }
    @Override public void appendEnergyInputs(List<EnergyComponent> list) { list.add(energy); }
    public float craftProgress() {
        var bridge = bridge();
        if (bridge == null) return 0;
        float progress = bridge.crafter.getProgress();
        return Float.isFinite(progress) ? Math.max(0, Math.min(1, progress)) : 0;
    }
    @Override public void appendItemInputs(List<ConfigurableItemStack> list) { list.addAll(inventory.getItemStacks().subList(0, ITEM_SLOTS)); }
    @Override public void appendItemOutputs(List<ConfigurableItemStack> list) { list.addAll(inventory.getItemStacks().subList(ITEM_SLOTS, ITEM_SLOTS * 2)); }
    @Override public void appendFluidInputs(List<ConfigurableFluidStack> list) { list.addAll(inventory.getFluidStacks().subList(0, FLUID_SLOTS)); }
    @Override public void appendFluidOutputs(List<ConfigurableFluidStack> list) { list.addAll(inventory.getFluidStacks().subList(FLUID_SLOTS, FLUID_SLOTS * 2)); }
    @Override protected void tickTransfer() {
        var bridge = bridge();
        if (bridge != null) {
            // Any linked hatch can return the whole machine's output; each transfer removes exactly what was accepted.
            OutputTransfer.toOrigin(bridge);
            bridge.refresh();
        }
    }
}
