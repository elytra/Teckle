package com.elytradev.teckle.common.worldnetwork;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

/**
 * Stores an endpoint position as well as the side that endpoint is being injected to.
 */
public class EndpointData {
    public EnumFacing side;
    public BlockPos pos;
    public int cost;
    public WorldNetworkEndpoint node;

    public EndpointData(WorldNetworkEndpoint endpoint, BlockPos pos, EnumFacing side, int cost) {
        this.pos = pos;
        this.side = side;
        this.cost = cost;
        this.node = endpoint;

        System.out.println("Created endpointData " + this.toString());
    }

    @Override
    public String toString() {
        return "EndpointData{" +
                "side=" + side +
                ", pos=" + pos +
                ", cost=" + cost +
                ", node=" + node +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndpointData that = (EndpointData) o;
        return side == that.side &&
                Objects.equals(pos, that.pos);
    }
}