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

package com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode;

import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

public class PullModeSingleStep extends PullMode {
    public int pulses;
    public int coolDown = 4;

    public PullModeSingleStep() {
        super(0, 208, 26, "pullmode.singlestep");
    }

    @Override
    public void onPulse(TileSortingMachine sortingMachine) {
        pulses++;
    }

    @Override
    public void onTick(TileSortingMachine sortingMachine) {
        if (isPaused())
            return;

        if (pulses > 0) {
            if (coolDown <= 0) {
                sortingMachine.getSortMode().pulse(sortingMachine, this);
                pulses--;
                coolDown = 4;
            }

            if (coolDown > 0)
                coolDown--;
        }
    }

    @Override
    public NBTBase serializeNBT() {
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setInteger("pulses", pulses);
        tagCompound.setInteger("cooldown", coolDown);

        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        NBTTagCompound tagCompound = (NBTTagCompound) nbt;
        this.pulses = tagCompound.getInteger("pulses");
        this.coolDown = tagCompound.getInteger("cooldown");
    }
}
