package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.worldnetwork.common.pathing.PathNode;
import com.google.common.collect.TreeMultiset;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Comparator;

public class TileRetriever extends TileLitNetworkMember {

    public AdvancedItemStackHandler filterData = new AdvancedItemStackHandler(9);
    public AdvancedItemStackHandler buffer = new AdvancedItemStackHandler(9);
    public EnumDyeColor outputColour = null;

    private TreeMultiset<PathNode> sourceNodes = TreeMultiset.create(Comparator.comparingInt(o -> o.cost));
    private int roundRobinTicker = 0;
    private int selectorPosition = 0;
    private boolean useSelector = true;
    private boolean matchCount = false;

    //private NetworkTileTransporter ejectionTile = new NetworkTileTransporter() {
    //    @Override
    //    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
    //        return new WorldNetworkEntryPoint(network, pos, getOutputFace(), getCapabilityFace());
    //    }
//
    //    @Override
    //    public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
    //        return side.equals(getOutputFace());
    //    }
//
    //    @Override
    //    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
    //        return traveller.getEntryPoint().position.equals(TileRetriever.this.pos);
    //    }
//
    //    @Override
    //    public boolean canConnectTo(EnumFacing side) {
    //        return side.equals(getOutputFace());
    //    }
//
    //    @Override
    //    public EnumFacing getOutputFace() {
    //        if (world != null) {
    //            IBlockState thisState = world.getBlockState(pos);
    //            if (thisState.getBlock().equals(TeckleObjects.blockSortingMachine)) {
    //                return thisState.getValue(BlockSortingMachine.FACING);
    //            }
    //        }
//
    //        return EnumFacing.DOWN;
    //    }
//
    //    @Override
    //    public void acceptReturn(WorldNetworkTraveller traveller, EnumFacing side) {
    //        if (!traveller.data.hasKey("stack"))
    //            return; // wtf am I supposed to do with this???
//
    //        ItemStack stack = new ItemStack(traveller.data.getCompoundTag("stack"));
    //        EnumFacing facing = getOutputFace();
//
    //        // Try and put it back where we found it.
    //        if (side.equals(getOutputFace())) {
    //            if (world.getTileEntity(pos.offset(facing.getOpposite())) != null) {
    //                TileEntity pushTo = world.getTileEntity(pos.offset(facing.getOpposite()));
    //                if (pushTo.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing)) {
    //                    IItemHandler itemHandler = pushTo.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, facing);
    //                    for (int slot = 0; slot < itemHandler.getSlots() && !stack.isEmpty(); slot++) {
    //                        stack = itemHandler.insertItem(slot, stack, false);
    //                    }
    //                }
    //            }
    //        }
    //        if (!stack.isEmpty()) {
    //            IItemHandler itemHandler = buffer;
    //            for (int slot = 0; slot < itemHandler.getSlots() && !stack.isEmpty(); slot++) {
    //                stack = itemHandler.insertItem(slot, stack, false);
    //            }
    //            traveller.data.setTag("stack", stack.serializeNBT());
    //            if (!stack.isEmpty())
    //                DropActions.ITEMSTACK.getSecond().dropToWorld(traveller);
    //        }
    //    }
//
    //    @Override
    //    public EnumFacing getCapabilityFace() {
    //        return getOutputFace();
    //    }
    //};
    //private WorldNetworkTile insertionTile = new NetworkTileTransporter() {
    //    @Override
    //    public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
    //        return side.equals(getCapabilityFace());
    //    }
//
    //    @Override
    //    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
    //        return new SortingMachineEndpoint(network, pos, getCapabilityFace());
    //    }
//
    //    @Override
    //    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
    //        return false;
    //    }
//
    //    @Override
    //    public boolean canConnectTo(EnumFacing side) {
    //        return side.equals(getCapabilityFace());
    //    }
//
    //    @Override
    //    public EnumFacing getCapabilityFace() {
    //        return TileRetriever.this.ejectionTile.getOutputFace().getOpposite();
    //    }
//
    //    @Override
    //    public boolean listenToNetworkChange() {
    //        return true;
    //    }
//
    //    @Override
    //    public void onNodeAdded(WorldNetworkNode addedNode) {
    //        // Only add if it's not already present, and has IO for transfer of items.
    //        if (addedNode.isEndpoint() && sourceNodes.stream().noneMatch(pN -> pN.realNode.equals(addedNode))) {
    //            IWorldNetwork network = this.getNode().getNetwork();
    //            List<PathNode> nodeStack = new ArrayList<>();
    //            List<BlockPos> iteratedPositions = new ArrayList<>();
    //            HashMap<BlockPos, HashMap<EnumFacing, EndpointData>> endpoints = new HashMap<>();
//
    //            nodeStack.add(new PathNode(null, this.getNode(), null));
    //            while (!nodeStack.isEmpty() && endpoints.size() < 6) {
    //                PathNode pathNode = nodeStack.remove(nodeStack.size() - 1);
    //                for (EnumFacing direction : EnumFacing.VALUES) {
    //                    BlockPos neighbourPos = pathNode.realNode.position.add(direction.getDirectionVec());
    //                    if (!network.isNodePresent(neighbourPos) || neighbourPos.equals(this.getNode().position) ||
    //                            iteratedPositions.contains(neighbourPos) ||
    //                            (endpoints.containsKey(neighbourPos) && endpoints.get(neighbourPos).containsKey(direction.getOpposite()))) {
    //                        continue;
    //                    }
//
    //                    WorldNetworkNode neighbourNode = network.getNode(neighbourPos, direction.getOpposite());
    //                    if (isValidSourceNode(neighbourPos, direction)) {
    //                        if (!endpoints.containsKey(neighbourPos)) {
    //                            endpoints.put(neighbourPos, new HashMap<>());
    //                        }
    //                        endpoints.get(neighbourPos).put(direction.getOpposite(),
    //                                new EndpointData(new PathNode(pathNode, neighbourNode, direction.getOpposite()),
    //                                        direction.getOpposite()));
    //                    } else {
    //                        if (neighbourNode.canConnectTo(direction.getOpposite())) {
    //                            nodeStack.add(new PathNode(pathNode, neighbourNode, direction.getOpposite()));
    //                            iteratedPositions.add(neighbourPos);
    //                        }
    //                    }
//
    //                    if (endpoints.size() < 6) {
    //                        break;
    //                    }
    //                }
    //            }
//
    //            for (Map.Entry<BlockPos, HashMap<EnumFacing, EndpointData>> entry : endpoints.entrySet()) {
    //                for (EndpointData endpointData : entry.getValue().values()) {
    //                    if (sourceNodes.stream().noneMatch(pathNode -> pathNode.realNode.position.equals(endpointData.pos)
    //                            && pathNode.from.equals(endpointData.node.from))) {
    //                        sourceNodes.add(endpointData.node);
    //                    }
    //                }
    //            }
    //        }
    //    }
//
    //    @Override
    //    public void onNodeRemoved(WorldNetworkNode removedNode) {
    //        // Remove the node if it's known to us.
    //        sourceNodes.removeIf(pN -> pN.realNode.equals(removedNode) || pN.realNode.position.equals(removedNode.position));
    //    }
//
    //    private boolean isValidSourceNode(BlockPos position, EnumFacing direction) {
    //        direction = direction.getOpposite();
//
    //        TileEntity tileEntity = world.getTileEntity(position);
    //        if (tileEntity != null && tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, direction)) {
    //            return true;
    //        }
//
    //        return false;
    //    }
    //};

    public boolean anyStackMode() {
        return !useSelector;
    }

    public boolean useSelector() {
        return useSelector;
    }

    @Override
    public void update() {
        super.update();
    }

    public ItemStack getFilter() {
        ItemStack selectedStack = ItemStack.EMPTY;
        if (useSelector) {
            selectedStack = filterData.getStackInSlot(selectorPosition);
        }
        return selectedStack;
    }

    public void incrementSelector() {
        int prevSelectorPos = selectorPosition;
        for (int newSelectorPos = selectorPosition + 1; newSelectorPos < 9; newSelectorPos++) {
            ItemStack stackInSlot = filterData.getStackInSlot(newSelectorPos);
            if (!stackInSlot.isEmpty()) {
                selectorPosition = newSelectorPos;
                break;
            }
        }

        if (selectorPosition == prevSelectorPos) {
            selectorPosition = 0;
            for (int newSelectorPos = 0; newSelectorPos < 9; newSelectorPos++) {
                ItemStack stackInSlot = filterData.getStackInSlot(newSelectorPos);
                if (!stackInSlot.isEmpty()) {
                    selectorPosition = newSelectorPos;
                    break;
                }
            }
        }
    }

    public void onPulse() {
        ItemStack filterItem = getFilter();

        if (!sourceNodes.isEmpty()) {
            PathNode[] nodes = (PathNode[]) sourceNodes.toArray();
            ItemStack extracted = ItemStack.EMPTY;
            for (int i = roundRobinTicker; i < nodes.length; i++) {
                PathNode node = nodes[i];
                IItemHandler nodeItemHandler;
                TileEntity tile = node.realNode.getNetworkTile().getTileEntity();
                if (tile == null || !tile.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, node.faceFrom))
                    continue;

                nodeItemHandler = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, node.faceFrom);

                int countToExtract = filterItem.isEmpty() ? 64 : matchCount ? filterItem.getCount() : 64;
                for (int slot = 0; slot < nodeItemHandler.getSlots(); slot++) {
                    ItemStack testExtraction = nodeItemHandler.extractItem(slot, countToExtract, true);
                    if (!filterItem.isEmpty()) {
                        if (ItemHandlerHelper.canItemStacksStack(testExtraction, filterItem)) {
                            extracted = nodeItemHandler.extractItem(slot, countToExtract, false);
                            break;
                        }
                    } else {
                        if (!testExtraction.isEmpty()) {
                            extracted = nodeItemHandler.extractItem(slot, countToExtract, false);
                            break;
                        }
                    }
                }
                if (!extracted.isEmpty()) {
                    if (node.cost <= sourceNodes.firstEntry().getElement().cost) {
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

            if (extracted.isEmpty()) {
                roundRobinTicker = 0;
            } else if (useSelector) {
                incrementSelector();
            }
        }
    }
}
