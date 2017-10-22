package com.elytradev.teckle.common.helper;

import net.minecraft.item.EnumDyeColor;

import javax.annotation.Nullable;

public class ColourHelper {

    /**
     * Cycle a colour through possible values including null.
     *
     * @param colour  the colour to cycle from
     * @param reverse whether the cycle is reversed
     * @return the cycled colour.
     */
    @Nullable
    public static EnumDyeColor cycleColour(EnumDyeColor colour, boolean reverse) {
        if (!reverse) {
            if (colour == null) {
                colour = EnumDyeColor.byMetadata(0);
            } else {
                if (colour.getMetadata() == 15) {
                    colour = null;
                } else {
                    colour = EnumDyeColor.byMetadata(colour.getMetadata() + 1);
                }
            }
        } else {
            if (colour == null) {
                colour = EnumDyeColor.byMetadata(15);
            } else {
                if (colour.getMetadata() == 0) {
                    colour = null;
                } else {
                    colour = EnumDyeColor.byMetadata(colour.getMetadata() - 1);
                }
            }
        }
        return colour;
    }

}
