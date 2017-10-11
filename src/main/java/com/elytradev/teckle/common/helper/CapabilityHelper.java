package com.elytradev.teckle.common.helper;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;

/**
 * Contains some helper methods for capabilities to save some space and reduce redundancy.
 */
public class CapabilityHelper {

    @CapabilityInject(IItemHandler.class)
    public static Capability<IItemHandler> ITEM_HANDLER_CAPABILITY;

    public static boolean isItemHandler(@Nullable ICapabilityProvider provider, EnumFacing face) {
        if (provider == null)
            return false;
        return provider.hasCapability(ITEM_HANDLER_CAPABILITY, face);
    }

    @Nullable
    public static IItemHandler getItemHandler(@Nullable ICapabilityProvider provider, EnumFacing face) {
        if (!isItemHandler(provider, face))
            return null;

        return provider.getCapability(ITEM_HANDLER_CAPABILITY, face);
    }
}
