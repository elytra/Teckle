package com.elytradev.teckle.common;

import com.elytradev.concrete.config.ConcreteConfig;
import com.elytradev.concrete.config.ConfigValue;
import net.minecraftforge.common.config.Property;

import java.io.File;

public class TeckleConfiguration extends ConcreteConfig {

    @ConfigValue(type = Property.Type.INTEGER, category = "oregen")
    public int nikoliteCount = 8, nikoliteSize = 8, nikoliteMinHeight = 0, nikoliteMaxHeight = 16;

    @ConfigValue(type = Property.Type.INTEGER, category = "machine-cooldowns")
    public int transposerCooldown = 3, filterCooldown = 5, sortingMachineCooldown = 5, retrieverCooldown = 5, fabricatorCooldown = 5;

    @ConfigValue(type = Property.Type.BOOLEAN, category = "misc", comment = "Determines if the alloy furnace will import all available furnace recipes.")
    public boolean importFurnaceRecipes = true;

    protected TeckleConfiguration(File configFile, String modID) {
        super(configFile, modID);
    }
}
