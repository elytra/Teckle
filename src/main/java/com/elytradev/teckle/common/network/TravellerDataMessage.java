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
        if (!action.equals(Action.REPATH)) {
            this.start = traveller.currentNode.position;
        } else {
            this.start = traveller.previousNode.position;
        }
        this.path = traveller.activePath.pathPositions();
    }

    @Override
    protected void handle(EntityPlayer sender) {
        TileEntity tileAtPosition = sender.world.getTileEntity(start);

        if (tileAtPosition == null || !(tileAtPosition instanceof TileItemNetworkMember))
            return;

        if (action.equals(Action.REGISTER)) {
            ((TileItemNetworkMember) tileAtPosition).addTraveller(new TravellerData(data, path));
        } else if (action.equals(Action.UNREGISTER)) {
            ((TileItemNetworkMember) tileAtPosition).travellers.remove(data);
        } else {
            ((TileItemNetworkMember) tileAtPosition).travellers.get(data).path.addAll(this.path);
        }
    }

    public enum Action {
        REGISTER,
        UNREGISTER,
        REPATH
    }
}