package com.elytradev.teckle.client.render.tile;

import com.elytradev.teckle.client.render.model.ModelMachineOverlay;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockSortingMachine;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

public class TileNetworkMemberRenderer<T extends TileNetworkMember> extends TileEntitySpecialRenderer<T> {

    protected PropertyDirection facingProp;
    protected String lightTexture;
    private ModelMachineOverlay errorLightOverlay;

    public TileNetworkMemberRenderer(PropertyDirection facingProp, String lightTexture) {
        this.facingProp = facingProp;
        this.lightTexture = lightTexture;
    }

    @Override
    public void render(TileNetworkMember te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te.isJammed()) {
            IBlockState blockState = te.getWorld().getBlockState(te.getPos());
            if (blockState.getBlock() != TeckleObjects.blockSortingMachine)
                return;

            EnumFacing pointTo = blockState.getValue(BlockSortingMachine.FACING);
            if (errorLightOverlay == null) {
                errorLightOverlay = new ModelMachineOverlay(this.getClass(), lightTexture, true);
            }

            errorLightOverlay.render(getWorld(), new Vec3d(x, y, z), te.getPos(), blockState, pointTo);
        }
    }
}
