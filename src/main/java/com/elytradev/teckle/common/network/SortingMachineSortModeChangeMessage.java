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

package com.elytradev.teckle.common.network;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortMode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

@ReceivedOn(Side.SERVER)
public class SortingMachineSortModeChangeMessage extends Message {

    @MarshalledAs("i8")
    public int sortModeID;
    public BlockPos sortingMachinePos;

    public SortingMachineSortModeChangeMessage(NetworkContext ctx) {
        super(ctx);
    }

    public SortingMachineSortModeChangeMessage(int sortModeID, BlockPos sortingMachinePos) {
        super(TeckleNetworking.NETWORK);
        this.sortModeID = sortModeID;
        this.sortingMachinePos = sortingMachinePos;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileSortingMachine sortingMachine = (TileSortingMachine) sender.world.getTileEntity(sortingMachinePos);

            if (!sortingMachine.isUsableByPlayer(sender))
                return;

            try {
                sortingMachine.setSortMode(SortMode.SORT_MODES.get(sortModeID).newInstance());
            } catch (Exception e) {
                TeckleMod.LOG.error("Failed to instantiate sort mode from packet.");
            }
            sortingMachine.markDirty();
        }
    }
}
