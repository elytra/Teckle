package com.elytradev.teckle.common.crafting;

import com.elytradev.teckle.common.TeckleObjects;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

/**
 * Created by darkevilmac on 4/19/2017.
 */
public class AlloyRecipes {

    private static final AlloyRecipes INSTANCE = new AlloyRecipes();
    public List<AlloyRecipe> recipes;

    public AlloyRecipes() {
    }

    public static AlloyRecipes getInstance() {
        return INSTANCE;
    }

    public void init() {
        OreDictionary.registerOre("coal", Items.COAL); // Nothing to see here, move along.

        recipes.add(new AlloyRecipe(new ItemStack(TeckleObjects.itemSiliconBoule), new Tuple<>("sand", 8), new Tuple<>("coal", 8)));
    }


}
