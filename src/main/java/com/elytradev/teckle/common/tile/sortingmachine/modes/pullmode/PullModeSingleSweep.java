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

import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortModeFullMatchSelector;
import com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode.SortModePartialMatchSelector;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

public class PullModeSingleSweep extends PullMode {
    public int remainingSweeps = 0;
    public int selectorIncrementsRemaining = 0;
    public int coolDown = 4;

    public PullModeSingleSweep() {
        super(1, 208, 42, "pullmode.singlesweep");
    }

    @Override
    public void onPulse(TileSortingMachine sortingMachine) {
        remainingSweeps++;

        selectorIncrementsRemaining = 8 - sortingMachine.getSortMode().selectorPosition(sortingMachine);
    }

    @Override
    public void onTick(TileSortingMachine sortingMachine) {
        if (isPaused())
            return;

        // Quick check that this pull mode will work with our sort mode as this is only actually useful on a certain set of modes.
        // The UI should prevent this from happening so this is essentially just a sanity check.
        if (sortingMachine.getSortMode().getClass() != SortModeFullMatchSelector.class || sortingMachine.getSortMode().getClass() != SortModePartialMatchSelector.class) {
            try {
                sortingMachine.setPullMode(PullMode.SINGLE_STEP.newInstance());
            } catch (Exception e) {
                TeckleMod.LOG.error("Failed to change pull mode due to incompatibility with sort mode.", e);
            }
            return;
        }

        if (remainingSweeps > 0) {
            if (coolDown <= 0) {
                if (selectorIncrementsRemaining > 0) {
                    if (sortingMachine.getSortMode().pulse(sortingMachine, this)) {
                        selectorIncrementsRemaining = 8 - sortingMachine.getSortMode().selectorPosition(sortingMachine);

                        if (selectorIncrementsRemaining <= 0) {
                            remainingSweeps--;
                            selectorIncrementsRemaining = 8;
                        }
                    }
                }

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
        tagCompound.setInteger("remainingSweeps", remainingSweeps);
        tagCompound.setInteger("selectorIncrementsRemaining", selectorIncrementsRemaining);

        return tagCompound;
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        NBTTagCompound tagCompound = (NBTTagCompound) nbt;
        this.remainingSweeps = tagCompound.getInteger("remainingSweeps");
        this.selectorIncrementsRemaining = tagCompound.getInteger("selectorIncrementsRemaining");
        this.coolDown = tagCompound.getInteger("cooldown");
    }
}
