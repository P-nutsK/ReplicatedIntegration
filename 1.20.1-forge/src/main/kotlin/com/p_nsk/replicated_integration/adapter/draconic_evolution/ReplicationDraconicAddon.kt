package com.p_nsk.replicated_integration.adapter.draconic_evolution

import com.p_nsk.replicated_integration.api.addon.ReplicationAddonEnvironment
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonLoadSafetyContract
import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.core.ForgeReplicationAddon
import com.p_nsk.replicated_integration.core.ForgeReplicationAddonContext
import net.minecraft.world.item.crafting.Recipe

@OptIn(ReplicationAddonLoadSafetyContract::class)
object ReplicationDraconicAddon : ForgeReplicationAddon {
    override val id: String = "draconicevolution"

    override fun isEnabled(environment: ReplicationAddonEnvironment): Boolean =
        environment.isModLoaded(id)

    @Suppress("UNCHECKED_CAST")
    override fun collectConversions(context: ForgeReplicationAddonContext, collector: IConversionSink) {
        for (recipe in context.recipeManager.recipes) {
            val mapper = DraconicRecipeMappers.all.firstOrNull { it.supports(recipe) } ?: continue
            mapper.collect(recipe as Recipe<*>, collector)
        }
    }
}
