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

package com.elytradev.teckle.common.tile.sortingmachine;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.impl.NetworkTileTransporter;
import com.elytradev.teckle.client.gui.GuiSortingMachine;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockFilter;
import com.elytradev.teckle.common.container.ContainerSortingMachine;
import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.sortingmachine.modes.PullMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.PullModeSingleStep;
import com.elytradev.teckle.common.tile.sortingmachine.modes.SortMode;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;


public class TileSortingMachine extends TileNetworkMember implements ITickable, IElementProvider {

    public AdvancedItemStackHandler filterRows = new AdvancedItemStackHandler(8 * 6);
    public EnumDyeColor[] colours = new EnumDyeColor[8];

    public PullMode pullMode = new PullModeSingleStep();
    public SortMode sortMode;

    private NetworkTileTransporter networkTile = new NetworkTileTransporter() {
        @Override
        public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
            return new WorldNetworkEntryPoint(network, pos, getFacing());
        }

        @Override
        public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
            return side.equals(getFacing());
        }

        @Override
        public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
            if (traveller.getEntryPoint().position.equals(TileSortingMachine.this.pos))
                return true;

            if (from.equals(getFacing().getOpposite())) {
                // Allows use of filters for filtering items already in tubes. Not really a good reason to do this but it was possible in RP2 so it's possible in Teckle.
                return sortMode.canAcceptTraveller(traveller);
            }
            return false;
        }

        @Override
        public boolean canConnectTo(EnumFacing side) {
            return side.equals(getFacing()) || side.getOpposite().equals(getFacing());
        }

        @Override
        public EnumFacing getFacing() {
            if (world != null) {
                IBlockState thisState = world.getBlockState(pos);
                if (thisState.getBlock().equals(TeckleObjects.blockFilter)) {
                    return thisState.getValue(BlockFilter.FACING);
                }
            }

            return EnumFacing.DOWN;
        }

        @Override
        public void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side) {

        }
    };

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

        pullMode.onTick(this);
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
        try {
            pullMode = PullMode.PULL_MODES.get(compound.getInteger("pullModeID")).newInstance();
            pullMode.deserializeNBT(compound.getCompoundTag("pullMode"));
        } catch (Exception e) {
            TeckleMod.LOG.error("Failed to read sorting machine pull mode from nbt.", e);
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
        compound.setTag("pullMode", pullMode.serializeNBT());
        compound.setInteger("pullModeID", pullMode.getID());

        return super.writeToNBT(compound);
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == null) return null;
        if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY)
            return (T) networkTile;
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == null) return false;
        if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
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