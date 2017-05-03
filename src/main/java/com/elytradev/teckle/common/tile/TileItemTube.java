package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.item.ItemNetworkEndpoint;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;
import java.util.List;

public class TileItemTube extends TileNetworkMember {

    private EnumDyeColor colour = null;

    public EnumDyeColor getColour() {
        return colour;
    }

    public void setColour(EnumDyeColor colour) {
        this.colour = colour;
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tagCompound = super.getUpdateTag();
        if (colour != null) {
            tagCompound.setInteger("colour", colour.getMetadata());
        } else {
            tagCompound.removeTag("colour");
        }

        return tagCompound;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);

        this.colour = !pkt.getNbtCompound().hasKey("colour") ? null : EnumDyeColor.byMetadata(pkt.getNbtCompound().getInteger("colour"));
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
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
    public boolean canConnectTo(EnumFacing side) {
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        this.colour = !compound.hasKey("colour") ? null : EnumDyeColor.byMetadata(compound.getInteger("colour"));
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (colour != null) {
            compound.setInteger("colour", colour.getMetadata());
        } else {
            compound.removeTag("colour");
        }
        return super.writeToNBT(compound);
    }

}
