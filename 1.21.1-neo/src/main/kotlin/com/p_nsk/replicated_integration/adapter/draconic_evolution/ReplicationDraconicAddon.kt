package com.p_nsk.replicated_integration.adapter.draconic_evolution

import com.p_nsk.replicated_integration.api.addon.ReplicationAddonEnvironment
import com.p_nsk.replicated_integration.api.addon.ReplicationAddonLoadSafetyContract
import com.p_nsk.replicated_integration.api.graph.IConversionSink
import com.p_nsk.replicated_integration.core.NeoReplicationAddon
import com.p_nsk.replicated_integration.core.NeoReplicationAddonContext
import net.minecraft.world.item.crafting.RecipeHolder

@OptIn(ReplicationAddonLoadSafetyContract::class)
object ReplicationDraconicAddon : NeoReplicationAddon {
    override val id: String = "draconicevolution"

    override fun isEnabled(environment: ReplicationAddonEnvironment): Boolean =
        environment.isModLoaded(id)

    @Suppress("UNCHECKED_CAST")
    override fun collectConversions(context: NeoReplicationAddonContext, collector: IConversionSink) {
        for (recipe in context.recipeManager.recipes) {
            val mapper = DraconicRecipeMappers.all.firstOrNull { it.supports(recipe) } ?: continue
            mapper.collect(recipe as RecipeHolder<*>, collector)
        }
    }
}
