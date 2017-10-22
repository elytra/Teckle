/*
 *    Copyright 2017 Benjamin K (darkevilmac)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.elytradev.teckle.client.gui.base;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

public abstract class GuiTeckleButton extends GuiButton {

    private boolean acceptsSecondaryClick = false;

    public GuiTeckleButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    public GuiTeckleButton(int buttonId, int x, int y, String buttonText) {
        super(buttonId, x, y, buttonText);
    }

    public void drawHover(Minecraft mc, int mouseX, int mouseY) {
    }

    public void performAction(int mouseX, int mouseY, int mouseButton) {
    }

    public boolean acceptsSecondaryClick() {
        return acceptsSecondaryClick;
    }

    public GuiTeckleButton enableSecondaryClick() {
        this.acceptsSecondaryClick = true;
        return this;
    }

    public GuiTeckleButton disableSecondaryClick() {
        this.acceptsSecondaryClick = false;
        return this;
    }


    public void checkHovered(int mouseX, int mouseY) {
        this.hovered = mouseX >= this.x
                && mouseY >= this.y
                && mouseX < this.x + this.width
                && mouseY < this.y + this.height;
    }

}
