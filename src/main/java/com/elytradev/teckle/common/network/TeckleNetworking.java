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

package com.elytradev.teckle.common.network;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.network.messages.*;

/**
 * Used for registering packets and storing a global network context for Teckle.
 */
public class TeckleNetworking {

    public static final NetworkContext NETWORK = NetworkContext.forChannel(TeckleMod.MOD_ID);

    public static void setupNetwork() {
        registerMessage(DebugReceiverMessage.class);

        registerMessage(TravellerDataMessage.class);
        registerMessage(TravellerMoveMessage.class);
        registerMessage(AlloyFurnaceMessage.class);

        registerMessage(FilterColourChangeMessage.class);
        registerMessage(FabricatorTemplateMessage.class);

        registerMessage(RetrieverColourChangeMessage.class);
        registerMessage(RetrieverSelectorModeChangeMessage.class);
        registerMessage(RetrieverMatchCountMessage.class);
        registerMessage(RetrieverSelectorMessage.class);

        registerMessage(SortingMachineColourChangeMessage.class);
        registerMessage(SortingMachineSortModeChangeMessage.class);
        registerMessage(SortingMachinePullModeChangeMessage.class);
        registerMessage(SortingMachineDefaultRouteChangeMessage.class);
        registerMessage(SortingMachineSelectorMessage.class);
        registerMessage(TileLitMessage.class);
    }

    private static void registerMessage(Class<? extends Message> clazz) {
        NETWORK.register(clazz);
    }

}
