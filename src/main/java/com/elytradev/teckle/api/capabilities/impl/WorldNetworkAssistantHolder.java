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

package com.elytradev.teckle.api.capabilities.impl;

import com.elytradev.teckle.api.capabilities.IWorldNetworkAssistant;
import com.elytradev.teckle.api.capabilities.IWorldNetworkAssistantHolder;
import com.google.common.collect.Maps;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Default impl of IWorldNetworkAssistantHolder.
 */
public class WorldNetworkAssistantHolder implements IWorldNetworkAssistantHolder {
    private Map<Object, IWorldNetworkAssistant> assistantMap = Maps.newHashMap();

    public WorldNetworkAssistantHolder() {
        // Init assistants.

        assistantMap.put(ItemStack.class, new ItemNetworkAssistant());
    }

    /**
     * Get an assistant for the given type, default types are ItemStack
     *
     * @param type the type of assistant.
     * @return the assistant matching the given type.
     */
    @Nullable
    @Override
    public <T extends INBTSerializable> IWorldNetworkAssistant<T> getAssistant(@Nonnull Class<T> type) {
        if (assistantMap.containsKey(type))
            return assistantMap.get(type);

        return null;
    }

    /**
     * Add an assistant for the given type.
     *
     * @param type      the type to add.
     * @param assistant the assistant for that type.
     * @return the already existing assistant if one is present, or the given assistant if its the only one matching.
     */
    @Nonnull
    @Override
    public <T extends INBTSerializable> IWorldNetworkAssistant<T> addAssistant(@Nonnull Class<T> type, @Nonnull IWorldNetworkAssistant<T> assistant) {
        if (getAssistant(type) == null) {
            assistantMap.put(type, assistant);
            return assistant;
        } else {
            return assistantMap.get(type);
        }
    }

}
