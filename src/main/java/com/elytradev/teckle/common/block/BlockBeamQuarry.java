package com.elytradev.teckle.common.block;

import com.elytradev.teckle.api.capabilities.CapabilityWorldNetworkAssistantHolder;
import com.elytradev.teckle.api.capabilities.IWorldNetworkAssistant;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.handlers.TeckleGuiHandler;
import com.elytradev.teckle.common.network.messages.clientbound.TileUpdateMessage;
import com.elytradev.teckle.common.tile.TileBeamQuarry;
import com.elytradev.teckle.common.tile.inv.pool.AdvancedStackHandlerPool;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

import javax.annotation.Nullable;

public class BlockBeamQuarry extends BlockContainer {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public BlockBeamQuarry(Material materialIn) {
        super(materialIn);

        this.setHarvestLevel("pickaxe", 0);
        this.setHardness(2.0F);
        this.setResistance(10.0F);
        this.setDefaultState(blockState.getBaseState());
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        EnumFacing direction = placer.getHorizontalFacing().getOpposite();
        return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand).withProperty(FACING, direction).withProperty(ACTIVE, false);
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);

        getNetworkHelper(worldIn).onNodePlaced(worldIn, pos);
        if (!worldIn.isRemote) {
            TileEntity tileEntity = worldIn.getTileEntity(pos);
            if (tileEntity instanceof TileBeamQuarry) {
                ((TileBeamQuarry) tileEntity).setDimensions(4, 4, 8);
            }
            new TileUpdateMessage(worldIn, pos).sendToAllWatching(worldIn, pos);
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!playerIn.isSneaking()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity != null) {
                playerIn.openGui(TeckleMod.INSTANCE, TeckleGuiHandler.ElementType.ELEMENT_PROVIDER.caseNumber, worldIn, pos.getX(), pos.getY(), pos.getZ());
                return true;
            }
        }

        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ACTIVE);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing front = EnumFacing.byIndex(meta & 7);
        if (front.getAxis() == EnumFacing.Axis.Y) {
            front = EnumFacing.NORTH;
        }
        return this.getDefaultState().withProperty(FACING, front).withProperty(ACTIVE, (meta & 8) > 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int i = 0;
        i = i | state.getValue(FACING).getIndex();

        if (state.getValue(ACTIVE)) {
            i |= 8;
        }

        return i;
    }

    @Override
    public void onNeighborChange(IBlockAccess blockAccess, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(blockAccess, pos, neighbor);
        TileEntity tile = blockAccess.getTileEntity(pos);

        getNetworkHelper(tile.getWorld()).onNodeNeighbourChange(tile.getWorld(), pos, neighbor);
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileAtPos = worldIn.getTileEntity(pos);
        if (tileAtPos != null) {
            getNetworkHelper(worldIn).onNodeBroken(worldIn, pos);

            if (tileAtPos instanceof TileBeamQuarry) {
                TileBeamQuarry beamQuarry = (TileBeamQuarry) worldIn.getTileEntity(pos);

                // Vomit the buffer.
                beamQuarry.bufferData.getHandler().stream().filter(stack -> !stack.isEmpty()).forEach(stack -> InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack));
                // Vomit the junk supply.
                beamQuarry.junkSupply.getHandler().stream().filter(stack -> !stack.isEmpty()).forEach(stack -> InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack));
                // Vomit the junk types.
                beamQuarry.junkTypes.stream().filter(stack -> !stack.isEmpty()).forEach(stack -> InventoryHelper.spawnItemStack(worldIn, pos.getX(), pos.getY(), pos.getZ(), stack));

                AdvancedStackHandlerPool.getPool(worldIn).remove(beamQuarry.bufferData.getId());
                AdvancedStackHandlerPool.getPool(worldIn).remove(beamQuarry.junkSupply.getId());

                if (!worldIn.isRemote) {
                    ForgeChunkManager.Ticket ticket = ForgeChunkManager.requestTicket(TeckleMod.INSTANCE, worldIn, ForgeChunkManager.Type.NORMAL);
                    for (ChunkPos chunkPos : ((TileBeamQuarry) tileAtPos).chunksInBounds()) {
                        ForgeChunkManager.unforceChunk(ticket, chunkPos);
                    }
                }
            }
        }

        // Call super after we're done so we still have access to the tile.
        super.breakBlock(worldIn, pos, state);
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileBeamQuarry();
    }

    public IWorldNetworkAssistant<ItemStack> getNetworkHelper(World world) {
        return world.getCapability(CapabilityWorldNetworkAssistantHolder.NETWORK_ASSISTANT_HOLDER_CAPABILITY, null).getAssistant(ItemStack.class);
    }

}
