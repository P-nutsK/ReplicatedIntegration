package com.p_nsk.replicated_integration.core

import com.buuz135.replication.recipe.MatterValueRecipe
import net.minecraft.core.RegistryAccess
import net.minecraft.world.item.crafting.RecipeHolder
import net.minecraft.world.item.crafting.RecipeManager

data class NeoReplicationAddonContext(
    val recipeManager: RecipeManager,
    val registryAccess: RegistryAccess,
    val defaultMatterRecipes: Collection<RecipeHolder<MatterValueRecipe>>,
)
