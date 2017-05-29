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

import com.elytradev.teckle.common.tile.inv.ItemStream;
import com.elytradev.teckle.common.tile.sortingmachine.TileSortingMachine;
import com.elytradev.teckle.common.tile.sortingmachine.modes.pullmode.PullMode;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;

/**
 * Full match sort mode without a slider, will accept only if all items in a given compartment are available.
 */
public class SortModeFullMatch extends SortMode {
    public SortModeFullMatch() {
        super(2, "sortmode.fullmatch", SortModeType.COMPARTMENT);
    }

    @Override
    public boolean pulse(TileSortingMachine sortingMachine, PullMode mode) {
        // Pull mode not relevant for this task, first find what compartment can be satisfied.

        for (IItemHandler itemHandler : sortingMachine.getCompartmentHandlers()) {
            if (ItemStream.createItemStream(itemHandler).anyMatch(itemStack -> !itemStack.isEmpty())) {

            }
        }

        return false;
    }

    /**
     * Check if the traveller can enter the machine.
     *
     * @param sortingMachine the sorting machine.
     * @param traveller
     * @return
     */
    @Override
    public boolean canAcceptTraveller(TileSortingMachine sortingMachine, WorldNetworkTraveller traveller) {
        // This mode cant accept incoming travellers due to a lack of ability for travellers to store multiple stacks of items.
        // TODO: Confirm functionality makes sense once default routing is fully implemented.
        return false;
    }

    /**
     * Sort a traveller going through the network and change it if needed.
     *
     * @param sortingMachine the sorting machine.
     * @param traveller      the traveller entering the machine.
     * @return the modified traveller.
     */
    @Override
    public WorldNetworkTraveller processExistingTraveller(TileSortingMachine sortingMachine, WorldNetworkTraveller traveller) {
        // This should never happen.

        return traveller;
    }

    /**
     * Get the position of the selector, if no selector is used in this mode return -1.
     *
     * @param sortingMachine the sorting machine.
     * @return the selector position, a value between -1 and 7 (inclusive)
     */
    @Override
    public int selectorPosition(TileSortingMachine sortingMachine) {
        return 0;
    }

    @Override
    public void onTick(TileSortingMachine sortingMachine) {

    }

    @Override
    public NBTBase serializeNBT() {
        return new NBTTagCompound();
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {

    }
}
