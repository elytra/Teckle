package com.elytradev.teckle.common.network;

import com.elytradev.concrete.Message;
import com.elytradev.concrete.NetworkContext;
import com.elytradev.concrete.annotation.field.MarshalledAs;
import com.elytradev.concrete.annotation.type.ReceivedOn;
import com.elytradev.teckle.client.worldnetwork.ClientTravellerManager;
import com.elytradev.teckle.client.worldnetwork.DummyNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkPath;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Created by darkevilmac on 3/31/2017.
 */
@ReceivedOn(Side.CLIENT)
public class TravellerDataMessage extends Message {

    private static final BlockPos IMPOSSIBLEPOS = new BlockPos(0, -1, 0);

    @MarshalledAs("float")
    public float travelledDistance = 0F;
    public NBTTagCompound data;
    public BlockPos prev, current;
    public WorldNetworkPath path;
    public Action action;

    public TravellerDataMessage(NetworkContext ctx) {
        super(ctx);
    }

    public TravellerDataMessage(Action action, WorldNetworkTraveller traveller) {
        super(TeckleNetworking.NETWORK);
        this.action = action;
        this.data = traveller.data;
        this.current = traveller.currentNode.position;
        this.path = traveller.activePath;
        this.travelledDistance = traveller.travelledDistance;

        this.prev = IMPOSSIBLEPOS;

        // Prevents an NPE with concrete because it hates null :^)
        if (path == null)
            path = WorldNetworkPath.NOT_POSSIBLE;
    }

    public TravellerDataMessage(Action action, WorldNetworkTraveller traveller, BlockPos current, BlockPos previous) {
        super(TeckleNetworking.NETWORK);
        this.action = action;
        this.data = traveller.data;
        this.current = current;
        this.prev = previous;
        this.path = traveller.activePath;
        this.travelledDistance = traveller.travelledDistance;

        // Prevents an NPE with concrete because it hates null :^)
        if (path == null)
            path = WorldNetworkPath.NOT_POSSIBLE;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (action.equals(Action.REGISTER)) {
            DummyNetworkTraveller traveller = new DummyNetworkTraveller(data, path);
            traveller.activePath = path;
            if (!prev.equals(IMPOSSIBLEPOS)) {
                traveller.previousNode = path.next();
                traveller.currentNode = path.next();
                traveller.nextNode = path.next();
            } else {
                traveller.previousNode = new WorldNetworkNode(null, prev);
                traveller.currentNode = new WorldNetworkNode(null, current);
                traveller.nextNode = path.next();
            }

            traveller.travelledDistance = this.travelledDistance;

            ClientTravellerManager.put(data, traveller);
        } else if (action.equals(Action.UNREGISTER)) {
            ClientTravellerManager.removeTraveller(data, true);
        }
    }

    public enum Action {
        REGISTER,
        UNREGISTER,
        REREGISTER
    }
}
