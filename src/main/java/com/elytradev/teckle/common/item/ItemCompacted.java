package com.elytradev.teckle.common.item;

import com.elytradev.concrete.resgen.EnumResourceType;
import com.elytradev.concrete.resgen.IResourceHolder;
import com.elytradev.teckle.common.TeckleMod;
import net.minecraft.block.Block;
import net.minecraft.item.ItemColored;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

public class ItemCompacted extends ItemColored implements IResourceHolder {
    public ItemCompacted(Block block, boolean hasSubtypes) {
        super(block, hasSubtypes);
    }

    @Nullable
    @Override
    public ResourceLocation getResource(EnumResourceType resourceType, int meta) {
        if (resourceType == EnumResourceType.TEXTURE) {
            ItemIngot.IngotType ingotType = ItemIngot.IngotType.byMetadata(meta);
            if (ingotType == ItemIngot.IngotType.BRASS) ingotType = ItemIngot.IngotType.RED_ALLOY;
            return new ResourceLocation(TeckleMod.MOD_ID, "blocks/" + ingotType.getName() + "_block");
        }
        return null;
    }
}
