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

package com.elytradev.teckle.common.handlers;

import com.elytradev.teckle.common.tile.base.IElementProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class TeckleGuiHandler implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te;
        if (ID == ElementType.ELEMENT_PROVIDER.caseNumber) {
            te = world.getTileEntity(pos);
            if (te instanceof IElementProvider) {
                return ((IElementProvider) te).getServerElement(player);
            }
        }

        return null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te;
        if (ID == ElementType.ELEMENT_PROVIDER.caseNumber) {
            te = world.getTileEntity(pos);
            if (te instanceof IElementProvider) {
                return ((IElementProvider) te).getClientElement(player);
            }
        }

        return null;
    }

    public enum ElementType {
        ELEMENT_PROVIDER(0);

        public final int caseNumber;

        ElementType(int caseNumber) {
            this.caseNumber = caseNumber;
        }
    }

}
