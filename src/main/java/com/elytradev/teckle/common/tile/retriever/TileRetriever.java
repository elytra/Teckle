package com.elytradev.teckle.common.tile.retriever;

import com.elytradev.teckle.common.tile.TileLitNetworkMember;
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

    private NetworkTileRetrieverOutput outputTile;
    private NetworkTileRetrieverInput inputTile;

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
