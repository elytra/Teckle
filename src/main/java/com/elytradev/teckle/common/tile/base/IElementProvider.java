package com.elytradev.teckle.common.tile.base;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


/**
 * Used by TeckleGuiHandler to handle tile guis.
 */
public interface IElementProvider {

    Object getServerElement(EntityPlayer player);

    @SideOnly(Side.CLIENT)
    Object getClientElement(EntityPlayer player);

}
