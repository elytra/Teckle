package com.elytradev.teckle.compat.mcmp;

import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockItemTube;
import com.elytradev.teckle.common.tile.TileItemTube;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.multipart.IMultipart;
import mcmultipart.api.slot.EnumCenterSlot;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;

import java.util.Collections;
import java.util.List;

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

    @SuppressWarnings("deprecation")
    @Override
    public List<AxisAlignedBB> getOcclusionBoxes(IPartInfo part) {
        AxisAlignedBB bb = getBlock().getBoundingBox(getBlock().getDefaultState(), part.getActualWorld(), part.getPartPos());
        bb = bb.grow((-1f / 16f));
        if (part.getState() instanceof IExtendedBlockState && part.getState().getBlock() == getBlock()) {
            IExtendedBlockState extendedState = (IExtendedBlockState) part.getState();

            if (extendedState.getValue(BlockItemTube.NODE) != null && extendedState.getValue(BlockItemTube.NODE)) {
                bb = bb.grow((1f / 16f));
            }
        }

        return Collections.singletonList(bb);
    }

    @Override
    public void onPartChanged(IPartInfo part, IPartInfo otherPart) {
        if (part.getTile() != null && part.getTile().getTileEntity() instanceof TileItemTube) {
            TileItemTube tube = (TileItemTube) part.getTile().getTileEntity();

            if (tube.getNetworkTile() != null && tube.getNetworkTile().getNode() != null && tube.getNetworkTile().getNode().getNetwork() != null) {
                tube.getNetworkTile().getNode().getNetwork().validateNetwork();
            }
            tube.getNetworkTile().calculateBlockedFaces();
        }
    }

    @Override
    public void onPartAdded(IPartInfo part, IPartInfo otherPart) {
        // Node is killed when block is destroyed, this adds it back.
        TeckleObjects.blockItemTube.getNetworkHelper(part.getActualWorld()).onNodePlaced(part.getPartWorld(), part.getPartPos());
    }

    @Override
    public Block getBlock() {
        return TeckleObjects.blockItemTube;
    }
}
