package com.p_nsk.replicated_integration.adapter.mekanism

import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.addon.ReplicationAddon
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonEnvironment
import com.p_nsk.replicated_integration.adapter.mekanism.synthetic.NeoMekanismSyntheticConversionContributor
import com.p_nsk.replicated_integration.bridge.NeoReplicationAddonContext
import com.p_nsk.replicated_integration.config.NeoCompatibilityConfig
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
        if (NeoCompatibilityConfig.isNuclearRecipeEnabled()) {
            NeoMekanismSyntheticConversionContributor.collectNuclearConversions(collector)
        }
    }
}
