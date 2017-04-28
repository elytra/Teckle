package com.elytradev.concrete.resgen;

import com.elytradev.concrete.reflect.accessor.Accessor;
import com.elytradev.concrete.reflect.accessor.Accessors;
import com.elytradev.concrete.reflect.invoker.Invoker;
import com.elytradev.concrete.reflect.invoker.Invokers;
import com.elytradev.teckle.common.TeckleMod;
import net.minecraft.block.Block;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.LegacyV2Adapter;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.apache.commons.io.IOUtils;

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
    private static String SIMPLE_BLOCK_MODEL;
    private static String SIMPLE_ITEM_MODEL;
    private static String SIMPLE_BLOCK_STATE;

    static {
        try {
            SIMPLE_BLOCK_MODEL = IOUtils.toString(SimpleResourcePack.class.getResourceAsStream("simpleblock.json"));
            SIMPLE_ITEM_MODEL = IOUtils.toString(SimpleResourcePack.class.getResourceAsStream("simpleitem.json"));
            SIMPLE_BLOCK_STATE = IOUtils.toString(SimpleResourcePack.class.getResourceAsStream("simplestate.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AbstractResourcePack fallbackResourcePack;
    public String modID;

    public SimpleResourcePack(String modID) {
        super(getPackFileByModID(modID));
        this.modID = modID;

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
        TeckleMod.LOG.debug("SimpleResourcePack was asked to obtain: " + name);
        if (!((boolean) hasResourceName.invoke(fallbackResourcePack, name))) {
            if (isLocation(name, "/blockstates/")) {
                String blockID = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."));
                Block blockFromLocation = Block.getBlockFromName(modID + ":" + blockID);
                String modelLocation = modID + ":" + blockID;
                if (blockFromLocation instanceof ITexturedObject) {
                    modelLocation = ((ITexturedObject) blockFromLocation).getTextureLocation().toString();
                    modelLocation = modelLocation.substring(modelLocation.lastIndexOf("/" + 1));
                    modelLocation = modID + ":" + modelLocation;
                }

                String simpleBlockState = SIMPLE_BLOCK_STATE;
                simpleBlockState = simpleBlockState.replaceAll("#MDL", modelLocation);
                return IOUtils.toInputStream(simpleBlockState);
            } else if (isLocation(name, "/models/block/")) {
                String blockID = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."));
                Block blockFromLocation = Block.getBlockFromName(modID + ":" + blockID);
                String textureLocation = modID + ":blocks/" + blockID;
                if (blockFromLocation instanceof ITexturedObject) {
                    textureLocation = ((ITexturedObject) blockFromLocation).getTextureLocation().toString();
                }

                String simpleBlockJSON = SIMPLE_BLOCK_MODEL;
                simpleBlockJSON = simpleBlockJSON.replaceAll("#ALL", textureLocation);
                return IOUtils.toInputStream(simpleBlockJSON);
            } else if (isLocation(name, "/models/items/")) {
                String itemID = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."));
                Item itemFromLocation = Item.getByNameOrId(modID + ":" + itemID);
                String textureLocation = modID + ":items/" + itemID;
                if (itemFromLocation instanceof ITexturedObject) {
                    textureLocation = ((ITexturedObject) itemFromLocation).getTextureLocation().toString();
                }

                String simpleItemModel = SIMPLE_ITEM_MODEL;
                simpleItemModel = simpleItemModel.replaceAll("#L0", textureLocation);
                return IOUtils.toInputStream(simpleItemModel);
            }
        }

        return (InputStream) getInputStreamByName.invoke(fallbackResourcePack, name);
    }

    @Override
    protected boolean hasResourceName(String name) {
        if (isLocation(name, "/blockstates/") || isLocation(name, "/models/block/") || isLocation(name, "/models/items/")) {
            return true;
        } else {
            return (boolean) hasResourceName.invoke(fallbackResourcePack, name);
        }
    }

    @Override
    public Set<String> getResourceDomains() {
        return fallbackResourcePack.getResourceDomains();
    }

    private boolean isLocation(String place, String validation) {
        return place.startsWith(("assets/" + modID + validation)) && place.endsWith(".json");
    }

}
