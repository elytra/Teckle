package com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode;

import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

public class PullModeInline extends PullMode {

    public PullModeInline() {
        super(3, -1, -1, "pullmode.inline");
    }

    @Override
    public void onPulse(TileSortingMachine sortingMachine) {
    }

    @Override
    public void onTick(TileSortingMachine sortingMachine) {
    }

    @Override
    public NBTBase serializeNBT() {
        return new NBTTagCompound();
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
    }
}
