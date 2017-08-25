package com.elytradev.teckle.common.tile.sortingmachine;

import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkAssistantHolder;
import com.elytradev.teckle.api.capabilities.IWorldNetworkAssistant;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.tile.TileLitNetworkMember;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortMode;
import com.elytradev.teckle.common.worldnetwork.common.node.PositionData;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public abstract class NetworkTileSortingMachineBase extends WorldNetworkTile {
    public AdvancedStackHandlerEntry bufferData;
    public AdvancedStackHandlerEntry filterData;
    public UUID bufferID;
    public UUID filterID;

    private TileSortingMachine.DefaultRoute cachedDefaultRoute;
    private EnumDyeColor[] cachedColours = new EnumDyeColor[8];
    private NetworkTileSortingMachineBase otherTile;
    private List<IItemHandler> subHandlers;

    public NetworkTileSortingMachineBase(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    public AdvancedItemStackHandler getBuffer() {
        return bufferData.getHandler();
    }

    public TileSortingMachine.DefaultRoute getDefaultRoute() {
        if (getWorld() != null && getWorld().isBlockLoaded(getPos())) {
            cachedDefaultRoute = getSortingMachine().defaultRoute;
        }

        return cachedDefaultRoute;
    }

    private TileSortingMachine getSortingMachine() {
        if (getWorld().isBlockLoaded(getPos())) {
            return (TileSortingMachine) getWorld().getTileEntity(getPos());
        }
        return null;
    }

    public List<IItemHandler> getCompartmentHandlers() {
        if (subHandlers == null || subHandlers.isEmpty()) {
            subHandlers = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                subHandlers.add(filterData.getHandler().subHandler(i * 6, 6));
            }
        }

        return subHandlers;
    }

    public void setOtherTile(NetworkTileSortingMachineBase otherTile) {
        this.otherTile = otherTile;
    }

    public NetworkTileSortingMachineOutput getOutputTile() {
        if (otherTile == null)
            findOtherTile();

        return (NetworkTileSortingMachineOutput) (this instanceof NetworkTileSortingMachineOutput ? this : otherTile);
    }

    public NetworkTileSortingMachineInput getInputTile() {
        if (otherTile == null)
            findOtherTile();

        return (NetworkTileSortingMachineInput) (this instanceof NetworkTileSortingMachineInput ? this : otherTile);
    }

    private void findOtherTile() {
        Class<? extends NetworkTileSortingMachineBase> searchFor =
                this instanceof NetworkTileSortingMachineInput
                        ? NetworkTileSortingMachineOutput.class
                        : NetworkTileSortingMachineInput.class;
        otherTile = (NetworkTileSortingMachineBase) PositionData.getPositionData(getWorld().provider.getDimension(), getPos())
                .allNodeContainers().stream().flatMap(List::stream)
                .filter(nodeContainer -> nodeContainer.getNode() != getNode()
                        && Objects.equals(nodeContainer.getFacing(), getCapabilityFace().getOpposite())
                        && searchFor.isInstance(nodeContainer.getNetworkTile())).findFirst().get().getNetworkTile();
    }

    public boolean isLoaded() {
        return getWorld().isBlockLoaded(getPos());
    }

    public abstract SortMode getSortMode();

    public abstract PullMode getPullMode();

    public void setTriggered() {
        if (getSortingMachine() != null) {
            ((TileLitNetworkMember) getTileEntity()).setTriggered();
        }
    }

    public EnumDyeColor[] getColours() {
        if (getSortingMachine() != null) {
            cachedColours = getSortingMachine().colours;
        }

        return cachedColours;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList coloursTag = new NBTTagList();
        for (int i = 0; i < getColours().length; i++) {
            if (getColours()[i] != null) {
                coloursTag.appendTag(new NBTTagInt(getColours()[i].getMetadata()));
            } else {
                coloursTag.appendTag(new NBTTagInt(-1));
            }
        }
        tag.setTag("colours", coloursTag);
        tag.setInteger("defaultRoute", getDefaultRoute().getMetadata());
        tag.setUniqueId("buffer", bufferID);
        tag.setUniqueId("filter", filterID);
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        this.cachedDefaultRoute = TileSortingMachine.DefaultRoute.byMetadata(tag.getInteger("defaultRoute"));
        NBTTagList coloursTag = tag.getTagList("colours", 3);
        for (int i = 0; i < 8; i++) {
            if (coloursTag.getIntAt(i) > -1) {
                cachedColours[i] = EnumDyeColor.byMetadata(coloursTag.getIntAt(i));
            } else {
                cachedColours[i] = null;
            }
        }

        this.bufferID = tag.getUniqueId("buffer");
        this.filterID = tag.getUniqueId("filter");
        this.bufferData = AdvancedStackHandlerPool.getPool(getWorld().provider.getDimension()).get(bufferID);
        this.filterData = AdvancedStackHandlerPool.getPool(getWorld().provider.getDimension()).get(filterID);
    }
}
