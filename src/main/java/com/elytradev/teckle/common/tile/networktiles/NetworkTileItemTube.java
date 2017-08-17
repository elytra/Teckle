package com.elytradev.teckle.common.tile.networktiles;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.api.capabilities.impl.ItemNetworkAssistant;
import com.elytradev.teckle.common.tile.TileFilter;
import com.elytradev.teckle.common.tile.TileItemTube;
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
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

import static com.elytradev.teckle.common.TeckleMod.MULTIPART_CAPABILITY;

public class NetworkTileItemTube extends WorldNetworkTile {

    public List<EnumFacing> blockedFaces = Lists.newArrayList();
    private EnumDyeColor cachedColour = null;

    public NetworkTileItemTube(World world, BlockPos pos, EnumFacing face) {
        super(world, pos, face);
    }

    public NetworkTileItemTube(TileItemTube tube) {
        super(tube.getWorld(), tube.getPos(), null);
    }

    @Override
    public boolean isValidNetworkMember(IWorldNetwork network, EnumFacing side) {
        return true;
    }

    @Override
    public WorldNetworkNode createNode(IWorldNetwork network, BlockPos pos) {
        return new WorldNetworkNode(network, pos, null);
    }

    @Override
    public boolean canAcceptTraveller(WorldNetworkTraveller traveller, EnumFacing from) {
        if (this.getColour() != null && traveller.data.hasKey("colour")) {
            return Objects.equals(this.getColour(), EnumDyeColor.byMetadata(traveller.data.getInteger("colour")));
        }

        return true;
    }

    public void calculateBlockedFaces() {
        if (MULTIPART_CAPABILITY != null && world.isBlockLoaded(pos)) {
            for (EnumFacing side : EnumFacing.values()) {
                //MCMP loaded, do some checks.
                boolean found = false;
                TileEntity neighbourTile = world.getTileEntity(pos.offset(side));
                IMultipartTile multipartTile = (IMultipartTile) getTileEntity().getCapability(MULTIPART_CAPABILITY, null);
                if (multipartTile != null) {
                    Optional<IMultipartContainer> optionalContainer = MultipartHelper.getContainer(world, pos);

                    if (optionalContainer.isPresent()) {
                        IMultipartContainer container = optionalContainer.get();

                        for (IPartSlot slot : container.getParts().keySet()) {
                            if (slot.getFaceAccess(side.getOpposite()) == EnumSlotAccess.NONE) {
                                blockedFaces.add(side);
                                found = true;
                                break;
                            }
                        }
                    }
                }
                if (found)
                    continue;
                if (neighbourTile != null && neighbourTile.hasCapability(MULTIPART_CAPABILITY, null)) {
                    BlockPos neighbourPos = pos.offset(side);
                    Optional<IMultipartContainer> optionalContainer = MultipartHelper.getContainer(world, neighbourPos);

                    if (optionalContainer.isPresent()) {
                        IMultipartContainer container = optionalContainer.get();

                        for (Map.Entry<IPartSlot, ? extends IPartInfo> iPartSlotEntry : container.getParts().entrySet()) {
                            IPartSlot slot = iPartSlotEntry.getKey();

                            if (slot.getFaceAccess(side) == EnumSlotAccess.NONE) {
                                blockedFaces.add(side);
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            if (world.isBlockLoaded(pos))
                blockedFaces = Lists.newArrayList();
        }
    }

    @Override
    public boolean canConnectTo(EnumFacing side) {
        return !blockedFaces.contains(side);
    }

    @Override
    public void networkReloaded(IWorldNetwork network) {
        List<TileEntity> neighbourNodes = ItemNetworkAssistant.getPotentialNeighbourNodes(this, world, pos, true);
        for (TileEntity neighbourTile : neighbourNodes) {
            BlockPos posDiff = pos.subtract(neighbourTile.getPos());
            EnumFacing capabilityFace = WorldNetworkTraveller.getFacingFromVector(posDiff);

            if (CapabilityWorldNetworkTile.isPositionNetworkTile(world, neighbourTile.getPos(), capabilityFace)) {
                if (!getNode().getNetwork().isNodePresent(neighbourTile.getPos())) {
                    WorldNetworkTile neighbourNetworkTile = CapabilityWorldNetworkTile.getNetworkTileAtPosition(world, neighbourTile.getPos(), capabilityFace);
                    getNode().getNetwork().registerNode(neighbourNetworkTile.createNode(getNode().getNetwork(), neighbourTile.getPos()));
                    neighbourNetworkTile.setNode(getNode().getNetwork().getNode(neighbourTile.getPos(), neighbourNetworkTile.getCapabilityFace()));
                }
            } else {
                if (!getNode().getNetwork().isNodePresent(neighbourTile.getPos()))
                    getNode().getNetwork().registerNode(new ItemNetworkEndpoint(getNode().getNetwork(), neighbourTile.getPos(), (capabilityFace)));
            }
        }
    }

    public EnumDyeColor getColour() {
        if (world != null && world.isBlockLoaded(pos) && world.getTileEntity(pos) instanceof TileFilter) {
            this.cachedColour = ((TileFilter) world.getTileEntity(pos)).colour;
        }

        return this.cachedColour;
    }

    @Override
    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList blockedFaceIndices = new NBTTagList();
        for (EnumFacing blockedFace : blockedFaces) {
            blockedFaceIndices.appendTag(new NBTTagInt(blockedFace.getIndex()));
        }
        tag.setTag("BlockedFaces", blockedFaceIndices);

        if (getColour() != null) {
            tag.setInteger("colour", getColour().getMetadata());
        } else {
            tag.removeTag("colour");
        }
        return tag;
    }

    @Override
    public void deserializeNBT(NBTTagCompound tag) {
        NBTTagList blockedFaceIndices = tag.getTagList("BlockedFaces", Constants.NBT.TAG_INT);
        this.blockedFaces = Lists.newArrayList();
        for (int i = 0; i < blockedFaceIndices.tagCount(); i++) {
            this.blockedFaces.add(EnumFacing.values()[blockedFaceIndices.getIntAt(i)]);
        }

        this.cachedColour = !tag.hasKey("colour") ? null : EnumDyeColor.byMetadata(tag.getInteger("colour"));
    }
}
