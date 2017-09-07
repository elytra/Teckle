package com.elytradev.teckle.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraftforge.fml.client.CustomModLoadingErrorDisplayException;

public class MissingOreExceptionClient extends CustomModLoadingErrorDisplayException {

    private String title;
    private String[] message;

    public MissingOreExceptionClient(String message, Throwable cause) {
        super(message, cause);
        this.title = "Teckle is missing ores!";
        this.message = new String[4];
        for (int i = 0; i < message.split("\n").length; i++) {
            // Split new lines for rendering.
            this.message[i] = message.split("\n")[i];
        }
    }

    @Override
    public void initGui(GuiErrorScreen errorScreen, FontRenderer fontRenderer) {
    }

    @Override
    public void drawScreen(GuiErrorScreen errorScreen, FontRenderer fontRenderer, int mouseRelX, int mouseRelY, float tickTime) {
        errorScreen.drawCenteredString(errorScreen.mc.fontRenderer, this.title, errorScreen.width / 2, 90, 16777215);
        for (int i = 0; i < this.message.length; i++) {
            String m = this.message[i];
            errorScreen.drawCenteredString(errorScreen.mc.fontRenderer, m, errorScreen.width / 2, 110 + (i * 10), 16777215);
        }
    }
}
