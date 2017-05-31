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

package com.elytradev.teckle.common.network.messages;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Created by darkevilmac on 5/22/17.
 */
@ReceivedOn(Side.SERVER)
public class SortingMachineColourChangeMessage extends Message {

    public BlockPos pos;
    @MarshalledAs("i8")
    public int colourIndex;
    @MarshalledAs("i8")
    public int colour;

    public SortingMachineColourChangeMessage(NetworkContext ctx) {
        super(ctx);
    }

    public SortingMachineColourChangeMessage(BlockPos pos, int colourIndex, EnumDyeColor colour) {
        super(TeckleNetworking.NETWORK);
        this.pos = pos;
        this.colourIndex = colourIndex;
        this.colour = colour == null ? -1 : colour.getMetadata();
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileSortingMachine sortingMachine = (TileSortingMachine) sender.world.getTileEntity(pos);
            if (!sortingMachine.isUsableByPlayer(sender))
                return;
            sortingMachine.colours[colourIndex] = this.colour == -1 ? null : EnumDyeColor.byMetadata(this.colour);
        }
    }
}
