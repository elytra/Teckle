/*
 *    Copyright 2017 Benjamin K (darkevilmac)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.elytradev.teckle.common.crafting;

import com.elytradev.teckle.common.TeckleLog;
import com.elytradev.teckle.common.TeckleMod;
import com.elytradev.teckle.common.TeckleObjects;
import com.elytradev.teckle.common.item.ItemIngot;
import com.elytradev.teckle.common.item.ItemSiliconWafer;
import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraftforge.oredict.OreDictionary;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class AlloyRecipes {

    private static final AlloyRecipes INSTANCE = new AlloyRecipes();
    private List<AlloyRecipe> recipes = new ArrayList<>();

    public static AlloyRecipes getInstance() {
        return INSTANCE;
    }

    public List<AlloyRecipe> getRecipes() {
        return Lists.newArrayList(recipes);
    }

    /**
     * Unregisters all the currently registered Alloy Recipes.
     */
    public void unregisterAll() {
        recipes.clear();
    }

    /**
     * Registers the given recipe for use with the Alloy Furnace.
     *
     * @param recipe the AlloyRecipe to register.
     */
    public void registerRecipe(AlloyRecipe recipe) {
        recipes.add(recipe);
    }

    /**
     * Removes all recipes matching the given predicate.
     *
     * @param matcher the predicate to check each recipe against.
     */
    public void unregisterMatching(Predicate<AlloyRecipe> matcher) {
        recipes.removeIf(matcher);
    }

    public void init() {
        unregisterAll();

        AlloyRecipe siliconBouleRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemSiliconBoule),
                new Tuple<>("sand", 8),
                new Tuple<>("coal", 8));
        registerRecipe(siliconBouleRecipe);

        AlloyRecipe redDopedWaferRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemSiliconWafer, 1, ItemSiliconWafer.WaferType.RED.getMetadata()),
                new Tuple<>("dustRedstone", 4),
                new Tuple<>(new ItemStack(TeckleObjects.itemSiliconWafer, 1, 0), null));
        registerRecipe(redDopedWaferRecipe);

        AlloyRecipe blueDopedWaferRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemSiliconWafer, 1, ItemSiliconWafer.WaferType.BLUE.getMetadata()),
                new Tuple<>("dustNikolite", 4),
                new Tuple<>(new ItemStack(TeckleObjects.itemSiliconWafer, 1, 0), null));
        registerRecipe(blueDopedWaferRecipe);

        AlloyRecipe brassIngotRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemIngot, 4, ItemIngot.IngotType.BRASS.getMetadata()),
                new Tuple<>("ingotTin", 1),
                new Tuple<>("ingotCopper", 3)
        );
        registerRecipe(brassIngotRecipe);

        AlloyRecipe redAlloyIngotRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemIngot, 4, ItemIngot.IngotType.RED_ALLOY.getMetadata()),
                new Tuple<>("ingotCopper", 1),
                new Tuple<>("dustRedstone", 4)
        );
        registerRecipe(redAlloyIngotRecipe);

        AlloyRecipe redAlloyIngotRecipeAlt = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemIngot, 4, ItemIngot.IngotType.RED_ALLOY.getMetadata()),
                new Tuple<>("ingotIron", 1),
                new Tuple<>("dustRedstone", 4)
        );
        registerRecipe(redAlloyIngotRecipeAlt);

        AlloyRecipe blueAlloyIngotRecipe = new AlloyRecipe(
                new ItemStack(TeckleObjects.itemIngot, 4, ItemIngot.IngotType.BLUE_ALLOY.getMetadata()),
                new Tuple<>("ingotSilver", 1),
                new Tuple<>("dustNikolite", 4)
        );
        registerRecipe(blueAlloyIngotRecipe);

        // Adds all the vanilla recipes to the alloy furnace.
        if (TeckleMod.CONFIG.importFurnaceRecipes)
            recipes.addAll(FurnaceRecipes.instance().getSmeltingList().entrySet().stream().map(this::convertFurnaceRecipe).collect(Collectors.toList()));

        File recipeFolder = new File(TeckleMod.CONFIG.configFolder, "alloyrecipes");
        recipeFolder.mkdirs();

        for (File file : recipeFolder.listFiles()) {
            if (file.isFile() && !file.isHidden() && !file.getName().startsWith("_")
                    && file.getName().toLowerCase().endsWith(".json")) {
                RecipeData data = null;
                try {
                    data = new Gson().fromJson(Resources.toString(file.toURI().toURL(), Charsets.UTF_8), RecipeData.class);
                } catch (Exception e) {
                    TeckleLog.error("Failed to load alloy recipe. File {}", file.toString());
                }
                if (data != null) {
                    List<Tuple<Object, Integer>> inputs = Lists.newArrayList();
                    boolean failed = false;
                    for (int i = 0; i < data.getInputs().length; i++) {
                        String input = data.getInputs()[i];
                        // if theres two instances of ':' it has a meta defined.
                        boolean definedMeta = input.indexOf(":") != input.lastIndexOf(":");
                        int inputMeta = definedMeta ? Integer.valueOf(input.substring(input.lastIndexOf(":") + 1)) : 0;
                        int inputCount = i < data.getInputsCount().length ? data.getInputsCount()[i] : 1;
                        input = definedMeta ? input.substring(0, input.lastIndexOf(":")) : input;

                        if (input.contains(":")) {
                            // Normal registry name.
                            Item item = Item.REGISTRY.getObject(new ResourceLocation(input));
                            inputs.add(new Tuple<>(new ItemStack(item, inputCount, inputMeta), null));
                        } else if (OreDictionary.doesOreNameExist(input)) {
                            // Oredict name.
                            inputs.add(new Tuple<>(input, inputCount));
                        } else {
                            TeckleLog.error("Failed to load alloy recipe, invalid input data." +
                                    " Name:{}, Meta:{}, Count:{}", input, inputMeta, inputCount);
                            failed = true;
                            break;
                        }
                    }
                    if (failed)
                        continue;

                    ItemStack outputStack = null;
                    String output = data.getOutput();
                    // if theres two instances of ':' it has a meta defined.
                    boolean definedMeta = output.indexOf(":") != output.lastIndexOf(":");
                    int outputMeta = definedMeta ? Integer.valueOf(output.substring(output.lastIndexOf(":") + 1)) : 0;
                    int outputCount = data.getOutputCount() <= 0 ? 1 : data.getOutputCount();
                    output = definedMeta ? output.substring(0, output.lastIndexOf(":")) : output;
                    if (output.contains(":")) {
                        // Normal registry name.
                        Item item = Item.REGISTRY.getObject(new ResourceLocation(output));
                        outputStack = new ItemStack(item, outputCount, outputMeta);
                    } else {
                        TeckleLog.error("Failed to load alloy recipe, invalid output data." +
                                " Name:{}, Meta:{}, Count:{}", output, outputMeta, outputCount);
                        continue;
                    }

                    Tuple<Object, Integer>[] inputsArray = new Tuple[inputs.size()];
                    inputsArray = inputs.toArray(inputsArray);
                    AlloyRecipe loadedRecipe = new AlloyRecipe(outputStack, inputsArray);
                    registerRecipe(loadedRecipe);
                }
            }
        }
        plantExampleRecipe(recipeFolder);
    }


    private void plantExampleRecipe(File alloyRecipeFolder) {
        // Writes a small demo recipe with an underscore in the name so it doesn't load.
        // TODO: HJSON support?
        RecipeData testData = new RecipeData();
        testData.setInputs(new String[]{"ingotIron:0", "ingotGold:0"});
        testData.setInputsCount(new int[]{1, 1});
        testData.setOutput(Items.BLAZE_ROD.getRegistryName().toString());
        try {
            File demoRecipe = new File(alloyRecipeFolder, "_demorecipe.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(demoRecipe.toPath(), gson.toJson(testData, RecipeData.class).getBytes());
        } catch (Exception e) {
            // Not that big of a problem...
        }
    }

    private AlloyRecipe convertFurnaceRecipe(Map.Entry<ItemStack, ItemStack> furnaceRecipe) {
        return new AlloyRecipe(furnaceRecipe.getValue(), new Tuple<>(furnaceRecipe.getKey(), null));
    }

    /**
     * Data that we deserialize from JSON, uses getters and setters to prevent null results on certain optional vars.
     */
    public class RecipeData {
        private String output = "";
        private int outputCount = 1;

        private String[] inputs = new String[0];
        private int[] inputsCount = new int[0];

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public int getOutputCount() {
            return outputCount;
        }

        public void setOutputCount(int outputCount) {
            this.outputCount = outputCount;
        }

        public String[] getInputs() {
            if (inputs == null)
                return new String[0];
            return inputs;
        }

        public void setInputs(String[] inputs) {
            this.inputs = inputs;
        }

        public int[] getInputsCount() {
            if (inputsCount == null)
                return new int[0];
            return inputsCount;
        }

        public void setInputsCount(int[] inputsCount) {
            this.inputsCount = inputsCount;
        }
    }
}
