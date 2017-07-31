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
import com.elytradev.teckle.api.capabilities.IWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.impl.ItemNetworkAssistant;
import com.elytradev.teckle.api.capabilities.impl.NetworkTileTransporter;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkTraveller;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkNode;
import com.elytradev.teckle.common.worldnetwork.item.ItemNetworkEndpoint;
import com.google.common.collect.Lists;
import mcmultipart.api.container.IMultipartContainer;
import mcmultipart.api.container.IPartInfo;
import mcmultipart.api.multipart.IMultipartTile;
import mcmultipart.api.multipart.MultipartHelper;
import mcmultipart.api.slot.EnumSlotAccess;
import mcmultipart.api.slot.IPartSlot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiPredicate;

import static com.elytradev.teckle.common.TeckleMod.MULTIPART_CAPABILITY;

public class TileItemTube extends TileNetworkMember {

    private EnumDyeColor colour = null;
    private NetworkTileTransporter networkTile = new NetworkTileTransporter(WorldNetworkNode.class) {
        @Override
        public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
            return true;
        }

        @Override
        public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
            if (TileItemTube.this.colour != null && traveller.data.hasKey("colour")) {
                return Objects.equals(TileItemTube.this.colour, EnumDyeColor.byMetadata(traveller.data.getInteger("colour")));
            }

            return true;
        }

        @Override
        public boolean canConnectTo(EnumFacing side) {
            if (MULTIPART_CAPABILITY != null) {
                //MCMP loaded, do some checks.
                TileEntity neighbourTile = world.getTileEntity(pos.offset(side));
                IMultipartTile multipartTile = (IMultipartTile) getCapability(MULTIPART_CAPABILITY, null);
                if (multipartTile != null) {
                    Optional<IMultipartContainer> optionalContainer = MultipartHelper.getContainer(world, pos);

                    if (optionalContainer.isPresent()) {
                        IMultipartContainer container = optionalContainer.get();

                        for (IPartSlot slot : container.getParts().keySet()) {
                            if (slot.getFaceAccess(side.getOpposite()) == EnumSlotAccess.NONE)
                                return false;
                        }
                    }
                }
                if (neighbourTile != null && neighbourTile.hasCapability(MULTIPART_CAPABILITY, null)) {
                    BlockPos neighbourPos = pos.offset(side);
                    Optional<IMultipartContainer> optionalContainer = MultipartHelper.getContainer(world, neighbourPos);

                    if (optionalContainer.isPresent()) {
                        IMultipartContainer container = optionalContainer.get();

                        for (Map.Entry<IPartSlot, ? extends IPartInfo> iPartSlotEntry : container.getParts().entrySet()) {
                            IPartSlot slot = iPartSlotEntry.getKey();
                            IPartInfo info = iPartSlotEntry.getValue();
                            if (slot.getRegistryName().getResourceDomain() == "chiselsandbits") {
                            }

                            if (slot.getFaceAccess(side) == EnumSlotAccess.NONE)
                                return false;
                        }
                    }
                }
            }

            return true;
        }

        @Override
        public void networkReloaded(IWorldNetwork network) {
            List<TileEntity> neighbourNodes = ItemNetworkAssistant.getPotentialNeighbourNodes(networkTile, world, pos, true);
            for (TileEntity neighbourTile : neighbourNodes) {
                BlockPos posDiff = pos
                        .subtract(neighbourTile.getPos());
                EnumFacing capabilityFace = EnumFacing.getFacingFromVector(posDiff.getX(), posDiff.getY(), posDiff.getZ());

                if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, neighbourTile.getPos(), capabilityFace)) {
                    if (!getNode().getNetwork().isNodePresent(neighbourTile.getPos())) {
                        IWorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, neighbourTile.getPos(), capabilityFace);
                        getNode().getNetwork().registerNode(neighbourNetworkTile.createNode(getNode().getNetwork(), neighbourTile.getPos()));
                        neighbourNetworkTile.setNode(getNode().getNetwork().getNodeContainersAtPosition(neighbourTile.getPos()));
                    }
                } else {

                    if (!getNode().getNetwork().isNodePresent(neighbourTile.getPos())) {
                        getNode().getNetwork().registerNode(new ItemNetworkEndpoint(getNode().getNetwork(), neighbourTile.getPos(), Lists.newArrayList(capabilityFace)));
                    }
                }
            }
        }

        @Override
        public BiPredicate<WorldNetworkTraveller, EnumFacing> canAcceptTravellerPredicate() {
            return (t, f) -> !t.data.hasKey("colour") || EnumDyeColor.byMetadata(t.data.getInteger("colour")).equals(colour);
        }
    };

    public EnumDyeColor getColour() {
        return colour;
    }

    public void setColour(EnumDyeColor colour) {
        this.colour = colour;
    }

    public NetworkTileTransporter getNetworkTile() {
        return networkTile;
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
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.colour = !compound.hasKey("colour") ? null : EnumDyeColor.byMetadata(compound.getInteger("colour"));

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            UUID networkID = compound.getUniqueId("networkID");
            int dimID = compound.getInteger("databaseID");
            if (networkID == null) {
                getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            } else {
                WorldNetworkDatabase networkDB = WorldNetworkDatabase.getNetworkDB(dimID);
                Optional<Pair<BlockPos, EnumFacing>> any = networkDB.getRemappedNodes().keySet().stream()
                        .filter(pair -> Objects.equals(pair.getLeft(), getPos()) && Objects.equals(pair.getValue(), networkTile.getCapabilityFace())).findAny();
                if (any.isPresent()) {
                    networkID = networkDB.getRemappedNodes().remove(any.get());
                    TeckleMod.LOG.debug("Found a remapped network id for " + pos.toString() + " mapped id to " + networkID);
                }
                IWorldNetwork network = WorldNetworkDatabase.getNetworkDB(dimID).get(networkID);
                WorldNetworkNode node = networkTile.createNode(network, pos);
                network.registerNode(node);
                networkTile.setNode(node);
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (colour != null) {
            compound.setInteger("colour", colour.getMetadata());
        } else {
            compound.removeTag("colour");
        }

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            compound.setInteger("databaseID", getWorld().provider.getDimension());
            if (networkTile.getNode() == null)
                getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            compound.setUniqueId("networkID", networkTile.getNode().getNetwork().getNetworkID());
        }

        return super.writeToNBT(compound);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == null) {
            return null;
        } else if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY) {
            return (T) networkTile;
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
}
