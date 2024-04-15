package com.sammy.create_no_fan;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Optional;

@Mod(CreateNoFan.mod_id)
public class CreateNoFan {
    public static final String mod_id = "create_no_fan";
    public static final Logger logger = LogUtils.getLogger();
    public CreateNoFan() {
        MinecraftForge.EVENT_BUS.register(this);
    }
    public static boolean isRecipeAllowed(Recipe recipe) {
        return recipe.getId().getPath().endsWith("_no_fan");
    }
    public static boolean isRecipeAllowed(Optional<? extends Recipe> recipe) {
        return recipe.isPresent() && isRecipeAllowed(recipe.get());
    }
}
