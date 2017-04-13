package com.elytradev.teckle.common.handlers;

import com.elytradev.teckle.client.gui.GuiFilter;
import com.elytradev.teckle.common.container.ContainerFilter;
import com.elytradev.teckle.common.tile.TileFilter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

/**
 * Created by darkevilmac on 4/12/2017.
 */
public class TeckleGuiHandler implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te;
        switch (ID) {
            case 1:
                te = world.getTileEntity(pos);
                if (te instanceof TileFilter) {
                    return new ContainerFilter((TileFilter) te, player);
                }
            default:
                return null;
        }
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity te;
        switch (ID) {
            case 1:
                te = world.getTileEntity(pos);
                if (te instanceof TileFilter) {
                    return new GuiFilter((TileFilter) te, player);
                }
            default:
                return null;
        }
    }

}
