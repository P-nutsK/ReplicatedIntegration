package com.p_nsk.replicated_integration.adapter.mekanism

import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.api.addon.ReplicationAddon
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonEnvironment
import com.p_nsk.replicated_integration.adapter.mekanism.synthetic.ForgeMekanismSyntheticConversionContributor
import com.p_nsk.replicated_integration.bridge.ForgeReplicationAddonContext
import com.p_nsk.replicated_integration.config.ForgeCompatibilityConfig
import net.minecraft.world.item.crafting.Recipe

object ReplicationMekanismAddon : ReplicationAddon<ForgeReplicationAddonContext> {
    override val id: String = "mekanism"

    override fun isEnabled(environment: ReplicationAddonEnvironment): Boolean =
        environment.isModLoaded(id)

    @Suppress("UNCHECKED_CAST")
    override fun collectConversions(context: ForgeReplicationAddonContext, collector: IConversionSink) {
        for (recipe in context.recipeManager.recipes) {
            val mapper = MekanismRecipeMappers.all.firstOrNull { it.supports(recipe) } ?: continue
            mapper.collect(recipe as Recipe<*>, collector)
        }
        if (ForgeCompatibilityConfig.isNuclearRecipeEnabled()) {
            ForgeMekanismSyntheticConversionContributor.collectNuclearConversions(collector)
        }
    }
}
