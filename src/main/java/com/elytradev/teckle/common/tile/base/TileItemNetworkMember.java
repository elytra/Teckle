package com.elytradev.teckle.common.tile.base;

import com.elytradev.teckle.client.sync.TravellerData;
import com.elytradev.teckle.common.worldnetwork.WorldNetwork;
import com.elytradev.teckle.common.worldnetwork.WorldNetworkNode;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by darkevilmac on 3/28/2017.
 */
public class TileItemNetworkMember extends TileEntity implements ITickable {

    @SideOnly(Side.CLIENT)
    public HashMap<NBTTagCompound, TravellerData> travellers = new HashMap<>();
    private WorldNetworkNode node;

    public void addTraveller(TravellerData data) {
        travellers.put(data.tagCompound, data);
    }

    @Override
    public void update() {
        if (world.isRemote) {
            List<TravellerData> move = new ArrayList<>();

            for (TravellerData travellerData : travellers.values()) {
                if (travellerData.travelled >= 1F) {
                    move.add(travellerData);
                }

                travellerData.travelled += (1F / 20F);
            }

            for (TravellerData travellerData : move) {
                travellerData.increment();
                TileEntity nextTile = world.getTileEntity(travellerData.current());
                if (nextTile != null && nextTile instanceof TileItemNetworkMember) {
                    ((TileItemNetworkMember) nextTile).addTraveller(travellerData);
                }
                travellerData.travelled = 0F;
            }
            move.forEach(data -> travellers.remove(data.tagCompound));
        }
    }

    /**
     * Check if this tile can be added to a given network with a neighbour on a specified side.
     *
     * @param network the network to add to
     * @param side    the direction of the neighbour that wants to add
     * @return true if can be added false otherwise.
     */
    public boolean isValidNetworkMember(WorldNetwork network, EnumFacing side) {
        return true;
    }

    public WorldNetworkNode getNode() {
        return node;
    }

    public void setNode(WorldNetworkNode node) {
        this.node = node;
    }

    public WorldNetworkNode getNode(WorldNetwork network) {
        this.node = new WorldNetworkNode(network, pos);

        return node;
    }
}
