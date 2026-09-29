package dev.mehdi.modernenergistics.client;

import aztech.modern_industrialization.client.machines.GuiComponentsClient;
import aztech.modern_industrialization.client.machines.gui.*;

import dev.mehdi.modernenergistics.ModernEnergistics;
import dev.mehdi.modernenergistics.gui.HatchStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Unit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;


@Mod(value = ModernEnergistics.ID, dist = Dist.CLIENT)
public class BridgeClient {
    public BridgeClient(IEventBus bus) {
        bus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() ->
                GuiComponentsClient.register(HatchStatus.TYPE, StatusClient::new)));
    }
    private static final class StatusClient extends GuiComponentClient<Unit, Unit> {
        StatusClient(Unit params, Unit data) { super(params, data); }
        @Override public ClientComponentRenderer createRenderer(MachineScreen screen) {
            return new ClientComponentRenderer() {
                @Override public void renderBackground(GuiGraphics g, int x, int y) {
                    var font = Minecraft.getInstance().font;
                    // MI's default background texture is only 256 px high; draw our taller menu explicitly.
                    g.fill(x, y, x + 176, y + 310, 0xff373f49);
                    g.fill(x + 2, y + 2, x + 174, y + 308, 0xffc6c6c6);
                    // The player grid is part of MI's background image, which the fill above covers.
                    // Machine slots are redrawn by MI later; restore player slots at their menu positions.
                    for (var slot : screen.getMenu().slots) {
                        if (slot.container instanceof net.minecraft.world.entity.player.Inventory)
                            g.blit(MachineScreen.SLOT_ATLAS, x + slot.x - 1, y + slot.y - 1, 0, 0, 18, 18);
                    }
                    g.drawString(font, "Item inputs", x + 8, y + 19, 0x404040, false);
                    g.drawString(font, "Item outputs", x + 8, y + 69, 0x404040, false);
                    g.drawString(font, "Fluid inputs", x + 8, y + 119, 0x404040, false);
                    g.drawString(font, "Fluid outputs", x + 8, y + 153, 0x404040, false);
                    g.drawString(font, "Craft", x + 106, y + 195, 0x404040, false);
                }
            };
        }
    }
}

