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

package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.tile.networktiles.NetworkTileItemTube;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
import com.elytradev.teckle.common.worldnetwork.common.node.NodeContainer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TileItemTube extends TileNetworkMember {

    private EnumDyeColor colour = null;
    private NetworkTileItemTube networkTile;

    public EnumDyeColor getColour() {
        return colour;
    }

    public void setColour(EnumDyeColor colour) {
        this.colour = colour;
    }

    public NetworkTileItemTube getNetworkTile() {
        return networkTile;
    }

    public void setNetworkTile(NetworkTileItemTube networkTile) {
        this.networkTile = networkTile;
    }

    @Override
    public void validate() {
        if (getNetworkTile() == null) {
            this.setNetworkTile(new NetworkTileItemTube(this));
        }
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 0, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tagCompound = super.getUpdateTag();
        if (colour != null) {
            tagCompound.setInteger("colour", colour.getMetadata());
        } else {
            tagCompound.removeTag("colour");
        }

        return tagCompound;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        this.colour = !tag.hasKey("colour") ? null : EnumDyeColor.byMetadata(tag.getInteger("colour"));
        super.readFromNBT(tag);
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.colour = !tag.hasKey("colour") ? null : EnumDyeColor.byMetadata(tag.getInteger("colour"));

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            UUID networkID = tag.hasKey("networkIDLeast") ? tag.getUniqueId("networkID") : null;
            int dimID = tag.getInteger("databaseID");
            if (networkID == null) {
                getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            } else {
                WorldNetworkDatabase networkDB = WorldNetworkDatabase.getNetworkDB(dimID);
                Optional<Pair<BlockPos, EnumFacing>> any = networkDB.getRemappedNodes().keySet().stream()
                        .filter(pair -> Objects.equals(pair.getLeft(), getPos()) && Objects.equals(pair.getValue(), getNetworkTile().getCapabilityFace())).findAny();
                if (any.isPresent()) {
                    networkID = networkDB.getRemappedNodes().remove(any.get());
                    TeckleMod.LOG.debug("Found a remapped network id for " + pos.toString() + " mapped id to " + networkID);
                }

                IWorldNetwork network = WorldNetworkDatabase.getNetworkDB(dimID).get(networkID);
                for (NodeContainer container : network.getNodeContainersAtPosition(pos)) {
                    if (container.getFacing() == null && container.getNetworkTile() instanceof NetworkTileItemTube) {
                        setNetworkTile((NetworkTileItemTube) container.getNetworkTile());
                        break;
                    }
                }
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        if (colour != null) {
            tag.setInteger("colour", colour.getMetadata());
        } else {
            tag.removeTag("colour");
        }

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            tag.setInteger("databaseID", getWorld().provider.getDimension());
            if (getNetworkTile().getNode() == null)
                getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            tag.setUniqueId("networkID", getNetworkTile().getNode().getNetwork().getNetworkID());
        }
        return super.writeToNBT(tag);
    }

    public void setWorld(World worldIn) {
        super.setWorld(worldIn);
        if (getNetworkTile() != null)
            getNetworkTile().setWorld(worldIn);
    }

    @Override
    protected void setWorldCreate(World worldIn) {
        super.setWorldCreate(worldIn);
        // Mojang's method doesnt do this because they were drunk or something idk.
        setWorld(worldIn);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == null) {
            return null;
        } else if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY) {
            return (T) getNetworkTile();
        }

        return super.getCapability(capability, facing);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if (capability == null) {
            return false;
        } else if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY) {
            return true;
        }

        return super.hasCapability(capability, facing);
    }

    @Override
    public String toString() {
        return "TileItemTube{" +
                "pos=" + pos +
                ", colour=" + colour +
                ", networkTile=" + networkTile +
                '}';
    }
}
