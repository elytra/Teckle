package com.elytradev.teckle.common.network;

import com.elytradev.concrete.Message;
import com.elytradev.concrete.NetworkContext;
import com.elytradev.concrete.annotation.field.MarshalledAs;
import com.elytradev.concrete.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.tile.TileAlloyFurnace;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Created by darkevilmac on 4/20/2017.
 */
@ReceivedOn(Side.CLIENT)
public class AlloyFurnaceMessage extends Message {

    public BlockPos furnacePos;
    @MarshalledAs("int")
    public int cookTime, fuelBurnTime, currentFuelWorth;

    public AlloyFurnaceMessage(NetworkContext ctx) {
        super(ctx);
    }

    public AlloyFurnaceMessage(TileAlloyFurnace alloyFurnace) {
        super(TeckleNetworking.NETWORK);
        this.furnacePos = alloyFurnace.getPos();
        this.cookTime = alloyFurnace.cookTime;
        this.fuelBurnTime = alloyFurnace.fuelBurnTime;
        this.currentFuelWorth = alloyFurnace.currentFuelWorth;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender.world != null && sender.world.getTileEntity(furnacePos) instanceof TileAlloyFurnace) {
            TileAlloyFurnace alloyFurnace = (TileAlloyFurnace) sender.world.getTileEntity(furnacePos);

            alloyFurnace.fuelBurnTime = this.fuelBurnTime;
            alloyFurnace.cookTime = this.cookTime;
            alloyFurnace.currentFuelWorth = this.currentFuelWorth;
        }
    }

}
