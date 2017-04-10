package com.elytradev.teckle.common.network;

import com.elytradev.concrete.Message;
import com.elytradev.concrete.NetworkContext;
import com.elytradev.concrete.annotation.field.MarshalledAs;
import com.elytradev.concrete.annotation.type.ReceivedOn;
import com.elytradev.teckle.client.worldnetwork.ClientTravellerManager;
import com.elytradev.teckle.client.worldnetwork.DummyNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkPath;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Created by darkevilmac on 4/9/2017.
 */
@ReceivedOn(Side.CLIENT)
public class TravellerMoveMessage extends Message {

    @MarshalledAs("int")
    public int pathIndex = -1;
    public NBTTagCompound data;
    public WorldNetworkPath path;


    public TravellerMoveMessage(NetworkContext ctx) {
        super(ctx);
    }

    public TravellerMoveMessage(WorldNetworkTraveller traveller) {
        super(TeckleNetworking.NETWORK);
        this.path = traveller.activePath;
        this.pathIndex = this.path.getIndex() - 1;
        this.data = traveller.data;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        float travelledDistance = ClientTravellerManager.get(data).travelledDistance;
        path.setIndex(pathIndex);
        DummyNetworkTraveller traveller = new DummyNetworkTraveller(data, path);
        traveller.travelledDistance = travelledDistance;
        if (pathIndex < 0) {

        } else {
            traveller.previousNode = path.prev();
            traveller.currentNode = path.next();
            traveller.nextNode = path.next();
        }
        ClientTravellerManager.removeTraveller(data, true);
        ClientTravellerManager.put(data, traveller);
    }
}
