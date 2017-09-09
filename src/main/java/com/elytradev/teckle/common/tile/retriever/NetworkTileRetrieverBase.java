package com.elytradev.teckle.common.tile.retriever;

import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.worldnetwork.common.node.PositionData;
import com.elytradev.teckle.common.worldnetwork.common.pathing.PathNode;
import com.google.common.collect.TreeMultiset;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public abstract class NetworkTileRetrieverBase extends WorldNetworkTile {

    public AdvancedStackHandlerEntry bufferData;
    public AdvancedStackHandlerEntry filterData;
    public UUID bufferID;
    public UUID filterID;

    private NetworkTileRetrieverBase otherTile;

    public NetworkTileRetrieverBase(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    public TreeMultiset<PathNode> getSourceNodes() {
        return getInputTile().sourceNodes;
    }

    public void setOtherTile(NetworkTileRetrieverBase otherTile) {
        this.otherTile = otherTile;
    }

    public void setTriggered() {
        if (getWorld().isBlockLoaded(getPos())) {
            ((TileRetriever) getTileEntity()).setTriggered();
        }
    }

    public NetworkTileRetrieverOutput getOutputTile() {
        if (otherTile == null)
            findOtherTile();

        return (NetworkTileRetrieverOutput) (this instanceof NetworkTileRetrieverOutput ? this : otherTile);
    }

    public NetworkTileRetrieverInput getInputTile() {
        if (otherTile == null)
            findOtherTile();

        return (NetworkTileRetrieverInput) (this instanceof NetworkTileRetrieverInput ? this : otherTile);
    }

    private void findOtherTile() {
        Class<? extends NetworkTileRetrieverBase> searchFor =
                this instanceof NetworkTileRetrieverInput
                        ? NetworkTileRetrieverOutput.class
                        : NetworkTileRetrieverInput.class;
        otherTile = (NetworkTileRetrieverBase) PositionData.getPositionData(getWorld().provider.getDimension(), getPos())
                .allNodeContainers().stream().flatMap(List::stream)
                .filter(nodeContainer -> nodeContainer.getNode() != getNode()
                        && Objects.equals(nodeContainer.getFacing(), getCapabilityFace().getOpposite())
                        && searchFor.isInstance(nodeContainer.getNetworkTile())).findFirst().get().getNetworkTile();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();

        tag.setUniqueId("buffer", bufferID);
        tag.setUniqueId("filter", filterID);
        tag.setInteger("colour", getColour() == null ? -1 : getColour().getMetadata());

        return tag;
    }

    public EnumDyeColor getColour() {
        return getOutputTile().getColour();
    }

    public EnumDyeColor setColour(EnumDyeColor colour) {
        return getOutputTile().setColour(colour);
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        this.bufferID = tag.getUniqueId("buffer");
        this.filterID = tag.getUniqueId("filter");
        this.bufferData = AdvancedStackHandlerPool.getPool(getWorld().provider.getDimension()).get(bufferID);
        this.filterData = AdvancedStackHandlerPool.getPool(getWorld().provider.getDimension()).get(filterID);
    }
}
