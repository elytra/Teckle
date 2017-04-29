package com.elytradev.teckle.common.network;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.tile.TileFilter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Created by darkevilmac on 4/12/2017.
 */
@ReceivedOn(Side.SERVER)
public class FilterColourChangeMessage extends Message {

    public BlockPos filterPos;
    @MarshalledAs("i8")
    public int colour;

    public FilterColourChangeMessage(NetworkContext ctx) {
        super(ctx);
    }

    public FilterColourChangeMessage(BlockPos filterPos, EnumDyeColor colour) {
        super(TeckleNetworking.NETWORK);
        this.filterPos = filterPos;
        this.colour = colour == null ? -1 : colour.getMetadata();
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileFilter filter = (TileFilter) sender.world.getTileEntity(filterPos);

            filter.colour = this.colour == -1 ? null : EnumDyeColor.byMetadata(this.colour);
        }
    }
}
