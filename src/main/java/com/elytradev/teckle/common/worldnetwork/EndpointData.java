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

    public EndpointData(BlockPos pos, EnumFacing side) {
        this.pos = pos;
        this.side = side;
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