package com.elytradev.teckle.common.worldnetwork;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/**
 * Created by darkevilmac on 4/4/2017.
 */
public class WorldNetworkDBData extends WorldSavedData {

    protected static final String NAME = "tecklenetworks";
    public WorldNetworkDatabase db;

    public WorldNetworkDBData() {
        super(NAME);
    }

    public WorldNetworkDBData(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        db.loadDatabase(nbt);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        return db.saveDatabase(compound);
    }

    public static WorldNetworkDBData getDBData(World world){
        WorldNetworkDBData data = (WorldNetworkDBData) world.getPerWorldStorage().getOrLoadData(WorldNetworkDBData.class, "tecklenetworks");
        if (data == null) {
            data = new WorldNetworkDBData();
            data.db = WorldNetworkDatabase.getNetworkDB(world.provider.getDimension());
            world.getPerWorldStorage().setData("tecklenetworks", data);
            data.markDirty();
        }

        return data;
    }

}
