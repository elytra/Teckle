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

package com.elytradev.teckle.common.network.messages.serverbound;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.network.messages.TeckleMessage;
import com.elytradev.teckle.common.tile.TileFabricator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Sent by clients to the server to set the fake slots for the fabricator.
 */
@ReceivedOn(Side.SERVER)
public class FabricatorTemplateMessage extends TeckleMessage {

    public BlockPos fabricatorPos;
    @MarshalledAs("i8")
    public int templateIndex;
    public ItemStack stack;

    public FabricatorTemplateMessage(NetworkContext ctx) {
    }

    public FabricatorTemplateMessage(BlockPos fabricatorPos, ItemStack stack, int templateIndex) {
        this.fabricatorPos = fabricatorPos;
        this.stack = stack;
        this.templateIndex = templateIndex;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileFabricator fabricator = (TileFabricator) sender.world.getTileEntity(fabricatorPos);
            if (!fabricator.isUsableByPlayer(sender))
                return;
            fabricator.setTemplateSlot(templateIndex, stack);
            fabricator.markDirty();
        }
    }
}