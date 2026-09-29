package dev.mehdi.modernenergistics.gui;

import aztech.modern_industrialization.machines.gui.GuiComponentServer;
import dev.mehdi.modernenergistics.ModernEnergistics;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;

/** Static layout; energy and progress use MI's native components. */
public record HatchStatus() implements GuiComponentServer<Unit, Unit> {
    public static final Type<Unit, Unit> TYPE = new Type<>(ModernEnergistics.id("hatch_status"),
            StreamCodec.unit(Unit.INSTANCE), StreamCodec.unit(Unit.INSTANCE));
    @Override public Unit getParams() { return Unit.INSTANCE; }
    @Override public Type<Unit, Unit> getType() { return TYPE; }
    @Override public Unit extractData() { return Unit.INSTANCE; }
}
