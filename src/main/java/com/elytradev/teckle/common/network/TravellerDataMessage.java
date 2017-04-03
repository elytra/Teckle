package com.elytradev.teckle.common.network;

import com.elytradev.concrete.Message;
import com.elytradev.concrete.NetworkContext;
import com.elytradev.concrete.annotation.type.ReceivedOn;
import com.elytradev.teckle.client.worldnetwork.ClientTravellerManager;
import com.elytradev.teckle.client.worldnetwork.DumbNetworkTraveller;
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

        this.prev = BlockPos.ORIGIN;
    }

    public TravellerDataMessage(Action action, WorldNetworkTraveller traveller, BlockPos current, BlockPos previous) {
        super(TeckleNetworking.NETWORK);
        this.action = action;
        this.data = traveller.data;
        this.current = current;
        this.prev = previous;
        this.path = traveller.activePath;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (action.equals(Action.REGISTER)) {
            DumbNetworkTraveller traveller = new DumbNetworkTraveller(data, path);
            traveller.activePath = path;
            if (!prev.equals(BlockPos.ORIGIN)) {
                traveller.previousNode = path.next();
                traveller.currentNode = path.next();
                traveller.nextNode = path.next();
                // make it look like it came out of the inv.
                traveller.travelledDistance = -0.25F;
            } else {
                traveller.previousNode = new WorldNetworkNode(null, prev);
                traveller.currentNode = new WorldNetworkNode(null, current);
                traveller.nextNode = path.next();
            }

            ClientTravellerManager.put(data, traveller);
        } else if (action.equals(Action.UNREGISTER)) {
            ClientTravellerManager.travellers.remove(data);
        }
    }

    public enum Action {
        REGISTER,
        UNREGISTER
    }
}
