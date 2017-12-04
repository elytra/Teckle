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

package com.elytradev.teckle.common.item;

import com.elytradev.concrete.resgen.EnumResourceType;
import com.elytradev.concrete.resgen.IResourceHolder;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import java.util.Objects;

public class ItemSiliconWafer extends Item implements IResourceHolder {

    public ItemSiliconWafer() {
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + WaferType.byMetadata(stack.getMetadata()).getName();
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> subItems) {
        if (Objects.equals(tab, TeckleObjects.creativeTab) || Objects.equals(tab, CreativeTabs.SEARCH)) {
            for (WaferType waferType : WaferType.values()) {
                subItems.add(new ItemStack(this, 1, waferType.getMetadata()));
            }
        }
    }

    @Override
    public boolean getHasSubtypes() {
        return true;
    }

    @Override
    public boolean isDamageable() {
        return false;
    }

    @Override
    public ResourceLocation getResource(EnumResourceType resourceType, int meta) {
        ResourceLocation result = new ResourceLocation(TeckleMod.RESOURCE_DOMAIN + "items/siliconwafer_missingno");
        WaferType type = WaferType.byMetadata(meta);
        if (type != null) {
            result = new ResourceLocation(TeckleMod.RESOURCE_DOMAIN + "items/siliconwafer_" + type.getName());
        }

        return result;
    }

    public enum WaferType implements IStringSerializable {
        PLAIN(0, "plain", ""),
        RED(1, "red", "_red"),
        BLUE(2, "blue", "_blue");

        private static final ItemSiliconWafer.WaferType[] META_LOOKUP = new ItemSiliconWafer.WaferType[values().length];

        static {
            for (WaferType waferType : values()) {
                META_LOOKUP[waferType.getMetadata()] = waferType;
            }
        }

        private final int meta;
        private final String name;
        private String suffix;

        WaferType(int metaIn, String nameIn, String suffix) {
            this.meta = metaIn;
            this.name = nameIn;
            this.suffix = suffix;
        }

        public static ItemSiliconWafer.WaferType byMetadata(int meta) {
            if (meta < 0 || meta >= META_LOOKUP.length) {
                meta = 0;
            }

            return META_LOOKUP[meta];
        }

        public int getMetadata() {
            return this.meta;
        }

        public String toString() {
            return this.name;
        }

        @Override
        public String getName() {
            return this.name;
        }

        public String getSuffix() {
            return this.suffix;
        }
    }
}
