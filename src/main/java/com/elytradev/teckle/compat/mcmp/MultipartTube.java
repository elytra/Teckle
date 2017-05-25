package com.elytradev.teckle.compat.mcmp;

import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.tile.TileItemTube;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.slot.EnumCenterSlot;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Simple multipart for tubes.
 */
public class MultipartTube implements IMultipart {
    @Override
    public IPartSlot getSlotForPlacement(World world, BlockPos pos, IBlockState state, EnumFacing facing, float hitX, float hitY, float hitZ, EntityLivingBase placer) {
        return EnumCenterSlot.CENTER;
    }

    @Override
    public IPartSlot getSlotFromWorld(IBlockAccess world, BlockPos pos, IBlockState state) {
        return EnumCenterSlot.CENTER;
    }

    @Override
    public void onPartChanged(IPartInfo part, IPartInfo otherPart) {
        if (part.getTile() != null && part.getTile().getTileEntity() instanceof TileItemTube) {
            TileItemTube tube = (TileItemTube) part.getTile().getTileEntity();

            if (tube.getNetworkTile() != null && tube.getNetworkTile().getNode() != null && tube.getNetworkTile().getNode().network != null) {
                tube.getNetworkTile().getNode().network.validateNetwork();
            }
        }
    }

    @Override
    public Block getBlock() {
        return TeckleObjects.blockItemTube;
    }
}
