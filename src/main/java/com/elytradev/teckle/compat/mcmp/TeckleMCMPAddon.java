package com.elytradev.teckle.compat.mcmp;

import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.tile.TileItemTube;
import mcmultipart.api.addon.IMCMPAddon;
import mcmultipart.api.addon.MCMPAddon;
import mcmultipart.api.multipart.IMultipartRegistry;
import mcmultipart.api.multipart.IMultipartTile;
import mcmultipart.api.ref.MCMPCapabilities;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Teckle MCMP addon, used to provide compat when MCMP is present.
 */
@MCMPAddon
public class TeckleMCMPAddon implements IMCMPAddon {

    @Override
    public void registerParts(IMultipartRegistry registry) {
        //TODO: Disabled due to bugs, will enable again once I can fix it.

        //MinecraftForge.EVENT_BUS.register(this);

        //registry.registerPartWrapper(TeckleObjects.blockItemTube, new MultipartTube());
        //Item itemTube = Item.REGISTRY.getObject(TeckleObjects.blockItemTube.getRegistryName());
        //registry.registerStackWrapper(itemTube, s -> true, TeckleObjects.blockItemTube);
    }

    @SubscribeEvent
    public void onAttachTileCaps(AttachCapabilitiesEvent<TileEntity> e) {
        if (e.getObject() instanceof TileItemTube) {
            e.addCapability(new ResourceLocation(TeckleMod.MOD_ID, "multipart"), new ICapabilityProvider() {
                IMultipartTile multipartTileTube = IMultipartTile.wrap(e.getObject());

                @Override
                public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
                    return capability == MCMPCapabilities.MULTIPART_TILE;
                }

                @Nullable
                @Override
                public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
                    if (capability == MCMPCapabilities.MULTIPART_TILE)
                        return (T) multipartTileTube;
                    return null;
                }
            });
        }
    }
}
