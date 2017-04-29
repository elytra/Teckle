package com.elytradev.concrete.resgen;

import com.elytradev.concrete.reflect.accessor.Accessor;
import com.elytradev.concrete.reflect.accessor.Accessors;
import com.elytradev.concrete.reflect.invoker.Invoker;
import com.elytradev.concrete.reflect.invoker.Invokers;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.*;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.registry.RegistryDelegate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Custom resource pack that auto overwrites the default one FML generates,
 * allows for auto genning common models like cubes and sprite based items.
 *
 * @author darkevilmac (Benjamin K)
 * @see IResourceHolder for custom texture locations for items and blocks.
 */
public class SimpleResourcePack extends AbstractResourcePack implements IResourceManagerReloadListener {

    private static final Logger LOG = LogManager.getLogger("Concrete");

    private static Accessor<Map<String, IResourcePack>> resourcePackMap = Accessors.findField(FMLClientHandler.class, "resourcePackMap");
    private static Accessor<List<IResourcePack>> resourcePackList = Accessors.findField(FMLClientHandler.class, "resourcePackList");
    private static Accessor<File> resourcePackFile = Accessors.findField(AbstractResourcePack.class, "field_110597_b", "resourcePackFile");
    private static Accessor<IResourcePack> legacyPack = Accessors.findField(LegacyV2Adapter.class, "field_191383_a", "pack");
    private static Accessor<Map<String, FallbackResourceManager>> domainResourceManagers = Accessors.findField(SimpleReloadableResourceManager.class, "field_110548_a", "domainResourceManagers");
    private static Accessor<List<IResourcePack>> resourcePacks = Accessors.findField(FallbackResourceManager.class, "field_110540_a", "resourcePacks");

    private static Invoker hasResourceName = Invokers.findMethod(AbstractResourcePack.class, null, new String[]{"func_110593_b", "hasResourceName"}, String.class);
    private static Invoker getInputStreamByName = Invokers.findMethod(AbstractResourcePack.class, null, new String[]{"func_110591_a", "getInputStreamByName"}, String.class);

    private static String SIMPLE_BLOCK_MODEL;
    private static String SIMPLE_ITEM_MODEL;
    private static String SIMPLE_BLOCK_STATE;

    static {
        try {
            SIMPLE_BLOCK_MODEL = IOUtils.toString(SimpleResourcePack.class.getResourceAsStream("simpleblock.json"));
            SIMPLE_ITEM_MODEL = IOUtils.toString(SimpleResourcePack.class.getResourceAsStream("simpleitem.json"));
            SIMPLE_BLOCK_STATE = IOUtils.toString(SimpleResourcePack.class.getResourceAsStream("simplestate.json"));
        } catch (IOException e) {
            LOG.error("Caught IOException loading simple models, things will not definitely not work.", e);
        }
    }

    public AbstractResourcePack fallbackResourcePack;
    public String modID;
    public HashMap<String, String> cache;

    /**
     * Create a SimpleResourcePack for the specified mod, SimplePacks will auto gen simplemodels if you lack a blockstate or model file.
     * If the applicable file exists it will simply default to it.
     *
     * @param modID the id of the mod you want a simple pack for.
     */
    public SimpleResourcePack(String modID) {
        super(getPackFileByModID(modID));
        this.modID = modID;
        this.cache = Maps.newHashMap();

        // Replace the current FML pack with this.
        IResourcePack fallback = resourcePackMap.get(FMLClientHandler.instance()).replace(modID, this);
        if (fallback instanceof LegacyV2Adapter) {
            this.fallbackResourcePack = (AbstractResourcePack) legacyPack.get(fallback);
        } else if (fallback instanceof AbstractResourcePack) {
            this.fallbackResourcePack = (AbstractResourcePack) fallback;
        }

        if (fallback == null || this.fallbackResourcePack == null) {
            throw new MissingFallbackException(modID);
        }

        // Overwrite the current pack list entry in forge.
        resourcePackList.get(FMLClientHandler.instance()).remove(fallbackResourcePack);
        resourcePackList.get(FMLClientHandler.instance()).add(this);

        if (Minecraft.getMinecraft().getResourceManager() instanceof SimpleReloadableResourceManager) {
            SimpleReloadableResourceManager resourceManager = (SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager();
            resourceManager.registerReloadListener(this);

            // Forces this resource pack to be loaded early on. FML already did it's initial registration so we need to bypass that.
            FallbackResourceManager domainManager = domainResourceManagers.get(resourceManager).get(modID);
            resourcePacks.get(domainManager).clear();
            resourcePacks.get(domainManager).add(this);
        }
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

    public static ResourceLocation nameToLocation(String name) {
        name = name.substring(name.indexOf("/") + 1);
        String domain = name.substring(0, name.indexOf("/"));
        String path = name.substring(name.indexOf("/") + 1);

        LOG.debug("Converted " + name + " to " + new ResourceLocation(domain, path));
        return new ResourceLocation(domain, path);
    }

    private static Map<Pair<RegistryDelegate<Item>, Integer>, ModelResourceLocation> getCustomModels() {
        try {
            Field field = ModelLoader.class.getDeclaredField("customModels");
            return (Map<Pair<RegistryDelegate<Item>, Integer>, ModelResourceLocation>) FieldUtils.readStaticField(field, true);
        } catch (Exception e) {
            LOG.error("Caught exception getting customModels from the model loader, ", e);
        }

        return Maps.newHashMap();
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        // Default to fallback if possible.
        if (!((boolean) hasResourceName.invoke(fallbackResourcePack, name))) {
            if (cache.containsKey(name)) {
                LOG.debug("SimpleResourcePack was asked to obtain: " + name + " using cache.");
                return IOUtils.toInputStream(cache.get(name));
            }

            LOG.debug("SimpleResourcePack was asked to obtain: " + name);
            if (isLocation(name, "/blockstates/")) {
                return IOUtils.toInputStream(getBlockState(name));
            } else if (isLocation(name, "/models/block/")) {
                return IOUtils.toInputStream(getBlockModel(name));
            } else if (isLocation(name, "/models/item/")) {
                return IOUtils.toInputStream(getItemModel(name));
            }
        }

        return (InputStream) getInputStreamByName.invoke(fallbackResourcePack, name);
    }

    /**
     * Generates a string of JSON representing a blockstate for the given location.
     **/
    private String getBlockState(String name) {
        String blockID = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."));
        Block blockFromLocation = Block.getBlockFromName(modID + ":" + blockID);
        String modelLocation = modID + ":" + blockID;
        if (blockFromLocation instanceof IResourceHolder) {
            modelLocation = ((IResourceHolder) blockFromLocation).getResource(EnumResourceType.MODEL, 0).toString();
            modelLocation = modelLocation.substring(modelLocation.lastIndexOf("/" + 1));
            modelLocation = modID + ":" + modelLocation;
        }

        String simpleBlockState = SIMPLE_BLOCK_STATE;
        simpleBlockState = simpleBlockState.replaceAll("#MDL", modelLocation);
        cache.put(name, simpleBlockState);
        return simpleBlockState;
    }

    /**
     * Generates a string of JSON representing a block model for the given location.
     **/
    private String getBlockModel(String name) {
        String blockID = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."));
        Block blockFromLocation = Block.getBlockFromName(modID + ":" + blockID);
        String textureLocation = modID + ":blocks/" + blockID;
        Integer meta = getMetaFromName(name);

        if (blockFromLocation instanceof IResourceHolder) {
            textureLocation = ((IResourceHolder) blockFromLocation).getResource(EnumResourceType.TEXTURE, meta).toString();
        }

        String simpleBlockJSON = SIMPLE_BLOCK_MODEL;
        simpleBlockJSON = simpleBlockJSON.replaceAll("#ALL", textureLocation);
        cache.put(name, simpleBlockJSON);
        return simpleBlockJSON;
    }

    /**
     * Generates a string of JSON representing an item model for the given location.
     **/
    private String getItemModel(String name) {
        String itemID = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."));
        Item itemFromLocation = getItem(name);
        String textureLocation = modID + ":items/" + itemID;
        Integer meta = getMetaFromName(name);

        if (itemFromLocation instanceof IResourceHolder) {
            textureLocation = ((IResourceHolder) itemFromLocation).getResource(EnumResourceType.TEXTURE, meta).toString();
        }

        // Return a block model file if this is an ItemBlock.
        if (Block.getBlockFromItem(itemFromLocation) != Blocks.AIR) {
            if (itemFromLocation.getRegistryName().getResourcePath().equals(itemID)) {
                try {
                    cache.put(name, IOUtils.toString(getInputStreamByName(name.replace("/item/", "/block/"))));
                } catch (IOException e) {
                    LOG.error("Failed to get item model for " + name);
                }
            } else {
                String simpleBlockModel = SIMPLE_BLOCK_MODEL;
                simpleBlockModel = simpleBlockModel.replaceAll("#ALL", textureLocation);
                cache.put(name, simpleBlockModel);
            }
            return cache.get(name);
        }

        String simpleItemModel = SIMPLE_ITEM_MODEL;
        simpleItemModel = simpleItemModel.replaceAll("#L0", textureLocation);
        cache.put(name, simpleItemModel);
        return simpleItemModel;
    }

    private Item getItem(String name) {
        String itemID = name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."));
        if (Item.getByNameOrId(modID + ":" + itemID) != null) {
            return Item.getByNameOrId(modID + ":" + itemID);
        } else {
            ResourceLocation location = nameToLocation(name);
            try {
                HashBiMap<Pair<RegistryDelegate<Item>, Integer>, ModelResourceLocation> customModelsMap = HashBiMap.create(getCustomModels());
                String resourcePath = location.getResourcePath();
                resourcePath = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
                resourcePath = resourcePath.substring(0, resourcePath.lastIndexOf("."));
                String domain = location.getResourceDomain();
                location = new ModelResourceLocation(domain + ":" + resourcePath, isLocation(name, "/models/item/") ? "inventory" : "normal");
                if (customModelsMap.inverse().containsKey(location))
                    return customModelsMap.inverse().get(location).getLeft().get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Items.AIR;
        }
    }

    /**
     * Attempt to get metadata value from ModelLoader to account for custom mesh locations, returns 0 if none were found.
     *
     * @param name name provided by getInputStreamByName
     * @return metadata if found, 0 if none.
     */
    private Integer getMetaFromName(String name) {
        ResourceLocation location = nameToLocation(name);
        try {
            HashBiMap<Pair<RegistryDelegate<Item>, Integer>, ModelResourceLocation> customModelsMap = HashBiMap.create(getCustomModels());
            String resourcePath = location.getResourcePath();
            resourcePath = resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
            resourcePath = resourcePath.substring(0, resourcePath.lastIndexOf("."));
            String domain = location.getResourceDomain();
            location = new ModelResourceLocation(domain + ":" + resourcePath, isLocation(name, "/models/item/") ? "inventory" : "normal");
            if (customModelsMap.inverse().containsKey(location))
                return customModelsMap.inverse().get(location).getRight();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    protected boolean hasResourceName(String name) {
        if (isLocation(name, "/blockstates/")) {
            LOG.debug("Location was provided " + name + ", matched blockstate check.");
            return true;
        }
        if (isLocation(name, "/models/block/")) {
            LOG.debug("Location was provided " + name + ", matched block model check.");
            return true;
        }
        if (isLocation(name, "/models/item/")) {
            LOG.debug("Location was provided " + name + ", matched item model check.");
            return true;
        }
        LOG.debug("Unknown location was provided " + name + ", asking fallback.");
        return (boolean) hasResourceName.invoke(fallbackResourcePack, name);
    }

    @Override
    public Set<String> getResourceDomains() {
        return fallbackResourcePack.getResourceDomains();
    }

    /**
     * Check if the place provided matches the location validation.
     *
     * @param place      The place to check.
     * @param validation The validation to use.
     * @return true if matches, false otherwise.
     */
    private boolean isLocation(String place, String validation) {
        return place.startsWith(("assets/" + modID + validation)) && place.endsWith(".json");
    }

    /**
     * Auto clears the cache on a resource manager reload.
     *
     * @param iResourceManager
     */
    @Override
    public void onResourceManagerReload(IResourceManager iResourceManager) {
        cache.clear();
    }
}
