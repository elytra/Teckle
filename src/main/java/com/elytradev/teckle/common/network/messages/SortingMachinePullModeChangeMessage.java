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
import com.elytradev.teckle.common.TeckleLog;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Handles any changes in the pull mode of a sorting machine on the server.
 */
@ReceivedOn(Side.SERVER)
public class SortingMachinePullModeChangeMessage extends Message {

    @MarshalledAs("i8")
    public int pullModeID;
    public BlockPos sortingMachinePos;

    public SortingMachinePullModeChangeMessage(NetworkContext ctx) {
        super(ctx);
    }

    public SortingMachinePullModeChangeMessage(int pullModeID, BlockPos sortingMachinePos) {
        super(TeckleNetworking.NETWORK);
        this.pullModeID = pullModeID;
        this.sortingMachinePos = sortingMachinePos;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileSortingMachine sortingMachine = (TileSortingMachine) sender.world.getTileEntity(sortingMachinePos);
            if (!sortingMachine.isUsableByPlayer(sender))
                return;

            try {
                sortingMachine.setPullMode(PullMode.PULL_MODES.get(pullModeID).newInstance());
            } catch (Exception e) {
                TeckleLog.error("Failed to instantiate pull mode from packet.");
            }
            sortingMachine.markDirty();
        }
    }
}
