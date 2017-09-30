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

package com.elytradev.teckle.api.capabilities;

import com.elytradev.teckle.api.capabilities.impl.WorldNetworkAssistantHolder;
import com.elytradev.teckle.common.TeckleMod;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CapabilityWorldNetworkAssistantHolder {
    private static final ResourceLocation CAPABILITY_PROVIDER_NAME = new ResourceLocation(TeckleMod.MOD_ID, "NETWORK_ASSISTANT_HOLDER");
    @CapabilityInject(IWorldNetworkAssistantHolder.class)
    public static Capability<IWorldNetworkAssistantHolder> NETWORK_ASSISTANT_HOLDER_CAPABILITY = null;

    public static void register() {
        CapabilityManager.INSTANCE.register(IWorldNetworkAssistantHolder.class, new Capability.IStorage<IWorldNetworkAssistantHolder>() {
                    @Override
                    public NBTBase writeNBT(Capability<IWorldNetworkAssistantHolder> capability, IWorldNetworkAssistantHolder instance, EnumFacing side) {
                        return new NBTTagCompound();
                    }

                    @Override
                    public void readNBT(Capability<IWorldNetworkAssistantHolder> capability, IWorldNetworkAssistantHolder instance, EnumFacing side, NBTBase base) {
                    }
                },
                WorldNetworkAssistantHolder::new);
        MinecraftForge.EVENT_BUS.register(CapabilityWorldNetworkAssistantHolder.class);
    }

    @SuppressWarnings({"deprecation", "unused"}) // forge docs specify using this instead...
    @SubscribeEvent
    public static void attachWorldEvent(AttachCapabilitiesEvent<World> e) {
        TeckleMod.LOG.info("Attached assistant holder to {}", e.getObject());
        e.addCapability(CAPABILITY_PROVIDER_NAME, new ICapabilityProvider() {
            public IWorldNetworkAssistantHolder assistantHolder;

            @Override
            public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
                return capability != null && capability == NETWORK_ASSISTANT_HOLDER_CAPABILITY;
            }

            @Nullable
            @Override
            public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
                if (capability == null)
                    return null;
                if (capability == NETWORK_ASSISTANT_HOLDER_CAPABILITY) {
                    if (assistantHolder == null) assistantHolder = new WorldNetworkAssistantHolder();
                    return (T) assistantHolder;
                }

                return null;
            }
        });
    }

}
