package com.elytradev.teckle.common.network.messages;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Handles any change of the default route colour for the sorting machine on the server.
 */
@ReceivedOn(Side.SERVER)
public class SortingMachineDefaultRouteChangeMessage extends Message {

    @MarshalledAs("i8")
    public int routeMetadata;
    public BlockPos sortingMachinePos;

    public SortingMachineDefaultRouteChangeMessage(NetworkContext ctx) {
        super(ctx);
    }

    public SortingMachineDefaultRouteChangeMessage(int routeMetadata, BlockPos sortingMachinePos) {
        super(TeckleNetworking.NETWORK);
        this.routeMetadata = routeMetadata;
        this.sortingMachinePos = sortingMachinePos;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileSortingMachine sortingMachine = (TileSortingMachine) sender.world.getTileEntity(sortingMachinePos);
            if (!sortingMachine.isUsableByPlayer(sender))
                return;

            sortingMachine.defaultRoute = TileSortingMachine.DefaultRoute.byMetadata(routeMetadata);
            sortingMachine.markDirty();
        }
    }
}