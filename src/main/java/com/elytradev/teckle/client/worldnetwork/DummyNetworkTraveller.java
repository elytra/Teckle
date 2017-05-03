package com.elytradev.teckle.client.worldnetwork;

import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.pathing.WorldNetworkPath;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Created by darkevilmac on 4/2/2017.
 */
public class DummyNetworkTraveller extends WorldNetworkTraveller {

    public DummyNetworkTraveller(NBTTagCompound data, WorldNetworkPath path) {
        super(data);
        this.data = data;
        this.activePath = path;
    }

    @Override
    public void update() {
        // dont.
    }
}
