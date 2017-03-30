package com.elytradev.teckle.common.block.property;

import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * Created by darkevilmac on 3/30/2017.
 */
public class UnlistedBool implements IUnlistedProperty<Boolean> {

    String name;

    public UnlistedBool(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid(Boolean value) {
        return true;
    }

    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }

    @Override
    public String valueToString(Boolean value) {
        return value.toString();
    }
}
