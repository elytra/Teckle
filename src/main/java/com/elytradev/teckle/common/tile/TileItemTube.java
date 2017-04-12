package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.worldnetwork.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.item.ItemNetworkEndpoint;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import java.util.List;

public class TileItemTube extends TileNetworkMember implements ITickable {

    public EnumDyeColor colour = null;

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
    }

    @Override
    public void update() {
    }

    @Override
    public void networkReloaded(WorldNetwork network) {
        List<TileEntity> neighbourNodes = TeckleObjects.blockItemTube.getPotentialNeighbourNodes(network.world, pos, network, true);
        for (TileEntity neighbourNode : neighbourNodes) {
            if (neighbourNode instanceof TileNetworkMember) {
                if (!getNode().network.isNodePresent(neighbourNode.getPos())) {
                    getNode().network.registerNode(((TileNetworkMember) neighbourNode).getNode(getNode().network));
                    ((TileNetworkMember) neighbourNode).setNode(getNode().network.getNodeFromPosition(neighbourNode.getPos()));
                }
            } else {
                if (!getNode().network.isNodePresent(neighbourNode.getPos())) {
                    getNode().network.registerNode(new ItemNetworkEndpoint(getNode().network, neighbourNode.getPos()));
                }
            }
        }

    }

    @Override
    public WorldNetworkNode getNode(WorldNetwork network) {
        return new WorldNetworkNode(network, pos);
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        if (this.colour != null && traveller.data.hasKey("colour")) {
            return this.colour.equals(EnumDyeColor.byMetadata(traveller.data.getInteger("colour")));
        }

        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        colour = compound.getInteger("colour") > 15 ? null : EnumDyeColor.byMetadata(compound.getInteger("colour"));
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setInteger("colour", colour != null ? colour.getMetadata() : 16);
        return super.writeToNBT(compound);
    }

}
