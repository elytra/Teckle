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
    public BlockPos start;
    public WorldNetworkPath path;
    public Action action;

    public TravellerDataMessage(NetworkContext ctx) {
        super(ctx);
    }

    public TravellerDataMessage(Action action, WorldNetworkTraveller traveller, BlockPos start) {
        super(TeckleNetworking.NETWORK);
        this.action = action;
        this.data = traveller.data;
        this.start = start;
        this.path = traveller.activePath;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (action.equals(Action.REGISTER)) {
            ClientTravellerManager.travellers.put(data, new DumbNetworkTraveller(data, path, new WorldNetworkNode(null, start)));
        } else if (action.equals(Action.UNREGISTER)) {
            ClientTravellerManager.travellers.remove(data);
        }
    }

    public enum Action {
        REGISTER,
        UNREGISTER
    }
}
