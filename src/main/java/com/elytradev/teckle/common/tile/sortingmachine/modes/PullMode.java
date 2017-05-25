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

import javax.vecmath.Point2i;
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

    private final int id, x, y;
    private final String unlocalizedName;

    public PullMode(int id, int x, int y, String unlocalizedName) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.unlocalizedName = unlocalizedName;
    }

    public int getID() {
        return id;
    }

    public String getUnlocalizedName() {
        return unlocalizedName;
    }

    public Point2i textureOffset() {
        return new Point2i(x, y);
    }

    public abstract void onPulse(TileSortingMachine sortingMachine);

    public abstract void onTick(TileSortingMachine sortingMachine);


}
