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
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Simple base for network tiles, just used to handle probe data.
 */
public abstract class TileNetworkMember extends TileEntity {

    protected Object probeCapability;

    private boolean isJammed = false;

    @Nullable
    public <T extends INBTSerializable> IWorldNetworkAssistant<T> getNetworkAssistant(@Nonnull Class<T> type) {
        return world.getCapability(CapabilityWorldNetworkAssistantHolder.NETWORK_ASSISTANT_HOLDER_CAPABILITY, null).getAssistant(type);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == TeckleMod.PROBE_CAPABILITY) {
            if (probeCapability == null) probeCapability = new ProbeCapability();
            return (T) probeCapability;
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == TeckleMod.PROBE_CAPABILITY || super.hasCapability(capability, facing);
    }

    public boolean isJammed() {
        return this.isJammed;
    }

    public void setJammed(boolean jammed) {
        this.isJammed = jammed;
    }

    public void unJam() {
    }

    @Override
    protected void setWorldCreate(World worldIn) {
        super.setWorld(worldIn);
    }

    private final class ProbeCapability implements IProbeDataProvider {
        @Override
        public void provideProbeData(List<IProbeData> data) {
            List<WorldNetworkNode> nodes = Lists.newArrayList();
            for (EnumFacing facing : EnumFacing.VALUES) {
                if (!CapabilityWorldNetworkTile.isPositionNetworkTile(world, pos, facing))
                    continue;
                WorldNetworkTile networkTileAtPosition = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, pos, facing);
                WorldNetworkNode node = networkTileAtPosition.getNode();
                String faceName = networkTileAtPosition.getCapabilityFace() == null ? "All" : networkTileAtPosition.getCapabilityFace().getName();
                if (node == null || nodes.contains(node))
                    continue;

                nodes.add(node);
                if (TeckleMod.INDEV)
                    data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.node.network",
                            faceName,
                            node.getNetwork().getNetworkID().toString().toUpperCase().replaceAll("-", ""),
                            networkTileAtPosition.getNode().getNetwork().getNodes().size())));

                if (!node.getTravellers().isEmpty()) {
                    data.add(new ProbeData(new TextComponentTranslation("tooltip.teckle.traveller.data")));
                }

                for (WorldNetworkTraveller traveller : node.getTravellers()) {
                    float distance = ((float) traveller.activePath.getIndex() / (float) traveller.activePath.pathPositions().size()) * 10F;
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
}
