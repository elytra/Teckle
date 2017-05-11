package com.elytradev.teckle.common.network;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import com.elytradev.teckle.common.tile.TileFabricator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Created by darkevilmac on 5/10/17.
 */
@ReceivedOn(Side.SERVER)
public class FabricatorTemplateMessage extends Message {

    public BlockPos fabricatorPos;
    @MarshalledAs("i8")
    public int templateIndex;
    public ItemStack stack;

    public FabricatorTemplateMessage(NetworkContext ctx) {
        super(ctx);
    }

    public FabricatorTemplateMessage(BlockPos fabricatorPos, ItemStack stack, int templateIndex) {
        super(TeckleNetworking.NETWORK);
        this.fabricatorPos = fabricatorPos;
        this.stack = stack;
        this.templateIndex = templateIndex;
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (sender != null && sender.world != null) {
            TileFabricator fabricator = (TileFabricator) sender.world.getTileEntity(fabricatorPos);

            fabricator.setTemplateSlot(templateIndex, stack);
        }
    }
}