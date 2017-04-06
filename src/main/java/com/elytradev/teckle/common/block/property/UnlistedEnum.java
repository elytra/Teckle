package com.elytradev.teckle.common.block.property;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import net.minecraft.util.IStringSerializable;
import net.minecraftforge.common.property.IUnlistedProperty;

import java.util.Map;

/**
 * Created by darkevilmac on 4/5/2017.
 */
public class UnlistedEnum<T extends Enum<T> & IStringSerializable> implements IUnlistedProperty<T> {
    private final ImmutableSet<T> allowedValues;
    private final Map<String, T> nameToValue = Maps.<String, T>newHashMap();
    String name;
    private Class<T> valueClass;

    public UnlistedEnum(String name, Class<T> valueClass) {
        this.name = name;
        this.valueClass = valueClass;
        this.allowedValues = ImmutableSet.copyOf(valueClass.getEnumConstants());

        for (T t : allowedValues) {
            String s = t.getName();

            if (this.nameToValue.containsKey(s)) {
                throw new IllegalArgumentException("Multiple values have the same name \'" + s + "\'");
            }

            this.nameToValue.put(s, t);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid(T value) {
        return value == null || allowedValues.contains(value);
    }

    @Override
    public Class<T> getType() {
        return this.valueClass;
    }

    @Override
    public String valueToString(T value) {
        return value.toString();
    }
}
