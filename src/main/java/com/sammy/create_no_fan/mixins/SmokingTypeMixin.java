package com.sammy.create_no_fan.mixins;

import com.sammy.create_no_fan.CreateNoFan;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(AllFanProcessingTypes.SmokingType.class)
public abstract class SmokingTypeMixin {
    @Final
    @Shadow(remap = false)
    private static RecipeWrapper RECIPE_WRAPPER;

    @Inject(method = "canProcess", at = @At("HEAD"), cancellable = true, remap = false)
    private void canProcessMixin(ItemStack stack, Level level, CallbackInfoReturnable<Boolean> cir) {
        RECIPE_WRAPPER.setItem(0, stack);
        Optional<SmeltingRecipe> smeltingRecipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, RECIPE_WRAPPER, level);
        if (CreateNoFan.isRecipeAllowed(smeltingRecipe)) {
            cir.setReturnValue(false);
        }
    }
}
