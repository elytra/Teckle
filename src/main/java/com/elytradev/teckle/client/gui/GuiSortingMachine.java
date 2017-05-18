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

package com.elytradev.teckle.client.gui;

import com.elytradev.teckle.common.container.ContainerSortingMachine;
import com.elytradev.teckle.common.tile.TileSortingMachine;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Created by darkevilmac on 5/17/17.
 */
public class GuiSortingMachine extends GuiContainer {

    public EntityPlayer player;
    public TileSortingMachine sortingMachine;

    public GuiSortingMachine(TileSortingMachine tileSortingMachine, EntityPlayer player) {
        super(new ContainerSortingMachine(tileSortingMachine, player));
        this.player = player;
        this.sortingMachine = tileSortingMachine;
    }

    /**
     * Draws the background layer of this container (behind the items).
     *
     * @param partialTicks
     * @param mouseX
     * @param mouseY
     */
    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

    }
}
