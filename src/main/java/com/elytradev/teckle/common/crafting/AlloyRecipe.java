package com.elytradev.teckle.common.crafting;

import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Tuple;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Modified version of ShapelessOreRecipe that considers item stack size.
 */
public class AlloyRecipe {
    @Nonnull
    protected ItemStack output = ItemStack.EMPTY;
    protected NonNullList<Object> input = NonNullList.create();

    public AlloyRecipe(@Nonnull ItemStack result, Tuple<Object, Integer>... recipe) {
        output = result.copy();
        for (Tuple<Object, Integer> in : recipe) {
            if (in.getFirst() instanceof ItemStack) {
                input.add(((ItemStack) in.getFirst()).copy());
            } else if (in.getFirst() instanceof Item) {
                input.add(new ItemStack((Item) in.getFirst(), in.getSecond()));
            } else if (in.getFirst() instanceof Block) {
                input.add(new ItemStack((Block) in.getFirst(), in.getSecond()));
            } else if (in.getFirst() instanceof String) {
                List<ItemStack> list = new ArrayList<>();
                for (ItemStack stack : OreDictionary.getOres((String) in.getFirst())) {
                    ItemStack addStack = stack.copy();
                    addStack.setCount(in.getSecond());
                    list.add(addStack);
                }
                input.add(list);
            } else {
                String ret = "Invalid shapeless ore recipe: ";
                for (Object tmp : recipe) {
                    ret += tmp + ", ";
                }
                ret += output;
                throw new RuntimeException(ret);
            }
        }
    }

    public int getRecipeSize() {
        return input.size();
    }

    @Nonnull
    public ItemStack getCraftingResult() {
        return output.copy();
    }

    public List<ItemStack> matches(IItemHandler inv) {
        NonNullList<Object> required = NonNullList.create();
        NonNullList<ItemStack> consumed = NonNullList.create();
        required.addAll(input);

        for (int x = 0; x < inv.getSlots(); x++) {
            ItemStack stackInSlot = inv.getStackInSlot(x);

            if (!stackInSlot.isEmpty()) {
                boolean inRecipe = false;
                Iterator<Object> req = required.iterator();

                while (req.hasNext()) {
                    boolean match = false;
                    Object next = req.next();

                    if (next instanceof ItemStack) {
                        match = OreDictionary.itemMatches((ItemStack) next, stackInSlot, false) && stackInSlot.getCount() >= ((ItemStack) next).getCount();
                        consumed.add((ItemStack) next);
                    } else if (next instanceof List) {
                        Iterator<ItemStack> itr = ((List<ItemStack>) next).iterator();
                        while (itr.hasNext() && !match) {
                            ItemStack nextStack = itr.next();
                            match = OreDictionary.itemMatches(nextStack, stackInSlot, false) && stackInSlot.getCount() >= nextStack.getCount();
                            consumed.add(nextStack);
                        }
                    }

                    if (match) {
                        inRecipe = true;
                        required.remove(next);
                        break;
                    }
                }

                if (!inRecipe) {
                    return NonNullList.create();
                }
            }
        }

        if(!required.isEmpty()){
            consumed.clear();
        }

        return consumed;
    }

    public int getCookTime(){
        return 100;
    }
}
