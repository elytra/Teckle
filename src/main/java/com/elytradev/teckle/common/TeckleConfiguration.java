package com.elytradev.teckle.common;

import com.elytradev.concrete.config.ConcreteConfig;
import com.elytradev.concrete.config.ConfigValue;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

public class TeckleConfiguration extends ConcreteConfig {

    public File configFolder;

    @ConfigValue(type = Property.Type.INTEGER, category = "oregen")
    public int nikoliteCount = 8, nikoliteSize = 8, nikoliteMinHeight = 0, nikoliteMaxHeight = 16;

    @ConfigValue(type = Property.Type.INTEGER, category = "machine-cooldowns")
    public int transposerCooldown = 3, filterCooldown = 5, sortingMachineCooldown = 5, retrieverCooldown = 5, fabricatorCooldown = 5, breakerCooldown = 6;

    @ConfigValue(type = Property.Type.BOOLEAN, category = "misc", comment = "Determines if the alloy furnace will import all available furnace recipes.")
    public boolean importFurnaceRecipes = true;

    @ConfigValue(type = Property.Type.INTEGER, category = "worldnetwork", comment = "Determines the amount of ticks that pass between cleaning a network database.")
    public int databaseCleaningCooldown = 160;

    @ConfigValue(type = Property.Type.BOOLEAN, category = "advanced", comment = "Disables ore checks when Teckle is launcing, only disable if you plan to replace default recipes." +
            " WARNING: Alloy Recipes are currently hardcoded and no system is currently im place to remove them without another mod interaction!")
    public boolean skipOreChecks = false;

    private TeckleConfiguration(File configFile) {
        super(configFile, TeckleMod.MOD_ID);
        this.configFolder = configFile.getParentFile();
    }

    /**
     * Create a config file for Teckle, moves any existing legacy config as well.
     *
     * @param e FMLPreInitializationEvent for the mod.
     * @return an instance of the config class.
     */
    public static TeckleConfiguration createConfig(FMLPreInitializationEvent e) {
        //Move config file if it exists.
        File teckleFolder = new File(e.getModConfigurationDirectory(), "teckle");
        teckleFolder.mkdirs();
        if (e.getSuggestedConfigurationFile().exists()) {
            e.getSuggestedConfigurationFile().renameTo(new File(teckleFolder, "teckle.cfg"));
        }

        TeckleConfiguration config = new TeckleConfiguration(new File(teckleFolder, "teckle.cfg"));
        config.loadConfig();
        return config;
    }
}
