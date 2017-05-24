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
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.ArrayList;


public abstract class SortMode implements INBTSerializable {

    public static final ArrayList<Class<? extends SortMode>> SORT_MODES;
    public static final Class<? extends SortMode> SLOT_ANY_STACK = SortModeAnyStack.class;
    public static final Class<? extends SortMode> SLOT_FULL_STACK = SortModeFullStack.class;
    public static final Class<? extends SortMode> COMPARTMENT_FULL_MATCH_SELECTOR = SortModeFullMatchSelector.class;
    public static final Class<? extends SortMode> COMPARTMENT_PARTIAL_MATCH_SELECTOR = SortModePartialMatchSelector.class;
    public static final Class<? extends SortMode> COMPARTMENT_FULL_MATCH = SortModeFullMatch.class;

    static {
        SORT_MODES = new ArrayList<>();

        SORT_MODES.add(0, COMPARTMENT_FULL_MATCH_SELECTOR);
        SORT_MODES.add(1, COMPARTMENT_PARTIAL_MATCH_SELECTOR);
        SORT_MODES.add(2, COMPARTMENT_FULL_MATCH);

        SORT_MODES.add(3, SLOT_ANY_STACK);
        SORT_MODES.add(4, SLOT_FULL_STACK);
        try {
            for (Class<? extends SortMode> sortMode : SORT_MODES) {
                sortMode.newInstance();
            }
        } catch (Exception e) {
            TeckleMod.LOG.error("Failed to instantiate sort modes.", e);
        }
    }

    private final String unlocalizedName;
    public int id;
    public SortModeType type;

    public SortMode(int id, String unlocalizedName, SortModeType type) {
        this.id = id;
        this.type = type;
        this.unlocalizedName = unlocalizedName;

        type.add(this);
    }

    public int getID() {
        return id;
    }

    public SortModeType getSortModeType() {
        return type;
    }

    public abstract void pulse(TileSortingMachine sortingMachine, PullMode mode);

    /**
     * Check if the traveller can enter the machine.
     *
     * @param sortingMachine the sorting machine.
     * @param traveller
     * @return
     */
    public abstract boolean canAcceptTraveller(TileSortingMachine sortingMachine, WorldNetworkTraveller traveller);

    /**
     * Sort a traveller going through the network and change it if needed.
     *
     * @param sortingMachine the sorting machine.
     * @param traveller      the traveller entering the machine.
     * @return the modified traveller.
     */
    public abstract WorldNetworkTraveller processExistingTraveller(TileSortingMachine sortingMachine, WorldNetworkTraveller traveller);

    /**
     * Get the position of the selector, if no selector is used in this mode return -1.
     *
     * @param sortingMachine the sorting machine.
     * @return the selector position, a value between -1 and 7 (inclusive)
     */
    public abstract int selectorPosition(TileSortingMachine sortingMachine);

    public String getUnlocalizedName() {
        return unlocalizedName;
    }
}
