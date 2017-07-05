package com.elytradev.teckle.common.network.messages;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.network.TeckleNetworking;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Created by darkevilmac on 13/06/17.
 */
@ReceivedOn(Side.CLIENT)
public class SortingMachineLitMessage extends Message {

    public boolean isLit;
    public BlockPos sortingMachinePos;

    public SortingMachineLitMessage(NetworkContext ctx) {
        super(ctx);
    }

    public SortingMachineLitMessage(TileSortingMachine sortingMachine) {
        super(TeckleNetworking.NETWORK);
        this.isLit = sortingMachine.isLit;
        this.sortingMachinePos = sortingMachine.getPos();
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileSortingMachine sortingMachine = (TileSortingMachine) sender.world.getTileEntity(sortingMachinePos);
            sortingMachine.isLit = isLit;
        }
    }
}
