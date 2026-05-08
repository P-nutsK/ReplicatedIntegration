package com.p_nsk.replicated_integration.adapter.advanced_ae

import com.p_nsk.replicated_integration.api.addon.ReplicationAddon
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonEnvironment
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonLoadSafetyContract
import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.core.ForgeReplicationAddonContext
import net.minecraft.world.item.crafting.Recipe

@OptIn(ReplicationAddonLoadSafetyContract::class)
object ReplicationAdvancedAEAddon : ReplicationAddon<ForgeReplicationAddonContext> {
    override val id: String = "advanced_ae"

    override fun isEnabled(environment: ReplicationAddonEnvironment): Boolean =
        environment.isModLoaded(id)

    @Suppress("UNCHECKED_CAST")
    override fun collectConversions(context: ForgeReplicationAddonContext, collector: IConversionSink) {
        for (recipe in context.recipeManager.recipes) {
            val mapper = AAERecipeMappers.all.firstOrNull { it.supports(recipe) } ?: continue
            mapper.collect(recipe as Recipe<*>, collector)
        }
    }
}
