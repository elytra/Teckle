package com.elytradev.teckle.client.resources;

import com.elytradev.concrete.reflect.accessor.Accessor;
import com.elytradev.concrete.reflect.accessor.Accessors;
import com.elytradev.concrete.reflect.invoker.Invoker;
import com.elytradev.concrete.reflect.invoker.Invokers;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import net.minecraftforge.fml.client.FMLClientHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by darkevilmac on 4/27/2017.
 */
public class SimpleResourcePack extends AbstractResourcePack {

    public static Accessor<Map<String, IResourcePack>> resourcePackMap = Accessors.findField(FMLClientHandler.class, "resourcePackMap");
    public static Accessor<List<IResourcePack>> resourcePackList = Accessors.findField(FMLClientHandler.class, "resourcePackList");
    public static Accessor<File> resourcePackFile = Accessors.findField(AbstractResourcePack.class, "field_110597_b", "resourcePackFile");
    public static Accessor<IResourcePack> legacyPack = Accessors.findField(LegacyV2Adapter.class, "field_191383_a", "pack");

    public static Invoker hasResourceName = Invokers.findMethod(AbstractResourcePack.class, null, new String[]{"func_110593_b", "hasResourceName"}, String.class);
    public static Invoker getInputStreamByName = Invokers.findMethod(AbstractResourcePack.class, null, new String[]{"func_110591_a", "getInputStreamByName"}, String.class);

    public AbstractResourcePack fallbackResourcePack;

    public SimpleResourcePack(String modID) {
        super(getPackFileByModID(modID));

        IResourcePack fallback = resourcePackMap.get(FMLClientHandler.instance()).replace(modID, this);
        if (fallback instanceof LegacyV2Adapter) {
            this.fallbackResourcePack = (AbstractResourcePack) legacyPack.get(fallback);
        } else if (fallback instanceof AbstractResourcePack) {
            this.fallbackResourcePack = (AbstractResourcePack) fallback;
        }
        resourcePackList.get(FMLClientHandler.instance()).remove(fallbackResourcePack);
        resourcePackList.get(FMLClientHandler.instance()).add(this);
    }

    private static File getPackFileByModID(String modID) {
        IResourcePack pack = FMLClientHandler.instance().getResourcePackFor(modID);
        if (pack instanceof LegacyV2Adapter) {
            return resourcePackFile.get(legacyPack.get(pack));
        } else if (pack instanceof AbstractResourcePack) {
            return resourcePackFile.get(pack);
        }
        return null;
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        return (InputStream) getInputStreamByName.invoke(fallbackResourcePack, name);
    }

    @Override
    protected boolean hasResourceName(String name) {
        return (boolean) hasResourceName.invoke(fallbackResourcePack, name);
    }

    @Override
    public Set<String> getResourceDomains() {
        return fallbackResourcePack.getResourceDomains();
    }
    
}
