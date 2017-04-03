package com.elytradev.teckle.client.worldnetwork;

import com.elytradev.teckle.common.worldnetwork.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkPath;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Created by darkevilmac on 4/2/2017.
 */
public class DumbNetworkTraveller extends WorldNetworkTraveller {

    public DumbNetworkTraveller(NBTTagCompound data, WorldNetworkPath path, WorldNetworkNode prevNode, WorldNetworkNode currentNode) {
        super(data);
        this.activePath = path;
        this.previousNode = prevNode;
        this.currentNode = currentNode;
        this.nextNode = path.next();

        travelledDistance = 0.5F;
    }

    @Override
    public void update() {
        // dont.
    }
}
