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

import com.google.common.collect.Lists;

import javax.vecmath.Point2i;
import java.util.ArrayList;

public enum SortModeType {
    COMPARTMENT(0, 216, 11),
    SLOT(1, 232, 11);

    public static SortModeType[] TYPES;

    static {
        TYPES = new SortModeType[SortModeType.values().length];
        for (int i = 0; i < SortModeType.values().length; i++) {
            TYPES[SortModeType.values()[i].id] = SortModeType.values()[i];
        }
    }

    public final int id;
    public final ArrayList<Class<? extends SortMode>> subModes = Lists.newArrayList();
    private int x, y;

    SortModeType(int i, int x, int y) {
        this.id = i;
        this.x = x;
        this.y = y;
    }

    public Point2i textureOffset() {
        return new Point2i(x, y);
    }

    public Class<? extends SortMode> getDefaultMode() {
        return subModes.get(0);
    }
}
