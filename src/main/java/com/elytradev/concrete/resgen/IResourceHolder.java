package com.elytradev.concrete.resgen;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public interface IResourceHolder {

    @SideOnly(Side.CLIENT)
    ResourceLocation getResource(EnumResourceType resourceType, int meta);

}
