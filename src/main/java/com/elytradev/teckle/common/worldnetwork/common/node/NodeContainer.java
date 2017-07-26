package com.elytradev.teckle.common.worldnetwork.common.node;

import com.elytradev.teckle.api.IWorldNetwork;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

/**
 * Contains information about a node on a given side of a position.
 */
public class NodeContainer {

    private BlockPos pos;
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

    public BlockPos getPos() {
        return pos;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public EnumFacing getFacing() {
        return facing;
    }

    public void setFacing(EnumFacing facing) {
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
