package com.elytradev.teckle.common.tile.retriever;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockRetriever;
import com.elytradev.teckle.common.tile.inv.SlotData;
import com.elytradev.teckle.common.worldnetwork.common.DropActions;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.common.pathing.PathNode;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Objects;
import java.util.function.BiPredicate;

public class NetworkTileRetrieverOutput extends NetworkTileRetrieverBase {

    protected int roundRobinTicker = 0;
    protected int selectorPosition = 0;
    protected boolean useSelector = true;
    protected boolean matchCount = false;

    private EnumFacing cachedFace = EnumFacing.DOWN;
    private EnumDyeColor colour;

    public NetworkTileRetrieverOutput(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    public NetworkTileRetrieverOutput(TileRetriever retriever) {
        super(retriever.getWorld(), retriever.getPos(), retriever.getFacing());

        this.filterData = retriever.filterData;
        this.bufferData = retriever.bufferData;
        this.filterID = retriever.filterID;
        this.bufferID = retriever.bufferID;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = super.serializeNBT();

        tag.setInteger("cachedFace", getOutputFace().getIndex());
        tag.setInteger("roundRobinTicker", roundRobinTicker);
        tag.setInteger("selectorPosition", selectorPosition);
        tag.setBoolean("useSelector", useSelector);
        tag.setBoolean("matchCount", matchCount);
        tag.setInteger("colour", colour == null ? -1 : colour.getMetadata());

        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        super.deserializeNBT(tag);
        this.cachedFace = EnumFacing.VALUES[tag.getInteger("cachedFace")];
        this.roundRobinTicker = tag.getInteger("roundRobinTicker");
        this.selectorPosition = tag.getInteger("selectorPosition");
        this.useSelector = tag.getBoolean("useSelector");
        this.matchCount = tag.getBoolean("matchCount");
        this.colour = tag.getInteger("colour") < 0 ? null : EnumDyeColor.byMetadata(tag.getInteger("colour"));
    }

    @Override
    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
        return new WorldNetworkEntryPoint(network, pos, getCapabilityFace(), getOutputFace());
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        return traveller.getEntryPoint().getPosition().equals(getPos());
    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return Objects.equals(side, getOutputFace());
    }


    @Override
    public EnumFacing getOutputFace() {
        if (getWorld() != null && getWorld().isBlockLoaded(getPos())) {
            IBlockState thisState = getWorld().getBlockState(getPos());
            if (Objects.equals(thisState.getBlock(), TeckleObjects.blockRetriever)) {
                cachedFace = thisState.getValue(BlockRetriever.FACING);
                if (getNode() instanceof WorldNetworkEntryPoint)
                    ((WorldNetworkEntryPoint) getNode()).setOutputFace(cachedFace);
            }
        }

        return cachedFace;
    }

    @Override
    public void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side) {
        if (!traveller.data.hasKey("stack"))
            return; // wtf am I supposed to do with this???

        ItemStack stack = new ItemStack(traveller.data.getCompoundTag("stack"));
        EnumFacing facing = getOutputFace();

        // Try and put it back where we found it.
        if (side.equals(getOutputFace())) {
            if (getWorld().getTileEntity(getPos().offset(facing.getOpposite())) != null) {
                TileEntity pushTo = getWorld().getTileEntity(getPos().offset(facing.getOpposite()));
                if (pushTo.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
                    IItemHandler itemHandler = pushTo.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
                    for (int slot = 0; slot < itemHandler.getSlots() && !stack.isEmpty(); slot++) {
                        stack = itemHandler.insertItem(slot, stack, false);
                    }
                }
            }
        }
        if (!stack.isEmpty()) {
            IItemHandler itemHandler = bufferData.getHandler();
            for (int slot = 0; slot < itemHandler.getSlots() && !stack.isEmpty(); slot++) {
                stack = itemHandler.insertItem(slot, stack, false);
            }
            traveller.data.setTag("stack", stack.serializeNBT());
            if (!stack.isEmpty())
                DropActions.ITEMSTACK.getSecond().dropToWorld(traveller);
        }
    }

    @Override
    public EnumFacing getCapabilityFace() {
        return getOutputFace();
    }

    public void onPulse() {
        if (!getSourceNodes().isEmpty()) {
            ItemStack filterItem = getFilterItem();
            int countToExtract = filterItem.isEmpty() ? 64 : matchCount ? filterItem.getCount() : 64;
            PathNode[] nodes = new PathNode[getSourceNodes().size()];
            getSourceNodes().toArray(nodes);
            SlotData selectedExtractionData = null;
            PathNode usedNode = null;
            for (int i = roundRobinTicker; i < nodes.length; i++) {
                PathNode node = usedNode = nodes[i];
                IItemHandler nodeItemHandler;
                TileEntity tile = getWorld().getTileEntity(node.realNode.getPosition());
                if (tile == null || !tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, node.faceFrom))
                    continue;

                nodeItemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, node.faceFrom);

                for (int slot = 0; slot < nodeItemHandler.getSlots(); slot++) {
                    ItemStack testExtraction = nodeItemHandler.extractItem(slot, countToExtract, true);
                    if (!filterItem.isEmpty()) {
                        if (ItemHandlerHelper.canItemStacksStack(testExtraction, filterItem)) {
                            selectedExtractionData = new SlotData(nodeItemHandler, slot);
                            break;
                        }
                    } else {
                        if (!testExtraction.isEmpty()) {
                            selectedExtractionData = new SlotData(nodeItemHandler, slot);
                            break;
                        }
                    }
                }
                if (!selectedExtractionData.isEmpty()) {
                    if (node.cost <= getSourceNodes().firstEntry().getElement().cost) {
                        if (i + 1 < nodes.length && nodes[i + 1].cost == node.cost) {
                            roundRobinTicker++;
                        } else {
                            roundRobinTicker = 0;
                        }
                    } else {
                        roundRobinTicker = 0;
                    }
                    break;
                }
            }
            ItemStack extractedStack = ItemStack.EMPTY;
            if (!selectedExtractionData.isEmpty()) {
                WorldNetworkEntryPoint entryPoint = new WorldNetworkEntryPoint(usedNode.realNode.getNetwork(), usedNode.realNode.getPosition(), usedNode.faceFrom, getOutputFace());
                BlockPos insertInto = entryPoint.getPosition().offset(usedNode.faceFrom);
                ImmutableMap<String, NBTBase> additionalData = getColour() != null ? ImmutableMap.of("colour", new NBTTagInt(getColour().getMetadata())) : ImmutableMap.of();
                BiPredicate<WorldNetworkNode, EnumFacing> endpointPredicate =
                        (worldNetworkNode, facing) ->
                                (Objects.equals(worldNetworkNode.getPosition(), getPos()) &&
                                        Objects.equals(worldNetworkNode.getCapabilityFace(), getInputTile().getCapabilityFace())
                                        || (Objects.equals(entryPoint.getPosition(), worldNetworkNode.getPosition())));
                extractedStack = selectedExtractionData.extract(countToExtract, false);
                ItemStack insertionResult = getNetworkAssistant(ItemStack.class).insertData(entryPoint, insertInto,
                        extractedStack, additionalData, endpointPredicate,
                        true, false);
                if (!insertionResult.isEmpty())
                    selectedExtractionData.itemHandler.insertItem(selectedExtractionData.slot, insertionResult, false);
            }

            if (selectedExtractionData.isEmpty()) {
                roundRobinTicker = 0;
            } else if (useSelector) {
                incrementSelector();
            }
        }
    }

    public void incrementSelector() {
        int prevSelectorPos = selectorPosition;
        for (int newSelectorPos = selectorPosition + 1; newSelectorPos < 9; newSelectorPos++) {
            ItemStack stackInSlot = filterData.getHandler().getStackInSlot(newSelectorPos);
            if (!stackInSlot.isEmpty()) {
                selectorPosition = newSelectorPos;
                break;
            }
        }

        if (selectorPosition == prevSelectorPos) {
            selectorPosition = 0;
            for (int newSelectorPos = 0; newSelectorPos < 9; newSelectorPos++) {
                ItemStack stackInSlot = filterData.getHandler().getStackInSlot(newSelectorPos);
                if (!stackInSlot.isEmpty()) {
                    selectorPosition = newSelectorPos;
                    break;
                }
            }
        }
    }

    public ItemStack getFilterItem() {
        ItemStack selectedStack = ItemStack.EMPTY;
        if (useSelector) {
            selectedStack = filterData.getHandler().getStackInSlot(selectorPosition);
        }
        return selectedStack;
    }

    @Override
    public EnumDyeColor getColour() {
        return this.colour;
    }

    @Override
    public EnumDyeColor setColour(EnumDyeColor colour) {
        EnumDyeColor oldColour = this.colour;
        this.colour = colour;
        return oldColour;
    }

    public boolean anyStackMode() {
        return !useSelector;
    }

    public boolean useSelector() {
        return useSelector;
    }

}
