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
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;

public abstract class PullMode implements INBTSerializable {

    public static final ArrayList<Class<? extends PullMode>> PULL_MODES;

    public static final Class<? extends PullMode> SINGLE_STEP = PullModeSingleStep.class;
    public static final Class<? extends PullMode> SINGLE_SWEEP = PullModeSingleSweep.class;
    public static final Class<? extends PullMode> AUTOMATIC = PullModeAuto.class;

    static {
        PULL_MODES = new ArrayList<>();

        PULL_MODES.add(0, SINGLE_STEP);
        PULL_MODES.add(1, SINGLE_SWEEP);
        PULL_MODES.add(2, AUTOMATIC);
    }

    public int id;

    public PullMode(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }

    public abstract void onPulse(TileSortingMachine sortingMachine);

    public abstract void onTick(TileSortingMachine sortingMachine);


}
