package mekanism.client.gui;

import java.util.Collections;
import mekanism.client.gui.element.GuiHeatInfo;
import mekanism.client.gui.element.GuiRateBar;
import mekanism.client.gui.element.GuiRateBar.IRateInfoHandler;
import mekanism.client.gui.element.tab.GuiBoilerTab;
import mekanism.client.gui.element.tab.GuiBoilerTab.BoilerTab;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.boiler.SynchronizedBoilerData;
import mekanism.common.inventory.container.ContainerFilter;
import mekanism.common.tile.TileEntityBoilerCasing;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.text.TextComponentUtil;
import mekanism.common.util.text.Translation;
import mekanism.common.util.UnitDisplayUtils;
import mekanism.common.util.UnitDisplayUtils.TemperatureUnit;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;

@OnlyIn(Dist.CLIENT)
public class GuiThermoelectricBoiler extends GuiEmbeddedGaugeTile<TileEntityBoilerCasing> {

    public GuiThermoelectricBoiler(PlayerInventory inventory, TileEntityBoilerCasing tile) {
        super(tile, new ContainerFilter(inventory, tile));
        ResourceLocation resource = getGuiLocation();
        addGuiElement(new GuiBoilerTab(this, tileEntity, BoilerTab.STAT, resource));
        addGuiElement(new GuiRateBar(this, new IRateInfoHandler() {
            @Override
            public ITextComponent getTooltip() {
                return TextComponentUtil.build(Translation.of("mekanism.gui.boilRate"), ": " + tileEntity.getLastBoilRate() + " mB/t");
            }

            @Override
            public double getLevel() {
                return tileEntity.structure == null ? 0 : (double) tileEntity.getLastBoilRate() / (double) tileEntity.structure.lastMaxBoil;
            }
        }, resource, 24, 13));
        addGuiElement(new GuiRateBar(this, new IRateInfoHandler() {
            @Override
            public ITextComponent getTooltip() {
                return TextComponentUtil.build(Translation.of("mekanism.gui.maxBoil"), ": " + tileEntity.getLastMaxBoil() + " mB/t");
            }

            @Override
            public double getLevel() {
                return tileEntity.structure == null ? 0 : tileEntity.getLastMaxBoil() * SynchronizedBoilerData.getHeatEnthalpy() /
                                                          (tileEntity.structure.superheatingElements * MekanismConfig.current().general.superheatingHeatTransfer.val());
            }
        }, resource, 144, 13));
        addGuiElement(new GuiHeatInfo(() -> {
            TemperatureUnit unit = TemperatureUnit.values()[MekanismConfig.current().general.tempUnit.val().ordinal()];
            String environment = UnitDisplayUtils.getDisplayShort(tileEntity.getLastEnvironmentLoss() * unit.intervalSize, false, unit);
            return Collections.singletonList(TextComponentUtil.build(Translation.of("mekanism.gui.dissipated"), ": " + environment + "/t"));
        }, this, resource));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        font.drawString(LangUtils.localize("container.inventory"), 8, (ySize - 96) + 4, 0x404040);
        font.drawString(tileEntity.getName(), (xSize / 2) - (font.getStringWidth(tileEntity.getName()) / 2), 5, 0x404040);
        renderScaledText(LangUtils.localize("gui.temp") + ": " +
                         MekanismUtils.getTemperatureDisplay(tileEntity.getTemperature(), TemperatureUnit.AMBIENT), 43, 30, 0x00CD00, 90);
        renderScaledText(LangUtils.localize("gui.boilRate") + ": " + tileEntity.getLastBoilRate() + " mB/t", 43, 39, 0x00CD00, 90);
        renderScaledText(LangUtils.localize("gui.maxBoil") + ": " + tileEntity.getLastMaxBoil() + " mB/t", 43, 48, 0x00CD00, 90);
        int xAxis = mouseX - guiLeft;
        int yAxis = mouseY - guiTop;
        if (xAxis >= 7 && xAxis <= 23 && yAxis >= 14 && yAxis <= 72) {
            FluidStack waterStored = tileEntity.structure != null ? tileEntity.structure.waterStored : null;
            if (waterStored != null) {
                displayTooltip(TextComponentUtil.build(waterStored, ": " + waterStored.amount + "mB"), xAxis, yAxis);
            } else {
                displayTooltip(TextComponentUtil.build(Translation.of("mekanism.gui.empty")), xAxis, yAxis);
            }
        } else if (xAxis >= 153 && xAxis <= 169 && yAxis >= 14 && yAxis <= 72) {
            FluidStack steamStored = tileEntity.structure != null ? tileEntity.structure.steamStored : null;
            if (steamStored != null) {
                displayTooltip(TextComponentUtil.build(steamStored, ": " + steamStored.amount + "mB"), xAxis, yAxis);
            } else {
                displayTooltip(TextComponentUtil.build(Translation.of("mekanism.gui.empty")), xAxis, yAxis);
            }
        }
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(int xAxis, int yAxis) {
        super.drawGuiContainerBackgroundLayer(xAxis, yAxis);
        if (tileEntity.structure != null) {
            if (tileEntity.getScaledWaterLevel(58) > 0) {
                displayGauge(7, 14, tileEntity.getScaledWaterLevel(58), tileEntity.structure.waterStored);
            }
            if (tileEntity.getScaledSteamLevel(58) > 0) {
                displayGauge(153, 14, tileEntity.getScaledSteamLevel(58), tileEntity.structure.steamStored);
            }
        }
    }

    @Override
    protected ResourceLocation getGuiLocation() {
        return MekanismUtils.getResource(ResourceType.GUI, "GuiThermoelectricBoiler.png");
    }

    @Override
    protected ResourceLocation getGaugeResource() {
        return MekanismUtils.getResource(ResourceType.GUI, "GuiIndustrialTurbine.png");
    }
}