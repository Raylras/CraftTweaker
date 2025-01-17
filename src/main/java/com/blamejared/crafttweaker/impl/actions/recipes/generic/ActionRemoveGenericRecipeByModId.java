package com.blamejared.crafttweaker.impl.actions.recipes.generic;

import com.blamejared.crafttweaker.api.managers.IRecipeManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

public class ActionRemoveGenericRecipeByModId extends ActionRemoveGenericRecipeBase {
    
    private final String modId;
    private final IRecipeManager.RecipeFilter exclude;
    
    public ActionRemoveGenericRecipeByModId(String modId, IRecipeManager.RecipeFilter exclude) {
        
        this.modId = modId;
        this.exclude = exclude;
    }
    
    @Override
    public String describe() {
        
        final String message = String.format("Removing all recipes from modId \"%s\"", modId);
        return exclude == null ? message : (message + ", while retaining recipes matching a filter");
    }
    
    @Override
    protected boolean shouldRemove(IRecipe<?> recipe) {
        
        final ResourceLocation id = recipe.getId();
        if(!id.getNamespace().equals(modId)) {
            return false;
        }
        
        return exclude == null || !exclude.test(id.getPath());
    }
    
}
