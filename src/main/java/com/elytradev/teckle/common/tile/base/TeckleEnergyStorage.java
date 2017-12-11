package com.elytradev.teckle.common.tile.base;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.energy.EnergyStorage;

public class TeckleEnergyStorage extends EnergyStorage implements INBTSerializable<NBTTagCompound> {
    public TeckleEnergyStorage(int capacity) {
        super(capacity);
    }

    public TeckleEnergyStorage(int capacity, int maxTransfer) {
        super(capacity, maxTransfer);
    }

    public TeckleEnergyStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public TeckleEnergyStorage(int capacity, int maxReceive, int maxExtract, int energy) {
        super(capacity, maxReceive, maxExtract, energy);
    }

    public void setEnergyStored(int energyStored) {
        this.energy = energyStored;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("energy", getEnergyStored());
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        this.setEnergyStored(tag.getInteger("energy"));
    }
}
