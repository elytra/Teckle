package com.elytradev.teckle.client.worldnetwork;

import com.elytradev.teckle.common.worldnetwork.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkPath;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * Created by darkevilmac on 4/2/2017.
 */
public class DumbNetworkTraveller extends WorldNetworkTraveller {

    public DumbNetworkTraveller(NBTTagCompound data, WorldNetworkPath path, WorldNetworkNode start) {
        super(data);
        this.activePath = path;
        this.previousNode = WorldNetworkNode.NONE;
        this.currentNode = start;
        this.nextNode = path.next();
    }

    @Override
    public void update() {
        // dont.
    }
}
