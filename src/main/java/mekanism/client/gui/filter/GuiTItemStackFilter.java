package mekanism.client.gui.filter;

import java.io.IOException;
import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.client.gui.button.GuiButtonDisableableImage;
import mekanism.client.gui.button.GuiColorButton;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.content.transporter.TItemStackFilter;
import mekanism.common.network.PacketEditFilter;
import mekanism.common.network.PacketLogisticalSorterGui;
import mekanism.common.network.PacketLogisticalSorterGui.SorterGuiPacket;
import mekanism.common.network.PacketNewFilter;
import mekanism.common.tile.TileEntityLogisticalSorter;
import mekanism.common.util.LangUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.TransporterUtils;
import mekanism.common.util.text.TextComponentUtil;
import mekanism.common.util.text.Translation;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class GuiTItemStackFilter extends GuiItemStackFilter<TItemStackFilter, TileEntityLogisticalSorter> {

    private TextFieldWidget minField;
    private TextFieldWidget maxField;
    private Button sizeButton;

    public GuiTItemStackFilter(PlayerEntity player, TileEntityLogisticalSorter tile, int index) {
        super(player, tile);
        origFilter = (TItemStackFilter) tileEntity.filters.get(index);
        filter = ((TItemStackFilter) tileEntity.filters.get(index)).clone();
    }

    public GuiTItemStackFilter(PlayerEntity player, TileEntityLogisticalSorter tile) {
        super(player, tile);
        isNew = true;
        filter = new TItemStackFilter();
    }

    @Override
    protected void addButtons() {
        buttons.add(saveButton = new Button(guiLeft + 47, guiTop + 62, 60, 20, LangUtils.localize("gui.save"),
              onPress -> {
                  if (!filter.getItemStack().isEmpty() && !minField.getText().isEmpty() && !maxField.getText().isEmpty()) {
                      int min = Integer.parseInt(minField.getText());
                      int max = Integer.parseInt(maxField.getText());
                      if (max >= min && max <= 64) {
                          filter.min = Integer.parseInt(minField.getText());
                          filter.max = Integer.parseInt(maxField.getText());
                          if (isNew) {
                              Mekanism.packetHandler.sendToServer(new PacketNewFilter(Coord4D.get(tileEntity), filter));
                          } else {
                              Mekanism.packetHandler.sendToServer(new PacketEditFilter(Coord4D.get(tileEntity), false, origFilter, filter));
                          }
                          sendPacketToServer(0);
                      } else if (min > max) {
                          status = EnumColor.DARK_RED + "Max<min";
                          ticker = 20;
                      } else { //if(max > 64 || min > 64)
                          status = EnumColor.DARK_RED + "Max>64";
                          ticker = 20;
                      }
                  } else if (filter.getItemStack().isEmpty()) {
                      status = EnumColor.DARK_RED + "No item";
                      ticker = 20;
                  } else if (minField.getText().isEmpty() || maxField.getText().isEmpty()) {
                      status = EnumColor.DARK_RED + "Max/min";
                      ticker = 20;
                  }
              }));
        buttons.add(deleteButton = new Button(guiLeft + 109, guiTop + 62, 60, 20, LangUtils.localize("gui.delete"),
              onPress -> {
                  Mekanism.packetHandler.sendToServer(new PacketEditFilter(Coord4D.get(tileEntity), true, origFilter, null));
                  sendPacketToServer(0);
              }));
        buttons.add(backButton = new GuiButtonDisableableImage(guiLeft + 5, guiTop + 5, 11, 11, 176, 11, -11, getGuiLocation(),
              onPress -> sendPacketToServer(isNew ? 5 : 0)));
        buttons.add(defaultButton = new GuiButtonDisableableImage(guiLeft + 11, guiTop + 64, 11, 11, 198, 11, -11, getGuiLocation(),
              onPress -> filter.allowDefault = !filter.allowDefault));
        buttons.add(colorButton = new GuiColorButton(guiLeft + 12, guiTop + 44, 16, 16, () -> filter.color,
              onPress -> {
                  if (InputMappings.isKeyDown(minecraft.mainWindow.getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT)) {
                      filter.color = null;
                  } else {
                      filter.color = TransporterUtils.increment(filter.color);
                  }
              }));
        buttons.add(sizeButton = new GuiButtonDisableableImage(guiLeft + 128, guiTop + 44, 11, 11, 187, 11, -11, getGuiLocation(),
              onPress -> filter.sizeMode = !filter.sizeMode));
    }

    @Override
    public void init() {
        super.init();
        minField = new TextFieldWidget(font, guiLeft + 149, guiTop + 19, 20, 11, "");
        minField.setMaxStringLength(2);
        minField.setText("" + filter.min);
        maxField = new TextFieldWidget(font, guiLeft + 149, guiTop + 31, 20, 11, "");
        maxField.setMaxStringLength(2);
        maxField.setText("" + filter.max);
    }

    @Override
    public boolean charTyped(char c, int i) {
        if ((!minField.isFocused() && !maxField.isFocused()) || i == GLFW.GLFW_KEY_ESCAPE) {
            return super.charTyped(c, i);
        }
        if (Character.isDigit(c) || isTextboxKey(c, i)) {
            return minField.charTyped(c, i) || maxField.charTyped(c, i);
        }
        return false;
    }

    @Override
    protected void drawForegroundLayer(int mouseX, int mouseY) {
        font.drawString(LangUtils.localize("gui.itemFilter.min") + ":", 128, 20, 0x404040);
        font.drawString(LangUtils.localize("gui.itemFilter.max") + ":", 128, 32, 0x404040);
        String sizeModeString = LangUtils.transOnOff(filter.sizeMode);
        if (tileEntity.singleItem && filter.sizeMode) {
            sizeModeString = EnumColor.RED + sizeModeString + "!";
        }

        font.drawString(sizeModeString, 141, 46, 0x404040);
        drawTransporterForegroundLayer(mouseX, mouseY, filter.getItemStack());
        if (!filter.getItemStack().isEmpty()) {
            renderScaledText(filter.getItemStack().getDisplayName(), 35, 41, 0x00CD00, 89);
        }

        int xAxis = mouseX - guiLeft;
        int yAxis = mouseY - guiTop;
        if (sizeButton.isMouseOver(mouseX, mouseY)) {
            if (tileEntity.singleItem && filter.sizeMode) {
                displayTooltip(TextComponentUtil.build(Translation.of("mekanism.gui.sizeMode"), " - ", Translation.of("mekanism.gui.sizeModeConflict")), xAxis, yAxis);
            } else {
                displayTooltip(TextComponentUtil.build(Translation.of("mekanism.gui.sizeMode")), xAxis, yAxis);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        minField.tick();
        maxField.tick();
    }

    @Override
    protected void drawItemStackBackground(int xAxis, int yAxis) {
        minField.drawTextBox();
        maxField.drawTextBox();
    }

    @Override
    protected void sendPacketToServer(int guiID) {
        Mekanism.packetHandler.sendToServer(new PacketLogisticalSorterGui(SorterGuiPacket.SERVER, Coord4D.get(tileEntity), guiID, 0, 0));
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        minField.mouseClicked(mouseX, mouseY, button);
        maxField.mouseClicked(mouseX, mouseY, button);
        if (button == 0 && overTypeInput(mouseX - guiLeft, mouseY - guiTop)) {
            ItemStack stack = minecraft.player.inventory.getItemStack();
            if (!stack.isEmpty() && !InputMappings.isKeyDown(minecraft.mainWindow.getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT)) {
                filter.setItemStack(stack.copy());
                filter.getItemStack().setCount(1);
            } else if (stack.isEmpty() && InputMappings.isKeyDown(minecraft.mainWindow.getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT)) {
                filter.setItemStack(ItemStack.EMPTY);
            }
            SoundHandler.playSound(SoundEvents.UI_BUTTON_CLICK);
        } else {
            transporterMouseClicked(button, filter);
        }
    }

    @Override
    protected ResourceLocation getGuiLocation() {
        return MekanismUtils.getResource(ResourceType.GUI, "GuiTItemStackFilter.png");
    }
}