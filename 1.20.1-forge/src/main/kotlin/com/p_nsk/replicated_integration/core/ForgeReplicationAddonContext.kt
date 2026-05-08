package com.p_nsk.replicated_integration.core

import com.buuz135.replication.recipe.MatterValueRecipe
import net.minecraft.core.RegistryAccess
import net.minecraft.world.item.crafting.RecipeManager

data class ForgeReplicationAddonContext(
    val recipeManager: RecipeManager,
    val registryAccess: RegistryAccess,
    val defaultMatterRecipes: List<MatterValueRecipe>,
)
