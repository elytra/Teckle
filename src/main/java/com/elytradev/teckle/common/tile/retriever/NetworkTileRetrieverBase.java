package com.elytradev.teckle.common.tile.retriever;

import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public abstract class NetworkTileRetrieverBase extends WorldNetworkTile {

    public AdvancedStackHandlerEntry bufferData;
    public AdvancedStackHandlerEntry filterData;
    public UUID bufferID;
    public UUID filterID;

    private EnumDyeColor cachedColour;
    private NetworkTileRetrieverBase otherTile;

    public NetworkTileRetrieverBase(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }
}
