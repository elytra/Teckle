package com.elytradev.teckle.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraftforge.fml.client.CustomModLoadingErrorDisplayException;

public class MissingOreExceptionClient extends CustomModLoadingErrorDisplayException {

    private String title;
    private String message;

    public MissingOreExceptionClient(String message, Throwable cause) {
        super(message, cause);
        this.title = "Teckle is missing ores!";
        this.message = message;
    }

    @Override
    public void initGui(GuiErrorScreen errorScreen, FontRenderer fontRenderer) {}

    @Override
    public void drawScreen(GuiErrorScreen errorScreen, FontRenderer fontRenderer, int mouseRelX, int mouseRelY, float tickTime) {
        errorScreen.drawCenteredString(errorScreen.mc.fontRenderer, this.title, errorScreen.width / 2, 90, 16777215);
        errorScreen.drawCenteredString(errorScreen.mc.fontRenderer, this.message, errorScreen.width / 2, 110, 16777215);
    }
}
