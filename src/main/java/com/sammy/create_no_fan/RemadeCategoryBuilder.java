package com.sammy.create_no_fan;

import com.simibubi.create.Create;
import com.simibubi.create.compat.jei.CreateJEI;
import com.simibubi.create.compat.jei.DoubleItemIcon;
import com.simibubi.create.compat.jei.EmptyBackground;
import com.simibubi.create.compat.jei.ItemIcon;
import com.simibubi.create.compat.jei.category.CreateRecipeCategory;
import com.simibubi.create.foundation.config.ConfigBase;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CRecipes;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

//I just copy-pasted this with some minor tweaks to make sure it works
//I did this because I can't figure out how to access the actual class lol
public class RemadeCategoryBuilder<T extends Recipe<?>> {
    public final Class<? extends T> recipeClass;
    public Predicate<CRecipes> predicate = cRecipes -> true;

    public IDrawable background;
    public IDrawable icon;

    public final List<Consumer<List<T>>> recipeListConsumers = new ArrayList<>();
    public final List<Supplier<? extends ItemStack>> catalysts = new ArrayList<>();

    public RemadeCategoryBuilder(Class<? extends T> recipeClass) {
        this.recipeClass = recipeClass;
    }

    public static <T2 extends Recipe<?>> RemadeCategoryBuilder<T2> builder(Class<? extends T2> recipeClass) {
        return new RemadeCategoryBuilder<>(recipeClass);
    }

    public RemadeCategoryBuilder<T> enableIf(Predicate<CRecipes> predicate) {
        this.predicate = predicate;
        return this;
    }

    public RemadeCategoryBuilder<T> enableWhen(Function<CRecipes, ConfigBase.ConfigBool> configValue) {
        predicate = c -> configValue.apply(c).get();
        return this;
    }

    public RemadeCategoryBuilder<T> addRecipeListConsumer(Consumer<List<T>> consumer) {
        recipeListConsumers.add(consumer);
        return this;
    }

    public RemadeCategoryBuilder<T> addRecipes(Supplier<Collection<? extends T>> collection) {
        return addRecipeListConsumer(recipes -> recipes.addAll(collection.get()));
    }

    public RemadeCategoryBuilder<T> addAllRecipesIf(Predicate<Recipe<?>> pred) {
        return addRecipeListConsumer(recipes -> consumeAllRecipes(recipe -> {
            if (pred.test(recipe)) {
                recipes.add((T) recipe);
            }
        }));
    }

    public RemadeCategoryBuilder<T> addAllRecipesIf(Predicate<Recipe<?>> pred, Function<Recipe<?>, T> converter) {
        return addRecipeListConsumer(recipes -> consumeAllRecipes(recipe -> {
            if (pred.test(recipe)) {
                recipes.add(converter.apply(recipe));
            }
        }));
    }

    public RemadeCategoryBuilder<T> addTypedRecipes(IRecipeTypeInfo recipeTypeEntry) {
        return addTypedRecipes(recipeTypeEntry::getType);
    }

    public RemadeCategoryBuilder<T> addTypedRecipes(Supplier<RecipeType<? extends T>> recipeType) {
        return addRecipeListConsumer(recipes -> CreateJEI.<T>consumeTypedRecipes(recipes::add, recipeType.get()));
    }

    public RemadeCategoryBuilder<T> addTypedRecipes(Supplier<RecipeType<? extends T>> recipeType, Function<Recipe<?>, T> converter) {
        return addRecipeListConsumer(recipes -> CreateJEI.<T>consumeTypedRecipes(recipe -> recipes.add(converter.apply(recipe)), recipeType.get()));
    }

    public RemadeCategoryBuilder<T> addTypedRecipesIf(Supplier<RecipeType<? extends T>> recipeType, Predicate<Recipe<?>> pred) {
        return addRecipeListConsumer(recipes -> CreateJEI.<T>consumeTypedRecipes(recipe -> {
            if (pred.test(recipe)) {
                recipes.add(recipe);
            }
        }, recipeType.get()));
    }

    public RemadeCategoryBuilder<T> addTypedRecipesExcluding(Supplier<RecipeType<? extends T>> recipeType,
                                                                 Supplier<RecipeType<? extends T>> excluded) {
        return addRecipeListConsumer(recipes -> {
            List<Recipe<?>> excludedRecipes = getTypedRecipes(excluded.get());
            CreateJEI.<T>consumeTypedRecipes(recipe -> {
                for (Recipe<?> excludedRecipe : excludedRecipes) {
                    if (doInputsMatch(recipe, excludedRecipe)) {
                        return;
                    }
                }
                recipes.add(recipe);
            }, recipeType.get());
        });
    }

    public RemadeCategoryBuilder<T> removeRecipes(Supplier<RecipeType<? extends T>> recipeType) {
        return addRecipeListConsumer(recipes -> {
            List<Recipe<?>> excludedRecipes = getTypedRecipes(recipeType.get());
            recipes.removeIf(recipe -> {
                for (Recipe<?> excludedRecipe : excludedRecipes)
                    if (doInputsMatch(recipe, excludedRecipe) && doOutputsMatch(recipe, excludedRecipe))
                        return true;
                return false;
            });
        });
    }

    public RemadeCategoryBuilder<T> catalystStack(Supplier<ItemStack> supplier) {
        catalysts.add(supplier);
        return this;
    }

    public RemadeCategoryBuilder<T> catalyst(Supplier<ItemLike> supplier) {
        return catalystStack(() -> new ItemStack(supplier.get()
                .asItem()));
    }

    public RemadeCategoryBuilder<T> icon(IDrawable icon) {
        this.icon = icon;
        return this;
    }

    public RemadeCategoryBuilder<T> itemIcon(ItemLike item) {
        icon(new ItemIcon(() -> new ItemStack(item)));
        return this;
    }

    public RemadeCategoryBuilder<T> doubleItemIcon(ItemLike item1, ItemLike item2) {
        icon(new DoubleItemIcon(() -> new ItemStack(item1), () -> new ItemStack(item2)));
        return this;
    }

    public RemadeCategoryBuilder<T> background(IDrawable background) {
        this.background = background;
        return this;
    }

    public RemadeCategoryBuilder<T> emptyBackground(int width, int height) {
        background(new EmptyBackground(width, height));
        return this;
    }

    public CreateRecipeCategory<T> build(String name, CreateRecipeCategory.Factory<T> factory, List<CreateRecipeCategory<?>> allCategories) {
        Supplier<List<T>> recipesSupplier;
        if (predicate.test(AllConfigs.server().recipes)) {
            recipesSupplier = () -> {
                List<T> recipes = new ArrayList<>();
                for (Consumer<List<T>> consumer : recipeListConsumers)
                    consumer.accept(recipes);
                return recipes;
            };
        } else {
            recipesSupplier = () -> Collections.emptyList();
        }

        CreateRecipeCategory.Info<T> info = new CreateRecipeCategory.Info<>(
                new mezz.jei.api.recipe.RecipeType<>(Create.asResource(name), recipeClass),
                Lang.translateDirect("recipe." + name), background, icon, recipesSupplier, catalysts);
        CreateRecipeCategory<T> category = factory.create(info);
        allCategories.add(category);
        return category;
    }


    public static void consumeAllRecipes(Consumer<Recipe<?>> consumer) {
        Minecraft.getInstance()
                .getConnection()
                .getRecipeManager()
                .getRecipes()
                .forEach(consumer);
    }

    public static <T extends Recipe<?>> void consumeTypedRecipes(Consumer<T> consumer, RecipeType<?> type) {
        Map<ResourceLocation, Recipe<?>> map = Minecraft.getInstance()
                .getConnection()
                .getRecipeManager().recipes.get(type);
        if (map != null) {
            map.values().forEach(recipe -> consumer.accept((T) recipe));
        }
    }

    public static List<Recipe<?>> getTypedRecipes(RecipeType<?> type) {
        List<Recipe<?>> recipes = new ArrayList<>();
        consumeTypedRecipes(recipes::add, type);
        return recipes;
    }

    public static List<Recipe<?>> getTypedRecipesExcluding(RecipeType<?> type, Predicate<Recipe<?>> exclusionPred) {
        List<Recipe<?>> recipes = getTypedRecipes(type);
        recipes.removeIf(exclusionPred);
        return recipes;
    }

    public static boolean doInputsMatch(Recipe<?> recipe1, Recipe<?> recipe2) {
        if (recipe1.getIngredients()
                .isEmpty()
                || recipe2.getIngredients()
                .isEmpty()) {
            return false;
        }
        ItemStack[] matchingStacks = recipe1.getIngredients()
                .get(0)
                .getItems();
        if (matchingStacks.length == 0) {
            return false;
        }
        return recipe2.getIngredients()
                .get(0)
                .test(matchingStacks[0]);
    }

    public static boolean doOutputsMatch(Recipe<?> recipe1, Recipe<?> recipe2) {
        return ItemStack.isSame(recipe1.getResultItem(), recipe2.getResultItem());
    }
}