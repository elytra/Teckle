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

import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import net.minecraft.nbt.NBTBase;

public class PullModeAuto extends PullMode {
    public PullModeAuto() {
        super(2);
    }

    @Override
    void onPulse(TileSortingMachine sortingMachine) {

    }

    @Override
    void onTick(TileSortingMachine sortingMachine) {

    }

    @Override
    public NBTBase serializeNBT() {
        return null;
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {

    }
}
