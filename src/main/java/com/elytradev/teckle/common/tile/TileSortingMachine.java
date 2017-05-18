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

package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.client.gui.GuiSortingMachine;
import com.elytradev.teckle.common.container.ContainerSortingMachine;
import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;


public class TileSortingMachine extends TileNetworkMember implements ITickable, IElementProvider {

    public AdvancedItemStackHandler filterRows = new AdvancedItemStackHandler(40);
    public EnumDyeColor[] colours = new EnumDyeColor[8];

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tagCompound = super.getUpdateTag();
        NBTTagList coloursTag = new NBTTagList();
        for (int i = 0; i < colours.length; i++) {
            if (colours[i] != null) {
                coloursTag.appendTag(new NBTTagInt(colours[i].getMetadata()));
            } else {
                coloursTag.appendTag(new NBTTagInt(-1));
            }
        }
        tagCompound.setTag("colours", coloursTag);
        return tagCompound;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);

        NBTTagList coloursTag = pkt.getNbtCompound().getTagList("colours", 3);

        for (int i = 0; i < 8; i++) {
            if (coloursTag.getIntAt(i) > -1) {
                colours[i] = EnumDyeColor.byMetadata(coloursTag.getIntAt(i));
            } else {
                colours[i] = null;
            }
        }
    }


    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        if (oldState.getBlock() == newSate.getBlock()) {
            return false;
        }

        return super.shouldRefresh(world, pos, oldState, newSate);
    }

    @Override
    public void update() {
        if (world.isRemote)
            return;


    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        NBTTagList coloursTag = compound.getTagList("colours", 3);
        for (int i = 0; i < 8; i++) {
            if (coloursTag.getIntAt(i) > -1) {
                colours[i] = EnumDyeColor.byMetadata(coloursTag.getIntAt(i));
            } else {
                colours[i] = null;
            }
        }

        filterRows.deserializeNBT(compound.getCompoundTag("filterRows"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagList coloursTag = new NBTTagList();
        for (int i = 0; i < colours.length; i++) {
            if (colours[i] != null) {
                coloursTag.appendTag(new NBTTagInt(colours[i].getMetadata()));
            } else {
                coloursTag.appendTag(new NBTTagInt(-1));
            }
        }
        compound.setTag("colours", coloursTag);
        compound.setTag("filterRows", filterRows.serializeNBT());

        return super.writeToNBT(compound);
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public Object getServerElement(EntityPlayer player) {
        return new ContainerSortingMachine(this, player);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientElement(EntityPlayer player) {
        return new GuiSortingMachine(this, player);
    }

}