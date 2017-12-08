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

package com.elytradev.teckle.common.network.messages.clientbound;

import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.messages.TeckleMessage;
import com.elytradev.teckle.common.tile.TileAlloyFurnace;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Used to set misc fields on the alloy furnace for clients.
 */
@ReceivedOn(Side.CLIENT)
public class AlloyFurnaceMessage extends TeckleMessage {

    public BlockPos furnacePos;
    @MarshalledAs("int")
    public int cookTime, fuelBurnTime, currentFuelWorth;

    public AlloyFurnaceMessage(NetworkContext ctx) {
    }

    public AlloyFurnaceMessage(TileAlloyFurnace alloyFurnace) {
        this.furnacePos = alloyFurnace.getPos();
        this.cookTime = alloyFurnace.cookTime;
        this.fuelBurnTime = alloyFurnace.fuelBurnTime;
        this.currentFuelWorth = alloyFurnace.currentFuelWorth;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender.world != null && sender.world.getTileEntity(furnacePos) instanceof TileAlloyFurnace) {
            TileAlloyFurnace alloyFurnace = (TileAlloyFurnace) sender.world.getTileEntity(furnacePos);

            alloyFurnace.fuelBurnTime = this.fuelBurnTime;
            alloyFurnace.cookTime = this.cookTime;
            alloyFurnace.currentFuelWorth = this.currentFuelWorth;
        }
    }

}
