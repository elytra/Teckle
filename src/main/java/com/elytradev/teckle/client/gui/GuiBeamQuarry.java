package com.elytradev.teckle.client.gui;

import com.elytradev.teckle.client.gui.base.GuiTeckle;
import com.elytradev.teckle.client.gui.base.GuiTeckleButton;
import com.elytradev.teckle.common.container.ContainerBeamQuarry;
import com.elytradev.teckle.common.network.messages.serverbound.ChangeQuarryBoundsMessage;
import com.elytradev.teckle.common.network.messages.serverbound.ToggleQuarryMessage;
import com.elytradev.teckle.common.tile.TileBeamQuarry;
import com.google.common.base.Predicate;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class GuiBeamQuarry extends GuiTeckle {

    public final TileBeamQuarry beamQuarry;
    public final EntityPlayer player;

    public GuiTextField leftOffset, forwardOffset, rightOffset;

    public GuiBeamQuarry(TileBeamQuarry tile, EntityPlayer player) {
        super(new ContainerBeamQuarry(tile, player));
        this.beamQuarry = tile;
        this.player = player;

        this.xSize = 176;
        this.ySize = 232;
    }

    @Override
    public void initGui() {
        super.initGui();

        this.leftOffset = new GuiTextField(0, this.fontRenderer, guiLeft + 21, guiTop + 25, 16, 10);
        this.forwardOffset = new GuiTextField(1, this.fontRenderer, guiLeft + 46, guiTop + 25, 16, 10);
        this.rightOffset = new GuiTextField(2, this.fontRenderer, guiLeft + 71, guiTop + 25, 16, 10);
        Predicate<String> fieldValidator = input -> {
            if (input.isEmpty())
                return true;
            try {
                int i = Integer.parseInt(input);
                return i > 0 && i <= 64;
            } catch (NumberFormatException e) {
                return false;
            }
        };
        this.leftOffset.setValidator(fieldValidator);
        this.forwardOffset.setValidator(fieldValidator);
        this.rightOffset.setValidator(fieldValidator);
        this.leftOffset.setMaxStringLength(2);
        this.forwardOffset.setMaxStringLength(2);
        this.rightOffset.setMaxStringLength(2);
        this.leftOffset.setTextColor(-1);
        this.forwardOffset.setTextColor(-1);
        this.rightOffset.setTextColor(-1);
        this.leftOffset.setEnableBackgroundDrawing(false);
        this.forwardOffset.setEnableBackgroundDrawing(false);
        this.rightOffset.setEnableBackgroundDrawing(false);
        this.leftOffset.setEnabled(true);
        this.forwardOffset.setEnabled(true);
        this.rightOffset.setEnabled(true);
        this.leftOffset.setText(String.valueOf(beamQuarry.left));
        this.forwardOffset.setText(String.valueOf(beamQuarry.forward));
        this.rightOffset.setText(String.valueOf(beamQuarry.right));
        if(beamQuarry.isActive()){
            this.leftOffset.setEnabled(false);
            this.forwardOffset.setEnabled(false);
            this.rightOffset.setEnabled(false);
        }
    }

    @Override
    public void registerButtons() {
        super.registerButtons();

        addButton(new GuiQuarryToggle(0, 10, 10, 64, 17, "ACTIVATE"));
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        boolean textBoxTyped = leftOffset.textboxKeyTyped(typedChar, keyCode)
                || forwardOffset.textboxKeyTyped(typedChar, keyCode)
                || rightOffset.textboxKeyTyped(typedChar, keyCode);
        if (textBoxTyped) {
            int left =    getIntValue(leftOffset.getText());
            int forward = getIntValue(forwardOffset.getText());
            int right =   getIntValue(rightOffset.getText());

            new ChangeQuarryBoundsMessage(beamQuarry.getPos(), left, right, forward).sendToServer();
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    private int getIntValue(String text) {
        try {
            int i = Integer.parseInt(text);
            return i;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.leftOffset.mouseClicked(mouseX, mouseY, mouseButton);
        this.forwardOffset.mouseClicked(mouseX, mouseY, mouseButton);
        this.rightOffset.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.leftOffset.drawTextBox();
        this.forwardOffset.drawTextBox();
        this.rightOffset.drawTextBox();

        String junkTypes = I18n.format("beamquarry.junktypes");
        String junk = I18n.format("beamquarry.junk");

        drawStringCentered(junkTypes, 141, 10, 4210752);
        drawStringCentered(junk, 141, 64, 4210752);
    }

    public void drawStringCentered(String text, int x, int y, int colour) {
        int width = this.fontRenderer.getStringWidth(text);
        this.fontRenderer.drawString(text, guiLeft + x - (width / 2), guiTop + y, colour);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        this.leftOffset.updateCursorCounter();
        this.forwardOffset.updateCursorCounter();
        this.rightOffset.updateCursorCounter();
    }

    @Override
    public ResourceLocation getBackgroundTexture() {
        return new ResourceLocation("teckle", "textures/gui/beamquarry.png");
    }

    public class GuiQuarryToggle extends GuiTeckleButton {
        public GuiQuarryToggle(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
            super(buttonId, x, y, widthIn, heightIn, buttonText);
        }

        @Override
        public void performAction(int mouseX, int mouseY, int mouseButton) {
            new ToggleQuarryMessage(beamQuarry.getPos(), true).sendToServer();
        }
    }

}
