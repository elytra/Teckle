package com.elytradev.teckle.common.network;

import com.elytradev.concrete.Message;
import com.elytradev.concrete.NetworkContext;
import com.elytradev.concrete.annotation.field.MarshalledAs;
import com.elytradev.concrete.annotation.type.ReceivedOn;
import com.elytradev.teckle.client.sync.TravellerData;
import com.elytradev.teckle.common.tile.base.TileItemNetworkMember;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkTraveller;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

/**
 * Created by darkevilmac on 3/31/2017.
 */
@ReceivedOn(Side.CLIENT)
public class TravellerDataMessage extends Message {

    public NBTTagCompound data;
    public BlockPos start;
    @MarshalledAs("blockpos-list")
    public List<BlockPos> path;
    public Action action;

    public TravellerDataMessage(NetworkContext ctx) {
        super(ctx);
    }

    public TravellerDataMessage(Action action, WorldNetworkTraveller traveller) {
        super(TeckleNetworking.NETWORK);
        this.action = action;
        this.data = traveller.data;
        this.start = traveller.currentNode.position;
        this.path = traveller.activePath.pathPositions();
    }

    @Override
    protected void handle(EntityPlayer sender) {
        TileEntity tileAtPosition = sender.world.getTileEntity(start);

        if (tileAtPosition == null || !(tileAtPosition instanceof TileItemNetworkMember))
            return;

        if (action.equals(Action.REGISTER)) {
            ((TileItemNetworkMember) tileAtPosition).addTraveller(new TravellerData(data, path));
        } else {
            ((TileItemNetworkMember) tileAtPosition).travellers.remove(data);
        }
    }

    public enum Action {
        REGISTER,
        UNREGISTER
    }
}
