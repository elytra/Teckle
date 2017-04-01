package com.elytradev.teckle.client.sync;

import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class TravellerData {

    public float travelled = 0;
    public NBTTagCompound tagCompound;
    public List<BlockPos> path;
    private int index = -1;

    public TravellerData(NBTTagCompound data, List<BlockPos> path) {
        this.tagCompound = data;
        this.path = path;
    }

    public void increment() {
        index++;
    }

    public BlockPos current() {
        return path.get(index);
    }

    public BlockPos last() {
        try {
            return path.get(index - 1);
        } catch (ArrayIndexOutOfBoundsException e){
            return current().add(WorldNetworkTraveller.getFacingFromVector(current().subtract(next())).getDirectionVec());
        }
    }

    public BlockPos next() {
        return path.get(index + 1);
    }

}
