/*
 *    Copyright 2017 Benjamin K (darkevilmac)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.elytradev.teckle.common.tile.sortingmachine.modes;

import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

public class PullModeSingleSweep extends PullMode {
    public int storedTicks = 0;
    public int coolDown = 4;

    public PullModeSingleSweep() {
        super(1);
    }

    @Override
    public void onPulse(TileSortingMachine sortingMachine) {
        storedTicks++;
    }

    @Override
    public void onTick(TileSortingMachine sortingMachine) {
        // Quick check that this pull mode will work with our sort mode as this is only actually useful on a certain mode.
        // The UI should prevent this from happening so this is just a backup.
        if (sortingMachine.sortMode.getClass() != SortModeFullMatchSelector.class || sortingMachine.sortMode.getClass() != SortModePartialMatchSelector.class) {
            try {
                sortingMachine.pullMode = PullMode.SINGLE_STEP.newInstance();
            } catch (Exception e) {
                TeckleMod.LOG.error("Failed to change pull mode due to incompatibility with sort mode.", e);
            }
            return;
        }

        if (storedTicks > 0) {
            if (coolDown <= 0) {
                if (sortingMachine.sortMode.pulse(sortingMachine, this))
                    storedTicks--;

                coolDown = 4;
            }

            if (coolDown > 0)
                coolDown--;
        }
    }

    @Override
    public NBTBase serializeNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setInteger("cooldown", coolDown);
        tagCompound.setInteger("storedTicks", storedTicks);

        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        NBTTagCompound tagCompound = (NBTTagCompound) nbt;
        this.storedTicks = tagCompound.getInteger("storedTicks");
        this.coolDown = tagCompound.getInteger("cooldown");
    }
}
