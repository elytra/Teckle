package com.elytradev.teckle.common.tile.networktiles;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.tile.TileBeamQuarry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class NetworkTileBeamQuarry extends WorldNetworkTile {
    public EnumFacing cachedFace = EnumFacing.DOWN;
    public UUID bufferID;
    public AdvancedStackHandlerEntry bufferData;
    public UUID junkSupplyID;
    public AdvancedStackHandlerEntry junkSupply;

    public NetworkTileBeamQuarry(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    public NetworkTileBeamQuarry(TileBeamQuarry beamQuarry, EnumFacing face) {
        super(beamQuarry.getWorld(), beamQuarry.getPos(), face);
        this.bufferID = beamQuarry.bufferID;
        this.bufferData = beamQuarry.bufferData;
        this.junkSupplyID = beamQuarry.junkSupplyID;
        this.junkSupply = beamQuarry.junkSupply;
    }

    @Override
    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
        this.setPos(pos);
        return new WorldNetworkEntryPoint(network, pos, getCapabilityFace(), getOutputFace());
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return Objects.equals(traveller.getEntryPoint().getPosition(), this.getPos())
                || Objects.equals(from, getOutputFace().getOpposite());

    }

    @Override
    public void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side) {
        if (!traveller.data.hasKey("stack"))
            return; // wtf am I supposed to do with this???

        ItemStack stack = new ItemStack(traveller.data.getCompoundTag("stack"));
        if (getCapabilityFace() != EnumFacing.UP) {
            stack = bufferData.getHandler().insertItem(stack, false);
        } else {
            stack = junkSupply.getHandler().insertItem(stack, false);
        }

        if (!stack.isEmpty()) {
            ItemStack remaining = stack.copy();
            // Spawn into the world I guess...
            if (!remaining.isEmpty()) {
                WorldNetworkTraveller fakeTravellerToDrop = new WorldNetworkTraveller(new NBTTagCompound());
                remaining.writeToNBT(fakeTravellerToDrop.data.getCompoundTag("stack"));
                DropActions.ITEMSTACK.getSecond().dropToWorld(fakeTravellerToDrop);
            }
        }
    }

    @Nullable
    @Override
    public EnumFacing getOutputFace() {
        return this.getCapabilityFace();
    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return side == getCapabilityFace();
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setUniqueId("buffer", bufferID);
        tag.setUniqueId("junkSupply", junkSupplyID);
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        this.bufferID = tag.getUniqueId("buffer");
        this.bufferData = AdvancedStackHandlerPool.getPool(getWorld().provider.getDimension()).get(bufferID);

        this.junkSupplyID = tag.getUniqueId("junkSupply");
        this.junkSupply = AdvancedStackHandlerPool.getPool(getWorld().provider.getDimension()).get(junkSupplyID);
    }
}
