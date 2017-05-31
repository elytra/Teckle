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
 * Created by darkevilmac on 5/31/17.
 */
@ReceivedOn(Side.CLIENT)
public class SortingMachineSelectorMessage extends Message {

    @MarshalledAs("i8")
    public int selectorPos;
    public BlockPos sortingMachinePos;

    public SortingMachineSelectorMessage(NetworkContext ctx) {
        super(ctx);
    }

    public SortingMachineSelectorMessage(int selectorPos, BlockPos sortingMachinePos) {
        super(TeckleNetworking.NETWORK);
        this.selectorPos = selectorPos;
        this.sortingMachinePos = sortingMachinePos;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileSortingMachine sortingMachine = (TileSortingMachine) sender.world.getTileEntity(sortingMachinePos);
            if (!sortingMachine.isUsableByPlayer(sender))
                return;

            sortingMachine.setSelectorPos(selectorPos);
        }
    }
}