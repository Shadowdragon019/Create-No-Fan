package com.sammy.create_no_fan.mixins;

import com.sammy.create_no_fan.CreateNoFan;
import com.sammy.create_no_fan.RemadeCategoryBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.jei.CreateJEI;
import com.simibubi.create.compat.jei.category.*;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import com.simibubi.create.content.fluids.potion.PotionMixingRecipes;
import com.simibubi.create.content.fluids.transfer.EmptyingRecipe;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.kinetics.crusher.AbstractCrushingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import com.simibubi.create.content.kinetics.fan.processing.HauntingRecipe;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.data.recipe.LogStrippingFakeRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.crafting.IShapedRecipe;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(CreateJEI.class)
public abstract class CreateJEIMixin implements IModPlugin {
    @Shadow(remap = false)
    @Final
    private List<CreateRecipeCategory<?>> allCategories;
    
    @Shadow(remap = false)
    private IIngredientManager ingredientManager;

    @Inject(method = "loadCategories", at = @At("TAIL"), remap = false)
    private void loadCategoriesMixin(CallbackInfo ci) {
        allCategories.clear();

        //Milling
        RemadeCategoryBuilder.builder(AbstractCrushingRecipe.class)
                .addTypedRecipes(AllRecipeTypes.MILLING)
                .catalyst(AllBlocks.MILLSTONE::get)
                .doubleItemIcon(AllBlocks.MILLSTONE.get(), AllItems.WHEAT_FLOUR.get())
                .emptyBackground(177, 53)
                .build("milling", MillingCategory::new, allCategories);

        //Crushing
        RemadeCategoryBuilder.builder(AbstractCrushingRecipe.class)
                .addTypedRecipes(AllRecipeTypes.CRUSHING)
                .addTypedRecipesExcluding(AllRecipeTypes.MILLING::getType, AllRecipeTypes.CRUSHING::getType)
                .catalyst(AllBlocks.CRUSHING_WHEEL::get)
                .doubleItemIcon(AllBlocks.CRUSHING_WHEEL.get(), AllItems.CRUSHED_GOLD.get())
                .emptyBackground(177, 100)
                .build("crushing", CrushingCategory::new, allCategories);

        //Pressing
        RemadeCategoryBuilder.builder(PressingRecipe.class)
                .addTypedRecipes(AllRecipeTypes.PRESSING)
                .catalyst(AllBlocks.MECHANICAL_PRESS::get)
                .doubleItemIcon(AllBlocks.MECHANICAL_PRESS.get(), AllItems.IRON_SHEET.get())
                .emptyBackground(177, 70)
                .build("pressing", PressingCategory::new, allCategories);

        //Washing
        RemadeCategoryBuilder.builder(SplashingRecipe.class)
                .addTypedRecipes(AllRecipeTypes.SPLASHING)
                .catalystStack(ProcessingViaFanCategory.getFan("fan_washing"))
                .doubleItemIcon(AllItems.PROPELLER.get(), Items.WATER_BUCKET)
                .emptyBackground(178, 72)
                .build("fan_washing", FanWashingCategory::new, allCategories);;

        //Smoking
        RemadeCategoryBuilder.builder(SmokingRecipe.class)
                .addAllRecipesIf(recipe -> recipe instanceof SmokingRecipe && !CreateNoFan.isRecipeAllowed(recipe))
                .catalystStack(ProcessingViaFanCategory.getFan("fan_smoking"))
                .doubleItemIcon(AllItems.PROPELLER.get(), Items.CAMPFIRE)
                .emptyBackground(178, 72)
                .build("fan_smoking", FanSmokingCategory::new, allCategories);

        //Blasting
        //I have no clue how accurate this is, as despite have the right count of recipes, they're all jumbled up in REI compared to normal
        RemadeCategoryBuilder.builder(AbstractCookingRecipe.class)
                .addAllRecipesIf(recipe -> recipe instanceof SmeltingRecipe && !CreateNoFan.isRecipeAllowed(recipe))
                .removeRecipes(() -> RecipeType.SMOKING)
                .catalystStack(ProcessingViaFanCategory.getFan("fan_blasting"))
                .doubleItemIcon(AllItems.PROPELLER.get(), Items.LAVA_BUCKET)
                .emptyBackground(178, 72)
                .build("fan_blasting", FanBlastingCategory::new, allCategories);
        //Normal Create recipes
		/*RemadeCategoryBuilder.builder(AbstractCookingRecipe.class)
				.addTypedRecipesExcluding(() -> RecipeType.SMELTING, () -> RecipeType.BLASTING)
				.addTypedRecipes(() -> RecipeType.BLASTING)
				.removeRecipes(() -> RecipeType.SMOKING)
				.catalystStack(ProcessingViaFanCategory.getFan("fan_blasting"))
				.doubleItemIcon(AllItems.PROPELLER.get(), Items.LAVA_BUCKET)
				.emptyBackground(178, 72)
				.build("fan_blasting", FanBlastingCategory::new, allCategories);*/

        //Haunting
        RemadeCategoryBuilder.builder(HauntingRecipe.class)
                .addTypedRecipes(AllRecipeTypes.HAUNTING)
                .catalystStack(ProcessingViaFanCategory.getFan("fan_haunting"))
                .doubleItemIcon(AllItems.PROPELLER.get(), Items.SOUL_CAMPFIRE)
                .emptyBackground(178, 72)
                .build("fan_haunting", FanHauntingCategory::new, allCategories);

        //Mixing
        RemadeCategoryBuilder.builder(BasinRecipe.class)
                .addTypedRecipes(AllRecipeTypes.MIXING)
                .catalyst(AllBlocks.MECHANICAL_MIXER::get)
                .catalyst(AllBlocks.BASIN::get)
                .doubleItemIcon(AllBlocks.MECHANICAL_MIXER.get(), AllBlocks.BASIN.get())
                .emptyBackground(177, 103)
                .build("mixing", MixingCategory::standard, allCategories);

        //Auto shapeless
        RemadeCategoryBuilder.builder(BasinRecipe.class)
                .enableWhen(c -> c.allowShapelessInMixer)
                .addAllRecipesIf(r -> r instanceof CraftingRecipe && !(r instanceof IShapedRecipe<?>)
                                && r.getIngredients()
                                .size() > 1
                                && !MechanicalPressBlockEntity.canCompress(r) && !AllRecipeTypes.shouldIgnoreInAutomation(r),
                        BasinRecipe::convertShapeless)
                .catalyst(AllBlocks.MECHANICAL_MIXER::get)
                .catalyst(AllBlocks.BASIN::get)
                .doubleItemIcon(AllBlocks.MECHANICAL_MIXER.get(), Items.CRAFTING_TABLE)
                .emptyBackground(177, 85)
                .build("automatic_shapeless", MixingCategory::autoShapeless, allCategories);

        //Brewing
        RemadeCategoryBuilder.builder(BasinRecipe.class)
                .enableWhen(c -> c.allowBrewingInMixer)
                .addRecipes(() -> PotionMixingRecipes.ALL)
                .catalyst(AllBlocks.MECHANICAL_MIXER::get)
                .catalyst(AllBlocks.BASIN::get)
                .doubleItemIcon(AllBlocks.MECHANICAL_MIXER.get(), Blocks.BREWING_STAND)
                .emptyBackground(177, 103)
                .build("automatic_brewing", MixingCategory::autoBrewing, allCategories);

        //Packing
        RemadeCategoryBuilder.builder(BasinRecipe.class)
                .addTypedRecipes(AllRecipeTypes.COMPACTING)
                .catalyst(AllBlocks.MECHANICAL_PRESS::get)
                .catalyst(AllBlocks.BASIN::get)
                .doubleItemIcon(AllBlocks.MECHANICAL_PRESS.get(), AllBlocks.BASIN.get())
                .emptyBackground(177, 103)
                .build("packing", PackingCategory::standard, allCategories);

        //Auto square
        RemadeCategoryBuilder.builder(BasinRecipe.class)
                .enableWhen(c -> c.allowShapedSquareInPress)
                .addAllRecipesIf(
                        r -> (r instanceof CraftingRecipe) && !(r instanceof MechanicalCraftingRecipe)
                                && MechanicalPressBlockEntity.canCompress(r) && !AllRecipeTypes.shouldIgnoreInAutomation(r),
                        BasinRecipe::convertShapeless)
                .catalyst(AllBlocks.MECHANICAL_PRESS::get)
                .catalyst(AllBlocks.BASIN::get)
                .doubleItemIcon(AllBlocks.MECHANICAL_PRESS.get(), Blocks.CRAFTING_TABLE)
                .emptyBackground(177, 85)
                .build("automatic_packing", PackingCategory::autoSquare, allCategories);

        //Sawing
        RemadeCategoryBuilder.builder(CuttingRecipe.class)
                .addTypedRecipes(AllRecipeTypes.CUTTING)
                .catalyst(AllBlocks.MECHANICAL_SAW::get)
                .doubleItemIcon(AllBlocks.MECHANICAL_SAW.get(), Items.OAK_LOG)
                .emptyBackground(177, 70)
                .build("sawing", SawingCategory::new, allCategories);

        //Block cutting
        RemadeCategoryBuilder.builder(BlockCuttingCategory.CondensedBlockCuttingRecipe.class)
                .enableWhen(c -> c.allowStonecuttingOnSaw)
                .addRecipes(() -> BlockCuttingCategory.CondensedBlockCuttingRecipe.condenseRecipes(RemadeCategoryBuilder.getTypedRecipesExcluding(RecipeType.STONECUTTING, AllRecipeTypes::shouldIgnoreInAutomation)))
                .catalyst(AllBlocks.MECHANICAL_SAW::get)
                .doubleItemIcon(AllBlocks.MECHANICAL_SAW.get(), Items.STONE_BRICK_STAIRS)
                .emptyBackground(177, 70)
                .build("block_cutting", BlockCuttingCategory::new, allCategories);

        //Wood cutting
        RemadeCategoryBuilder.builder(BlockCuttingCategory.CondensedBlockCuttingRecipe.class)
                .enableIf(c -> c.allowWoodcuttingOnSaw.get() && ModList.get()
                        .isLoaded("druidcraft"))
                .addRecipes(() -> BlockCuttingCategory.CondensedBlockCuttingRecipe.condenseRecipes(RemadeCategoryBuilder.getTypedRecipesExcluding(SawBlockEntity.woodcuttingRecipeType.get(), AllRecipeTypes::shouldIgnoreInAutomation)))
                .catalyst(AllBlocks.MECHANICAL_SAW::get)
                .doubleItemIcon(AllBlocks.MECHANICAL_SAW.get(), Items.OAK_STAIRS)
                .emptyBackground(177, 70)
                .build("wood_cutting", BlockCuttingCategory::new, allCategories);

        //Polishing
        RemadeCategoryBuilder.builder(SandPaperPolishingRecipe.class)
                .addTypedRecipes(AllRecipeTypes.SANDPAPER_POLISHING)
                .catalyst(AllItems.SAND_PAPER::get)
                .catalyst(AllItems.RED_SAND_PAPER::get)
                .itemIcon(AllItems.SAND_PAPER.get())
                .emptyBackground(177, 55)
                .build("sandpaper_polishing", PolishingCategory::new, allCategories);

        //Item application (why is this one's case different? it was "item_application" instead of "itemApplication")
        RemadeCategoryBuilder.builder(ItemApplicationRecipe.class)
                .addTypedRecipes(AllRecipeTypes.ITEM_APPLICATION)
                .addRecipes(LogStrippingFakeRecipes::createRecipes)
                .itemIcon(AllItems.BRASS_HAND.get())
                .emptyBackground(177, 60)
                .build("item_application", ItemApplicationCategory::new, allCategories);

        //Deploying
        RemadeCategoryBuilder.builder(DeployerApplicationRecipe.class)
                .addTypedRecipes(AllRecipeTypes.DEPLOYING)
                .addTypedRecipes(AllRecipeTypes.SANDPAPER_POLISHING::getType, DeployerApplicationRecipe::convert)
                .addTypedRecipes(AllRecipeTypes.ITEM_APPLICATION::getType, ManualApplicationRecipe::asDeploying)
                .catalyst(AllBlocks.DEPLOYER::get)
                .catalyst(AllBlocks.DEPOT::get)
                .catalyst(AllItems.BELT_CONNECTOR::get)
                .itemIcon(AllBlocks.DEPLOYER.get())
                .emptyBackground(177, 70)
                .build("deploying", DeployingCategory::new, allCategories);

        //Spout filling
        RemadeCategoryBuilder.builder(FillingRecipe.class)
                .addTypedRecipes(AllRecipeTypes.FILLING)
                .addRecipeListConsumer(recipes -> SpoutCategory.consumeRecipes(recipes::add, ingredientManager))
                .catalyst(AllBlocks.SPOUT::get)
                .doubleItemIcon(AllBlocks.SPOUT.get(), Items.WATER_BUCKET)
                .emptyBackground(177, 70)
                .build("spout_filling", SpoutCategory::new, allCategories);

        //Draining
        RemadeCategoryBuilder.builder(EmptyingRecipe.class)
                .addRecipeListConsumer(recipes -> ItemDrainCategory.consumeRecipes(recipes::add, ingredientManager))
                .addTypedRecipes(AllRecipeTypes.EMPTYING)
                .catalyst(AllBlocks.ITEM_DRAIN::get)
                .doubleItemIcon(AllBlocks.ITEM_DRAIN.get(), Items.WATER_BUCKET)
                .emptyBackground(177, 50)
                .build("draining", ItemDrainCategory::new, allCategories);

        //Auto shaped
        RemadeCategoryBuilder.builder(CraftingRecipe.class)
                .enableWhen(c -> c.allowRegularCraftingInCrafter)
                .addAllRecipesIf(r -> r instanceof CraftingRecipe && !(r instanceof IShapedRecipe<?>)
                        && r.getIngredients()
                        .size() == 1
                        && !AllRecipeTypes.shouldIgnoreInAutomation(r))
                .addTypedRecipesIf(() -> RecipeType.CRAFTING,
                        recipe -> recipe instanceof IShapedRecipe<?> && !AllRecipeTypes.shouldIgnoreInAutomation(recipe))
                .catalyst(AllBlocks.MECHANICAL_CRAFTER::get)
                .itemIcon(AllBlocks.MECHANICAL_CRAFTER.get())
                .emptyBackground(177, 107)
                .build("automatic_shaped", MechanicalCraftingCategory::new, allCategories);

        //Mechanical crafting
        RemadeCategoryBuilder.builder(CraftingRecipe.class)
                .addTypedRecipes(AllRecipeTypes.MECHANICAL_CRAFTING)
                .catalyst(AllBlocks.MECHANICAL_CRAFTER::get)
                .itemIcon(AllBlocks.MECHANICAL_CRAFTER.get())
                .emptyBackground(177, 107)
                .build("mechanical_crafting", MechanicalCraftingCategory::new, allCategories);

        //Sequenced assembly
        RemadeCategoryBuilder.builder(SequencedAssemblyRecipe.class)
                .addTypedRecipes(AllRecipeTypes.SEQUENCED_ASSEMBLY)
                .itemIcon(AllItems.PRECISION_MECHANISM.get())
                .emptyBackground(180, 115)
                .build("sequenced_assembly", SequencedAssemblyCategory::new, allCategories);

        //Mystery conversion
        RemadeCategoryBuilder.builder(ConversionRecipe.class)
                .addRecipes(() -> MysteriousItemConversionCategory.RECIPES)
                .itemIcon(AllBlocks.PECULIAR_BELL.get())
                .emptyBackground(177, 50)
                .build("mystery_conversion", MysteriousItemConversionCategory::new, allCategories);
    }
}
