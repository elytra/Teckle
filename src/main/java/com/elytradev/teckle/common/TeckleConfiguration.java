package com.elytradev.teckle.common;

import com.elytradev.concrete.config.ConcreteConfig;
import com.elytradev.concrete.config.ConfigValue;
import net.minecraftforge.common.config.Property;

import java.io.File;

public class TeckleConfiguration extends ConcreteConfig {

    @ConfigValue(type = Property.Type.INTEGER, category = "oregen")
    public int nikoliteCount = 8, nikoliteSize = 8, nikoliteMinHeight = 0, nikoliteMaxHeight = 16;

    protected TeckleConfiguration(File configFile, String modID) {
        super(configFile, modID);
    }
}
