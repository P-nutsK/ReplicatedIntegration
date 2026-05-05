package com.p_nsk.replicated_integration.addon

import com.p_nsk.replicated_integration.api.IConversionSink
import com.p_nsk.replicated_integration.api.ReplicationAddon
import com.p_nsk.replicated_integration.api.ReplicationAddonEnvironment
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeHolder

object ReplicationMekanismAddon : ReplicationAddon<NeoReplicationAddonContext> {
    override val id: String = "mekanism"

    override fun isEnabled(environment: ReplicationAddonEnvironment): Boolean =
        environment.isModLoaded(id)

    override fun collectConversions(context: NeoReplicationAddonContext, collector: IConversionSink) {
        for (recipe in context.recipeManager.recipes) {
            val mapper = MekanismRecipeMappers.all.firstOrNull { it.supports(recipe) } ?: continue
            mapper.collect(recipe as RecipeHolder<Recipe<*>>, collector)
        }
    }
}
