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

package com.elytradev.teckle.common.tile.base;

import com.elytradev.probe.api.IProbeData;
import com.elytradev.probe.api.IProbeDataProvider;
import com.elytradev.probe.api.UnitDictionary;
import com.elytradev.probe.api.impl.ProbeData;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkAssistantHolder;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.IWorldNetworkAssistant;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.google.common.collect.ImmutableList;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Simple base for network tiles, just used to handle probe data.
 */
public abstract class TileNetworkMember extends TileEntity {

    protected Object probeCapability;

    @Nullable
    public IWorldNetworkAssistant getNetworkAssistant(@Nonnull Class type) {
        return world.getCapability(CapabilityWorldNetworkAssistantHolder.NETWORK_ASSISTANT_HOLDER_CAPABILITY, null).getAssistant(type);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == null) return null;
        if (capability == TeckleMod.PROBE_CAPABILITY) {
            if (probeCapability == null) probeCapability = new ProbeCapability();
            return (T) probeCapability;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == null) return false;
        if (capability == TeckleMod.PROBE_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    private final class ProbeCapability implements IProbeDataProvider {
        @Override
        public void provideProbeData(List<IProbeData> data) {
            if (!TileNetworkMember.this.hasCapability(CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY, null))
                return;

            WorldNetworkNode node = TileNetworkMember.this.getCapability(CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY, null).getNode();

            if (node == null)
                return;

            if (TeckleMod.INDEV)
                data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.node.network", node.network.getNetworkID().toString().toUpperCase().replaceAll("-", ""))));

            if (!node.getTravellers().isEmpty()) {
                data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.traveller.data")));
            }

            for (WorldNetworkTraveller traveller : node.getTravellers()) {
                float distance = (Float.valueOf(traveller.activePath.getIndex()) / Float.valueOf(traveller.activePath.pathPositions().size())) * 10F;
                distance += traveller.travelledDistance;
                distance -= 0.1F;
                distance = MathHelper.clamp(distance, 0F, 10F);
                if (distance > 0) {
                    ItemStack stack = new ItemStack(traveller.data.getCompoundTag("stack"));
                    data.add(new ProbeData(new TextComponentString(stack.getDisplayName()))
                            .withInventory(ImmutableList.of(stack))
                            .withBar(0, distance * 10, 100, UnitDictionary.PERCENT));
                }
            }
        }
    }
}
