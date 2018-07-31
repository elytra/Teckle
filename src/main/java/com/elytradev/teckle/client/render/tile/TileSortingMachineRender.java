package com.elytradev.teckle.client.render.tile;

import com.elytradev.teckle.client.render.model.ModelMachineOverlay;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;


public class TileSortingMachineRender extends TileNetworkMemberRenderer<TileSortingMachine> {

    private ModelMachineOverlay lightOverlay;

    public TileSortingMachineRender() {
        super(BlockSortingMachine.FACING, "teckle:blocks/sortingmachineerror");
    }

    @Override
    public void render(TileSortingMachine te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        super.render(te, x, y, z, partialTicks, destroyStage, alpha);
        IBlockState blockState = te.getWorld().getBlockState(te.getPos());
        if (blockState.getBlock() != TeckleObjects.blockSortingMachine || !te.isLit() && !te.isJammed())
            return;

        EnumFacing pointTo = blockState.getValue(BlockSortingMachine.FACING);
        if (lightOverlay == null) {
            lightOverlay = new ModelMachineOverlay(this.getClass(), "teckle:blocks/sortingmachineside_on", true);
        }
        lightOverlay.render(getWorld(), new Vec3d(x, y, z), te.getPos(), blockState, pointTo);
    }

}