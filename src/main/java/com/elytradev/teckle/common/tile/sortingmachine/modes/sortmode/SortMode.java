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

import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.tile.sortingmachine.NetworkTileSortingMachineBase;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
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
    private final int id;
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

    public abstract boolean pulse(TileSortingMachine sortingMachine, PullMode mode);

    /**
     * Check if the traveller can enter the machine.
     *
     * @param sortingMachine the sorting machine.
     * @param traveller
     * @param from
     * @return
     */
    public abstract boolean canAcceptTraveller(NetworkTileSortingMachineBase sortingMachine, WorldNetworkTraveller traveller, EnumFacing from);

    /**
     * Get the position of the selector, if no selector is used in this mode return 0.
     *
     * @param sortingMachine the sorting machine.
     * @return the selector position, a value between 0 and 8 (inclusive)
     */
    public abstract int selectorPosition(TileSortingMachine sortingMachine);

    public abstract void onTick(TileSortingMachine sortingMachine);

    public String getUnlocalizedName() {
        return unlocalizedName;
    }

    /**
     * Accept the given traveller if the machine is set to inline mode.
     *
     * @param sortingMachine
     * @param traveller      the traveller to accept.
     * @param from           the side the traveller is to be injected into.
     * @return true if the entire traveller is accepted, false otherwise.
     */
    public abstract ItemStack acceptTraveller(NetworkTileSortingMachineBase sortingMachine, WorldNetworkTraveller traveller, EnumFacing from);
}
