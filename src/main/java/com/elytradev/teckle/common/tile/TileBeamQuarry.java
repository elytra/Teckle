package com.elytradev.teckle.common.tile;

import com.elytradev.teckle.api.IWorldNetwork;
import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkTile;
import com.elytradev.teckle.api.capabilities.IWorldNetworkAssistant;
import com.elytradev.teckle.api.capabilities.WorldNetworkTile;
import com.elytradev.teckle.client.gui.GuiBeamQuarry;
import com.elytradev.teckle.common.TeckleLog;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.block.BlockBeamQuarry;
import com.elytradev.teckle.common.container.ContainerBeamQuarry;
import com.elytradev.teckle.common.network.messages.clientbound.TileUpdateMessage;
import com.elytradev.teckle.common.tile.base.IElementProvider;
import com.elytradev.teckle.common.tile.base.TileNetworkMember;
import com.elytradev.teckle.common.tile.inv.AdvancedItemStackHandler;
import com.elytradev.teckle.common.tile.inv.ItemStream;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerEntry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import com.elytradev.teckle.common.tile.networktiles.NetworkTileBeamQuarry;
import com.elytradev.teckle.common.worldnetwork.common.WorldNetworkDatabase;
import com.elytradev.teckle.common.worldnetwork.common.node.NodeContainer;
import com.elytradev.teckle.common.worldnetwork.common.node.WorldNetworkEntryPoint;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TileBeamQuarry extends TileNetworkMember implements ITickable, IElementProvider {

    private BlockPos min = pos;
    private BlockPos max = pos;
    private BlockPos cursor = pos;
    public int left, right, forward;
    public AdvancedStackHandlerEntry bufferData;
    public AdvancedStackHandlerEntry junkSupply;
    public UUID bufferID, junkSupplyID;
    private EnumFacing facing;

    public NetworkTileBeamQuarry leftNetworkTile, rightNetworkTile, topNetworkTile;

    public AdvancedItemStackHandler junkTypes = new AdvancedItemStackHandler(6).withInsertCheck((integer, itemStack) -> itemStack.getItem() instanceof ItemBlock).withSlotLimit((i) -> 1);

    private ArrayList<MutablePair<BlockPos, Boolean>> stairPositions = Lists.newArrayList();

    @Override
    public void validate() {
        try {
            AdvancedStackHandlerPool pool = AdvancedStackHandlerPool.getPool(world);
            this.bufferData = pool.getOrCreatePoolEntry(bufferID, getPos(), 25);
            this.bufferID = bufferData.getId();
            this.junkSupply = pool.getOrCreatePoolEntry(junkSupplyID, getPos(), 12);
            this.junkSupplyID = junkSupply.getId();

            if (leftNetworkTile == null || rightNetworkTile == null || topNetworkTile == null) {
                this.leftNetworkTile = new NetworkTileBeamQuarry(this, getFacing().getOpposite().rotateYCCW());
                this.rightNetworkTile = new NetworkTileBeamQuarry(this, getFacing().getOpposite().rotateY());
                this.topNetworkTile = new NetworkTileBeamQuarry(this, EnumFacing.UP);
            }
            this.leftNetworkTile.junkSupply = this.junkSupply;
            this.leftNetworkTile.junkSupplyID = this.junkSupplyID;
            this.leftNetworkTile.bufferData = this.bufferData;
            this.leftNetworkTile.bufferID = this.bufferID;
            this.rightNetworkTile.junkSupply = this.junkSupply;
            this.rightNetworkTile.junkSupplyID = this.junkSupplyID;
            this.rightNetworkTile.bufferData = this.bufferData;
            this.rightNetworkTile.bufferID = this.bufferID;
            this.topNetworkTile.junkSupply = this.junkSupply;
            this.topNetworkTile.junkSupplyID = this.junkSupplyID;
            this.topNetworkTile.bufferData = this.bufferData;
            this.topNetworkTile.bufferID = this.bufferID;

            this.tileEntityInvalid = false;
        } catch (Exception e) {
            TeckleLog.error("Failed to validate beam quarry. {}", e);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        this.min = BlockPos.fromLong(tag.getLong("min"));
        this.max = BlockPos.fromLong(tag.getLong("max"));
        this.setCursor(BlockPos.fromLong(tag.getLong("cursor")));
        this.left = tag.getInteger("left");
        this.right = tag.getInteger("right");
        this.forward = tag.getInteger("forward");
        this.facing = tag.getInteger("facing") > 0 ? EnumFacing.values()[tag.getInteger("facing")] : null;

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            this.junkTypes.deserializeNBT(tag.getCompoundTag("junkTypes"));
            this.bufferID = tag.getUniqueId("buffer");
            this.bufferData = AdvancedStackHandlerPool.getPool(world.provider.getDimension()).get(bufferID);
            this.junkSupplyID = tag.getUniqueId("junkSupply");
            this.junkSupply = AdvancedStackHandlerPool.getPool(world.provider.getDimension()).get(junkSupplyID);

            if (loadNetworkTile(tag, "leftTileID", getFacing().getOpposite().rotateYCCW(), NetworkTileBeamQuarry.class))
                if (loadNetworkTile(tag, "rightTileID", getFacing().getOpposite().rotateY(), NetworkTileBeamQuarry.class))
                    loadNetworkTile(tag, "topTileID", EnumFacing.UP, NetworkTileBeamQuarry.class);

            validate();
        }
    }

    protected boolean loadNetworkTile(NBTTagCompound tag, String tileIDKey, EnumFacing tileFace, Class<? extends WorldNetworkTile> tileType) {
        UUID networkID = tag.hasUniqueId(tileIDKey) ? tag.getUniqueId(tileIDKey) : null;
        int dimID = tag.getInteger("databaseID");
        if (networkID == null) {
            getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            return false;
        } else {
            WorldNetworkDatabase networkDB = WorldNetworkDatabase.getNetworkDB(dimID);
            Optional<Pair<BlockPos, EnumFacing>> any = networkDB.getRemappedNodes().keySet().stream()
                    .filter(pair -> Objects.equals(pair.getLeft(), getPos()) && Objects.equals(pair.getValue(), tileFace)).findAny();
            if (any.isPresent()) {
                networkID = networkDB.getRemappedNodes().remove(any.get());
                TeckleLog.debug("Found a remapped network id for " + pos.toString() + " mapped id to " + networkID);
            }

            IWorldNetwork network = WorldNetworkDatabase.getNetworkDB(dimID).get(networkID);
            for (NodeContainer container : network.getNodeContainersAtPosition(pos)) {
                if (Objects.equals(container.getFacing(), tileFace) && container.getNetworkTile() != null && tileType.isInstance(container.getNetworkTile())) {
                    if (tileFace == EnumFacing.UP) {
                        topNetworkTile = (NetworkTileBeamQuarry) container.getNetworkTile();
                    } else if (tileFace == getFacing().getOpposite().rotateY()) {
                        rightNetworkTile = (NetworkTileBeamQuarry) container.getNetworkTile();
                    } else if (tileFace == getFacing().getOpposite().rotateYCCW()) {
                        leftNetworkTile = (NetworkTileBeamQuarry) container.getNetworkTile();
                    }
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setLong("min", this.min.toLong());
        tag.setLong("max", this.max.toLong());
        tag.setLong("cursor", this.getCursor().toLong());
        tag.setInteger("left", left);
        tag.setInteger("right", right);
        tag.setInteger("forward", forward);
        tag.setInteger("facing", getFacing() == null ? -1 : getFacing().getIndex());

        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            if (bufferData == null || junkSupply == null)
                validate();
            tag.setTag("junkTypes", this.junkTypes.serializeNBT());
            tag.setUniqueId("buffer", this.bufferData.getId());
            tag.setUniqueId("junkSupply", this.junkSupply.getId());

            tag.setInteger("databaseID", getWorld().provider.getDimension());

            if (leftNetworkTile.getNode() == null || rightNetworkTile.getNode() == null || topNetworkTile == null)
                getNetworkAssistant(ItemStack.class).onNodePlaced(world, pos);
            tag.setUniqueId("leftTileID", leftNetworkTile.getNode().getNetwork().getNetworkID());
            tag.setUniqueId("rightTileID", rightNetworkTile.getNode().getNetwork().getNetworkID());
            tag.setUniqueId("topTileID", topNetworkTile.getNode().getNetwork().getNetworkID());
        }
        return super.writeToNBT(tag);
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock()
                || newState.getValue(BlockBeamQuarry.FACING) != oldState.getValue(BlockBeamQuarry.FACING);
    }

    @Override
    public void update() {
        //TODO: Power consumption and cooldown.
        if (isActive()) {
            if (world.isRemote)
                return;

            if (getCursor() == pos) {
                setCursor(min);
            }

            // Check the current cursor position for validity.
            IBlockState cursorState = world.getBlockState(getCursor());
            if (!isStateValid(cursorState)) {
                adjustCursor();
                cursorState = world.getBlockState(cursor);
            }
            // Mine the current cursor position.
            if (isStateValid(cursorState)) {
                AxisAlignedBB dropBox = new AxisAlignedBB(getCursor().getX() - 0.5, getCursor().getY() - 0.5, getCursor().getZ() - 0.5,
                        getCursor().getX() + 0.5, getCursor().getY() + 0.5, getCursor().getZ() + 0.5);
                dropBox = dropBox.expand(1.5, 1.5, 1.5);
                world.destroyBlock(getCursor(), true);
                List<EntityItem> entityItems = world.getEntitiesWithinAABB(EntityItem.class, dropBox);
                List<ItemStack> items = entityItems.stream().map(EntityItem::getItem).collect(Collectors.toList());
                entityItems.forEach(Entity::setDead);
                for (ItemStack stack : items) {
                    Stream<ItemStack> junkStream = ItemStream.createItemStream(junkTypes);
                    ItemStack finalItem = stack;
                    if (junkStream.anyMatch(j -> ItemHandlerHelper.canItemStacksStack(finalItem, j))) {
                        stack = junkSupply.getHandler().insertItem(stack, false);
                    }
                    stack = tryPush(stack);
                    stack = bufferData.getHandler().insertItem(stack, false);
                    if (!stack.isEmpty())
                        world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, stack));
                }
            }
        }
        if (world.isRemote) {
            // Show the border for the quarry area.
            for (BlockPos dustPos : BlockPos.getAllInBox(new BlockPos(min.getX(), pos.getY(), min.getZ()),
                    new BlockPos(max.getX(), pos.getY(), max.getZ()))) {
                world.spawnParticle(EnumParticleTypes.REDSTONE, true, dustPos.getX() + 0.5, pos.getY(), dustPos.getZ() + 0.5, 0, 0, 0);
            }
        }
    }

    private void adjustCursor() {
        int startX = Math.abs(cursor.subtract(min).getX());
        int startZ = Math.abs(cursor.subtract(min).getZ());
        BlockPos difference = max.subtract(min);

        for (int y = cursor.getY(); y > 0; y--) {
            boolean negativeX = isNegative(difference.getX());
            for (int x = startX; x <= Math.abs(difference.getX()); x++) {
                boolean negativeZ = isNegative(difference.getZ());
                for (int z = startZ; z <= Math.abs(difference.getZ()); z++) {
                    BlockPos cursorPos = min.add(negativeX ? -x : x, -min.getY() + y, negativeZ ? -z : z);
                    setCursor(cursorPos);
                    if (isStateValid(world.getBlockState(cursor)))
                        return;
                }
                startZ = 0;
            }
            startX = 0;


            if (!ItemStream.createItemStream(junkSupply.getHandler()).allMatch(ItemStack::isEmpty)) {
                int finalY = y;
                stairPositions.stream().filter(s -> s.getLeft().getY() >= finalY && !s.getRight()).forEachOrdered(s -> {
                    Optional<ItemStack> any = ItemStream.createItemStream(junkSupply.getHandler()).filter(i -> !i.isEmpty()).findAny();
                    if (any.isPresent()) {
                        world.setBlockState(s.getLeft(), Block.getBlockFromItem(any.get().getItem()).getStateFromMeta(any.get().getMetadata()));
                        any.get().shrink(1);
                        s.setRight(true);
                    }
                });
            }
        }

        // Recheck to confirm everything is clear, then deactivate the quarry if it is.
        List<BlockPos> stairBlockPositions = stairPositions.stream().map(MutablePair::getLeft).collect(Collectors.toList());
        for (BlockPos p : BlockPos.getAllInBox(min, max)) {
            if (stairBlockPositions.contains(p))
                continue;

            if (isStateValid(world.getBlockState(p))) {
                cursor = p;
                return;
            }
        }
        world.setBlockState(pos, world.getBlockState(pos).withProperty(BlockBeamQuarry.ACTIVE, false));
    }

    private boolean isNegative(int i) {
        return i < 0;
    }

    public ItemStack tryPush(ItemStack stack) {
        IWorldNetworkAssistant<ItemStack> networkAssistant = getNetworkAssistant(ItemStack.class);
        if (!stack.isEmpty())
            stack = networkAssistant.insertData((WorldNetworkEntryPoint) leftNetworkTile.getNode(),
                    pos.offset(getFacing().getOpposite().rotateYCCW()), stack, ImmutableMap.of(), false, false);
        if (!stack.isEmpty())
            stack = networkAssistant.insertData((WorldNetworkEntryPoint) rightNetworkTile.getNode(),
                    pos.offset(getFacing().getOpposite().rotateY()), stack, ImmutableMap.of(), false, false);
        return stack;
    }

    /**
     * Check if the state given is valid for mining or if the cursor needs to move.
     *
     * @param state the state to check.
     * @return true if the state can be mined, false otherwise.
     */
    public boolean isStateValid(IBlockState state) {
        return state.getBlock() != Blocks.AIR && state.getBlock() != Blocks.BEDROCK;
    }

    /**
     * Checks if the quarry is currently active with power.
     *
     * @return true if the quarry can run, false otherwise.
     */
    public boolean isActive() {
        if (world.getBlockState(pos).getBlock() == TeckleObjects.blockBeamQuarry)
            return world.getBlockState(pos).getValue(BlockBeamQuarry.ACTIVE);
        return false;
    }

    /**
     * Set the bounds to mine within, also updates the cursor.
     *
     * @param min the minimum position mining will be restricted in.
     * @param max the maximum position mining will be restricted in.
     */
    private void setBounds(BlockPos min, BlockPos max) {
        this.min = min;
        this.max = max;
        this.setCursor(min.add(new Vec3i(0, 4, 0)));

        if (!world.isRemote) {
            new TileUpdateMessage(world, pos).sendToAllWatching(this);
        }

        generateStairPositions();
    }

    private void generateStairPositions() {
        if (world.isRemote)
            return;

        stairPositions.clear();
        BlockPos currentPos = pos.offset(getFacing().getOpposite()).subtract(new Vec3i(0, 1, 0));
        EnumFacing offsetFace = getFacing().rotateY();
        int xMin, xMax;
        int zMin, zMax;
        xMin = this.min.getX() < this.max.getX() ? this.min.getX() : this.max.getX();
        xMax = this.min.getX() < this.max.getX() ? this.max.getX() : this.min.getX();
        zMin = this.min.getZ() < this.max.getZ() ? this.min.getZ() : this.max.getZ();
        zMax = this.min.getZ() < this.max.getZ() ? this.max.getZ() : this.min.getZ();

        for (int y = pos.getY() - 1; y > 0; y--) {
            stairPositions.add(0, new MutablePair<>(currentPos, false));
            BlockPos offsetPos = currentPos.offset(offsetFace);
            if (offsetFace.getAxis() == EnumFacing.Axis.X) {
                if (offsetPos.getX() > xMax || offsetPos.getX() < xMin) {
                    offsetFace = offsetFace.rotateY();
                }
            } else {
                if (offsetPos.getZ() > zMax || offsetPos.getZ() < zMin) {
                    offsetFace = offsetFace.rotateY();
                }
            }
            currentPos = currentPos.offset(offsetFace);
            currentPos = new BlockPos(currentPos.getX(), y, currentPos.getZ());
        }
    }

    public void setDimensions(int left, int right, int forward) {
        setDimensions(getFacing().getOpposite(), left, right, forward);
    }

    public void setDimensions(EnumFacing facing, int left, int right, int forward) {
        BlockPos basePos = pos.offset(facing);
        EnumFacing relativeLeft = facing.rotateYCCW();
        EnumFacing relativeRight = facing.rotateY();
        BlockPos min = basePos.offset(relativeLeft, left);
        BlockPos max = basePos.offset(relativeRight, right);
        max = max.offset(facing, forward);
        this.setBounds(min, max);
        this.left = left;
        this.right = right;
        this.forward = forward;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        this.readFromNBT(tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        this.readFromNBT(pkt.getNbtCompound());
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public Object getServerElement(EntityPlayer player) {
        return new ContainerBeamQuarry(this, player);
    }

    @Override
    public Object getClientElement(EntityPlayer player) {
        return new GuiBeamQuarry(this, player);
    }

    public EnumFacing getFacing() {
        if (world.isBlockLoaded(pos) && world.getBlockState(pos).getBlock() == TeckleObjects.blockBeamQuarry)
            facing = world.getBlockState(pos).getValue(BlockBeamQuarry.FACING);
        return facing;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing capFace) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY) {
            if (capFace == null)
                return false;
            EnumFacing facing = getFacing();
            if (capFace == EnumFacing.UP || (facing != null && capFace.getAxis() == facing.rotateY().getAxis())) {
                return true;
            }
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (facing != null) {
            if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
                if (facing == EnumFacing.UP) {
                    return (T) junkSupply.getHandler();
                } else if (facing.getAxis() == getFacing().rotateY().getAxis()) {
                    return (T) bufferData.getHandler();
                }
            } else if (capability == CapabilityWorldNetworkTile.NETWORK_TILE_CAPABILITY) {
                if (facing == EnumFacing.UP) {
                    return (T) topNetworkTile;
                } else if (facing == getFacing().getOpposite().rotateY()) {
                    return (T) rightNetworkTile;
                } else if (facing == getFacing().getOpposite().rotateYCCW()) {
                    return (T) leftNetworkTile;
                }
            }
        }
        return super.getCapability(capability, facing);
    }

    public BlockPos getCursor() {
        return cursor;
    }

    public void setCursor(BlockPos cursor) {
        this.cursor = cursor;
    }
}
