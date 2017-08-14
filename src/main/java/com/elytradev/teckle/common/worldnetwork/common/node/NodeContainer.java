package com.elytradev.teckle.common.worldnetwork.common.node;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Contains information about a node on a given side of a position.
 */
public class NodeContainer {

    @Nonnull
    private BlockPos pos = new BlockPos(0, -1, 0);
    @Nullable
    private EnumFacing facing;
    private IWorldNetwork network;
    private WorldNetworkNode node;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeContainer that = (NodeContainer) o;
        return Objects.equals(getPos(), that.getPos()) &&
                getFacing() == that.getFacing();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPos(), getFacing());
    }

    @Nonnull
    public BlockPos getPos() {
        return pos;
    }

    public void setPos(@Nonnull BlockPos pos) {
        this.pos = pos;
    }

    @Nullable
    public EnumFacing getFacing() {
        return facing;
    }

    public void setFacing(@Nullable EnumFacing facing) {
        this.facing = facing;
    }

    public IWorldNetwork getNetwork() {
        return network;
    }

    public void setNetwork(IWorldNetwork network) {
        this.network = network;
    }

    public WorldNetworkNode getNode() {
        return node;
    }

    public void setNode(WorldNetworkNode node) {
        this.node = node;
    }

    public boolean isLoaded() {
        return getNode().isLoaded();
    }

    public WorldNetworkTile getNetworkTile() {
        return getNode().getNetworkTile();
    }

    /**
     * Determines if the container is valid, checks if the position is a negative y.
     *
     * @return true if the container is valid, or false if the data stores is not valid.
     */
    public boolean isValid() {
        return pos.getY() >= 0;
    }

    @Override
    public String toString() {
        return "NodeContainer{" +
                "pos=" + pos +
                ", facing=" + facing +
                ", network=" + network +
                ", node=" + node +
                '}';
    }
}
