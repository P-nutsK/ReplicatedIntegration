package com.p_nsk.replicated_integration.data

import com.buuz135.replication.calculation.ReplicationCalculation
import com.p_nsk.replicated_integration.Constants
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.AddReloadListenerEvent

object ReplicationReloadHooks {
    @SubscribeEvent
    fun onAddReloadListener(event: AddReloadListenerEvent) {
        event.addListener(MatterNodeValueReloadListener)
        event.addListener(
            ResourceManagerReloadListener {
                ReplicationCalculation.organizeRecipes(
                    event.serverResources.recipeManager,
                    event.registryAccess,
                )
                Constants.LOGGER.info(
                    "Organized {} Replication default matter recipes during datapack reload",
                    ReplicationCalculation.DEFAULT_MATTER_RECIPE.size,
                )
            },
        )
    }
}
