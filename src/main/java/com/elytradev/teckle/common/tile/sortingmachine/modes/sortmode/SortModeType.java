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

package com.elytradev.teckle.common.tile.sortingmachine.modes.sortmode;

import com.google.common.collect.Lists;

import javax.vecmath.Point2i;
import java.util.ArrayList;

public enum SortModeType {
    COMPARTMENT(0, 176, 26, "sortmodetype.compartment"),
    SLOT(1, 192, 26, "sortmodetype.slot");

    public static SortModeType[] TYPES;

    static {
        TYPES = new SortModeType[SortModeType.values().length];
        for (int i = 0; i < SortModeType.values().length; i++) {
            TYPES[SortModeType.values()[i].id] = SortModeType.values()[i];
        }
    }

    public final int id;
    private final ArrayList<Class<? extends SortMode>> subModes = Lists.newArrayList();
    private int x, y, minID, maxID;
    private String unlocalizedName;

    SortModeType(int i, int x, int y, String unlocalizedName) {
        this.id = i;
        this.x = x;
        this.y = y;
        this.unlocalizedName = unlocalizedName;

        this.minID = Integer.MAX_VALUE;
        this.maxID = Integer.MIN_VALUE;
    }

    public ArrayList<Class<? extends SortMode>> getSubModes() {
        return subModes;
    }

    public boolean add(SortMode sortMode) {
        if (subModes.contains(sortMode.getClass()))
            return false;

        int id = sortMode.getID();

        minID = minID > id ? id : minID;
        maxID = maxID < id ? id : maxID;

        return subModes.add(sortMode.getClass());
    }


    public Point2i textureOffset() {
        return new Point2i(x, y);
    }

    public int minID() {
        return minID;
    }

    public int maxID() {
        return maxID;
    }

    public String getTranslationKey() {
        return unlocalizedName;
    }

    public Class<? extends SortMode> getDefaultMode() {
        return subModes.get(0);
    }
}
